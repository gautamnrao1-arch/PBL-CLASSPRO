import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * LinuxFifoIPC.java
 *
 * Implements genuine POSIX FIFOs (named pipes) for the PIC16F72 Virtual Simulator
 * on Linux / POSIX systems.
 *
 * Conforms to the {@link IPCManager} interface.
 */
public class LinuxFifoIPC implements IPCManager {

    private final String channelName;
    private final boolean isServer;
    private final String requestFifoPath;
    private final String responseFifoPath;
    private final boolean isBidirectional;

    private InputStream inputStream;
    private OutputStream outputStream;

    private volatile boolean connected = false;
    private volatile boolean closed = false;

    private final Object sendLock = new Object();
    private final Object receiveLock = new Object();

    /**
     * Constructs a Linux POSIX FIFO IPC endpoint.
     *
     * @param channelName Logical channel name (e.g. "ui_core", "core_logger")
     * @param isServer True for server role, false for client role
     */
    public LinuxFifoIPC(String channelName, boolean isServer) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty.");
        }
        this.channelName = channelName.trim();
        this.isServer = isServer;
        this.requestFifoPath = IPCConstants.getLinuxFifoPath(this.channelName);
        this.responseFifoPath = IPCConstants.getLinuxFifoResponsePath(this.channelName);
        // CHANNEL_UI_CORE is bidirectional; CHANNEL_CORE_LOGGER is unidirectional event log
        this.isBidirectional = !IPCConstants.CHANNEL_CORE_LOGGER.equalsIgnoreCase(this.channelName);
    }

    @Override
    public synchronized void startServer() throws IOException {
        if (closed) {
            throw new IOException("Cannot start server on closed channel: " + channelName);
        }
        if (connected) {
            return;
        }

        createFifoIfAbsent(requestFifoPath);
        if (isBidirectional) {
            createFifoIfAbsent(responseFifoPath);
        }

        // Server reads from request FIFO (blocks until client opens for writing)
        inputStream = new FileInputStream(requestFifoPath);

        if (isBidirectional) {
            // Server writes to response FIFO (blocks until client opens for reading)
            outputStream = new FileOutputStream(responseFifoPath);
        }

        connected = true;
    }

    @Override
    public synchronized void connectClient() throws IOException {
        if (closed) {
            throw new IOException("Cannot connect client on closed channel: " + channelName);
        }
        if (connected) {
            return;
        }

        long deadline = System.currentTimeMillis() + IPCConstants.DEFAULT_CONNECT_TIMEOUT_MS;
        File reqFile = new File(requestFifoPath);

        // Wait until server has created the FIFO files
        while (!reqFile.exists() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(IPCConstants.RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Connection interrupted while waiting for FIFO server", e);
            }
        }

        if (!reqFile.exists()) {
            throw new IOException("Timed out waiting for FIFO file '" + requestFifoPath + "'");
        }

        // Client writes to request FIFO (matches server reading)
        outputStream = new FileOutputStream(requestFifoPath);

        if (isBidirectional) {
            // Client reads from response FIFO (matches server writing)
            inputStream = new FileInputStream(responseFifoPath);
        }

        connected = true;
    }

    @Override
    public void sendMessage(IPCMessage message) throws IOException {
        if (!isConnected()) {
            throw new IOException("Cannot send message: FIFO '" + channelName + "' is not connected.");
        }
        if (outputStream == null) {
            throw new IOException("Channel '" + channelName + "' is read-only at this endpoint.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        synchronized (sendLock) {
            message.writeDelimitedTo(outputStream);
        }
    }

    @Override
    public IPCMessage receiveMessage() throws IOException {
        if (!isConnected()) {
            throw new IOException("Cannot receive message: FIFO '" + channelName + "' is not connected.");
        }
        if (inputStream == null) {
            throw new IOException("Channel '" + channelName + "' is write-only at this endpoint.");
        }
        synchronized (receiveLock) {
            try {
                return IPCMessage.readDelimitedFrom(inputStream);
            } catch (IOException e) {
                if (closed || !connected) {
                    return null;
                }
                throw e;
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

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        connected = false;

        IOException failure = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ex) {
                failure = ex;
            } finally {
                inputStream = null;
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ex) {
                if (failure == null) failure = ex;
            } finally {
                outputStream = null;
            }
        }

        // Clean up FIFO files on server termination
        if (isServer) {
            new File(requestFifoPath).delete();
            if (isBidirectional) {
                new File(responseFifoPath).delete();
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void createFifoIfAbsent(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            try {
                Process proc = new ProcessBuilder("mkfifo", path).start();
                int exitCode = proc.waitFor();
                if (exitCode != 0 && !f.exists()) {
                    throw new IOException("Failed to create POSIX FIFO at '" + path + "' (mkfifo exit code " + exitCode + ")");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while executing mkfifo for " + path, e);
            }
        }
    }
}
