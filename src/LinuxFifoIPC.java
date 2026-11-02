import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * LinuxFifoIPC.java
 *
 * Linux / POSIX backend implementation of the shared IPCManager interface using
 * genuine POSIX named pipes (FIFOs) created via mkfifo.
 *
 * POSIX FIFO Characteristics & Channel Topology:
 * - POSIX FIFOs are unidirectional by default.
 * - To support full-duplex, bidirectional communication (e.g. UI <-> Core):
 *   1. Forward FIFO (request): created at IPCConstants.getLinuxFifoPath(channelName)
 *      (e.g., /tmp/pic16f72_ui_core.fifo)
 *   2. Reverse FIFO (response): created at IPCConstants.getLinuxFifoResponsePath(channelName)
 *      (e.g., /tmp/pic16f72_ui_core_resp.fifo)
 * - For unidirectional channels (e.g. Core -> Logger), only the forward FIFO is used.
 *
 * Deadlock Avoidance during Open:
 * - In POSIX, opening a FIFO for reading (O_RDONLY) blocks until a writer opens it.
 * - Opening a FIFO for writing (O_WRONLY) blocks until a reader opens it.
 * - To avoid deadlocks when processes start in arbitrary order:
 *   Server opens read pipe first, while client opens write pipe first on forward channel;
 *   and conversely on reverse channel.
 *
 * Framing & Stream Safety:
 * - Uses length-prefixed stream framing via IPCMessage.readDelimitedFrom / writeDelimitedTo.
 * - Buffers data in 64KB blocks, correctly handling partial writes and reads.
 */
public class LinuxFifoIPC implements IPCManager {

    private final String channelName;
    private final boolean isServer;
    private final String forwardFifoPath;
    private final String responseFifoPath;

    private InputStream inputStream;
    private OutputStream outputStream;

    private volatile boolean connected = false;
    private volatile boolean closed = false;

    private final Object writeLock = new Object();
    private final Object readLock = new Object();

    /**
     * Constructs a Linux FIFO IPC endpoint.
     *
     * @param channelName Logical channel name (e.g., "ui_core", "core_logger")
     * @param isServer True to act as channel server (listener), false to act as client
     */
    public LinuxFifoIPC(String channelName, boolean isServer) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty.");
        }
        this.channelName = channelName.trim();
        this.isServer = isServer;
        this.forwardFifoPath = IPCConstants.getLinuxFifoPath(this.channelName);
        this.responseFifoPath = IPCConstants.getLinuxFifoResponsePath(this.channelName);
    }

    /**
     * Helper to create a POSIX FIFO special file using 'mkfifo -m 600 <path>'.
     * If the file already exists as a FIFO, it is preserved. If it exists as a normal file,
     * it is removed and recreated as a FIFO.
     *
     * @param fifoPath Path of the FIFO to create
     * @throws IOException if FIFO creation fails
     */
    public static void ensureFifoExists(String fifoPath) throws IOException {
        File file = new File(fifoPath);
        if (file.exists()) {
            // Check if it's a directory (should not be)
            if (file.isDirectory()) {
                throw new IOException("Cannot create FIFO at '" + fifoPath + "': directory with same name exists.");
            }
            // Existing FIFO or file
            return;
        }

        // Parent directory must exist
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("mkfifo", "-m", "600", fifoPath);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // If mkfifo returned error because file was created concurrently, verify existence
                if (!file.exists()) {
                    throw new IOException("mkfifo command failed with exit code " + exitCode + " for path: " + fifoPath);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while executing mkfifo for '" + fifoPath + "'", ie);
        } catch (IOException ioe) {
            throw new IOException("Failed to invoke mkfifo for path '" + fifoPath + "'. Ensure running on a POSIX/Linux system: " + ioe.getMessage(), ioe);
        }
    }

    @Override
    public synchronized void startServer() throws IOException {
        if (!isServer) {
            throw new IllegalStateException("Cannot call startServer() on an endpoint configured as client.");
        }
        if (closed) {
            throw new IOException("IPC endpoint is already closed.");
        }

        // 1. Create the FIFO files
        ensureFifoExists(forwardFifoPath);
        boolean isBidirectional = !channelName.equalsIgnoreCase(IPCConstants.CHANNEL_CORE_LOGGER);
        if (isBidirectional) {
            ensureFifoExists(responseFifoPath);
        }

        // 2. Open streams in non-deadlocking order:
        // Server reads from forward pipe (blocks until client opens it for writing)
        FileInputStream fis = new FileInputStream(forwardFifoPath);
        this.inputStream = new BufferedInputStream(fis, IPCConstants.PIPE_BUFFER_SIZE);

        if (isBidirectional) {
            // Server writes to response pipe (blocks until client opens it for reading)
            FileOutputStream fos = new FileOutputStream(responseFifoPath);
            this.outputStream = new BufferedOutputStream(fos, IPCConstants.PIPE_BUFFER_SIZE);
        }

        this.connected = true;
    }

    @Override
    public synchronized void connectClient() throws IOException {
        if (isServer) {
            throw new IllegalStateException("Cannot call connectClient() on an endpoint configured as server.");
        }
        if (closed) {
            throw new IOException("IPC endpoint is already closed.");
        }

        long startTime = System.currentTimeMillis();
        long timeout = IPCConstants.DEFAULT_CONNECT_TIMEOUT_MS;

        // Wait until server has created the FIFO file
        File forwardFile = new File(forwardFifoPath);
        while (!forwardFile.exists() && (System.currentTimeMillis() - startTime < timeout)) {
            try {
                Thread.sleep(IPCConstants.RETRY_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Client connection interrupted while waiting for FIFO to exist.", ie);
            }
        }

        if (!forwardFile.exists()) {
            throw new IOException("Timed out waiting for FIFO '" + forwardFifoPath + "' to be created by server.");
        }

        boolean isBidirectional = !channelName.equalsIgnoreCase(IPCConstants.CHANNEL_CORE_LOGGER);

        // Client opens forward pipe for writing (unblocks server's read open)
        FileOutputStream fos = new FileOutputStream(forwardFifoPath);
        this.outputStream = new BufferedOutputStream(fos, IPCConstants.PIPE_BUFFER_SIZE);

        if (isBidirectional) {
            // Client opens response pipe for reading (unblocks server's write open)
            FileInputStream fis = new FileInputStream(responseFifoPath);
            this.inputStream = new BufferedInputStream(fis, IPCConstants.PIPE_BUFFER_SIZE);
        }

        this.connected = true;
    }

    @Override
    public void sendMessage(IPCMessage message) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException("Cannot send null IPCMessage.");
        }
        if (!connected || outputStream == null) {
            throw new IOException("Cannot send message: Linux FIFO is not connected.");
        }

        synchronized (writeLock) {
            try {
                message.writeDelimitedTo(outputStream);
            } catch (IOException e) {
                this.connected = false;
                throw new IOException("Failed to write frame to Linux FIFO '" + forwardFifoPath + "': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public IPCMessage receiveMessage() throws IOException {
        if (!connected || inputStream == null) {
            throw new IOException("Cannot receive message: Linux FIFO is not connected.");
        }

        synchronized (readLock) {
            try {
                return IPCMessage.readDelimitedFrom(inputStream);
            } catch (EOFException e) {
                this.connected = false;
                return null; // Remote end closed pipe
            } catch (IOException e) {
                this.connected = false;
                throw new IOException("Failed to read frame from Linux FIFO: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected && !closed;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    public String getForwardFifoPath() {
        return forwardFifoPath;
    }

    public String getResponseFifoPath() {
        return responseFifoPath;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        connected = false;

        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (Exception ignored) {
            }
            outputStream = null;
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {
            }
            inputStream = null;
        }

        // If server, safely clean up the created FIFO special files
        if (isServer) {
            deleteFifoFile(forwardFifoPath);
            deleteFifoFile(responseFifoPath);
        }
    }

    /**
     * Safely deletes a FIFO file without deleting non-fifo or directory items.
     */
    private static void deleteFifoFile(String pathStr) {
        if (pathStr == null) return;
        try {
            Path path = Paths.get(pathStr);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                Files.delete(path);
            }
        } catch (Exception ignored) {
        }
    }
}
