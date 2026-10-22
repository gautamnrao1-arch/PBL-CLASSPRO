import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * WindowsNamedPipeIPC.java
 *
 * Windows backend implementation of the shared IPCManager interface using
 * genuine Win32 Named Pipes via JNA Kernel32.
 *
 * Pipe endpoints are formatted under "\\.\pipe\pic16f72_<channel>" as specified
 * in IPCConstants.
 *
 * Concurrency & Thread-safety:
 * - Read and Write operations use dedicated locks to prevent interleaving of frames.
 * - Handles native handles cleanly on close or error.
 */
public class WindowsNamedPipeIPC implements IPCManager {

    private final String channelName;
    private final String pipePath;
    private final boolean isServer;

    private HANDLE pipeHandle = WinBase.INVALID_HANDLE_VALUE;
    private InputStream inputStream;
    private OutputStream outputStream;

    private volatile boolean connected = false;
    private volatile boolean closed = false;

    private final Object writeLock = new Object();
    private final Object readLock = new Object();

    /**
     * Constructs a Windows Named Pipe IPC endpoint.
     *
     * @param channelName Logical channel name (e.g., "ui_core", "core_logger")
     * @param isServer True to act as pipe server (CreateNamedPipe / ConnectNamedPipe),
     *                 false to act as pipe client (CreateFile)
     */
    public WindowsNamedPipeIPC(String channelName, boolean isServer) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty.");
        }
        this.channelName = channelName.trim();
        this.isServer = isServer;
        this.pipePath = IPCConstants.getWindowsPipeName(this.channelName);
    }

    @Override
    public synchronized void startServer() throws IOException {
        if (!isServer) {
            throw new IllegalStateException("Cannot call startServer() on an endpoint configured as client.");
        }
        if (closed) {
            throw new IOException("IPC endpoint is already closed.");
        }

        // Win32 Named Pipe parameters
        int openMode = WinBase.PIPE_ACCESS_DUPLEX; // Bidirectional
        int pipeMode = WinBase.PIPE_TYPE_BYTE | WinBase.PIPE_READMODE_BYTE | WinBase.PIPE_WAIT; // Byte stream mode, blocking
        int maxInstances = WinBase.PIPE_UNLIMITED_INSTANCES;
        int outBufferSize = IPCConstants.PIPE_BUFFER_SIZE;
        int inBufferSize = IPCConstants.PIPE_BUFFER_SIZE;
        int defaultTimeout = IPCConstants.DEFAULT_CONNECT_TIMEOUT_MS;

        pipeHandle = Kernel32.INSTANCE.CreateNamedPipe(
                pipePath,
                openMode,
                pipeMode,
                maxInstances,
                outBufferSize,
                inBufferSize,
                defaultTimeout,
                null
        );

        if (pipeHandle == null || WinBase.INVALID_HANDLE_VALUE.equals(pipeHandle)) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new IOException("Failed to create Win32 Named Pipe '" + pipePath + "'. Win32 Error: " + err);
        }

        // Wait for a client process to connect
        boolean connectSuccess = Kernel32.INSTANCE.ConnectNamedPipe(pipeHandle, null);
        if (!connectSuccess) {
            int err = Kernel32.INSTANCE.GetLastError();
            if (err == WinError.ERROR_PIPE_CONNECTED) {
                // Client connected between CreateNamedPipe and ConnectNamedPipe calls
                connectSuccess = true;
            } else {
                close();
                throw new IOException("ConnectNamedPipe failed on '" + pipePath + "'. Win32 Error: " + err);
            }
        }

        this.inputStream = new NamedPipeInputStream(pipeHandle);
        this.outputStream = new NamedPipeOutputStream(pipeHandle);
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

        while (System.currentTimeMillis() - startTime < timeout) {
            if (closed) {
                throw new IOException("Connection canceled because endpoint was closed.");
            }

            pipeHandle = Kernel32.INSTANCE.CreateFile(
                    pipePath,
                    WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                    0, // No sharing
                    null,
                    WinNT.OPEN_EXISTING,
                    0,
                    null
            );

            if (pipeHandle != null && !WinBase.INVALID_HANDLE_VALUE.equals(pipeHandle)) {
                // Successfully connected
                this.inputStream = new NamedPipeInputStream(pipeHandle);
                this.outputStream = new NamedPipeOutputStream(pipeHandle);
                this.connected = true;
                return;
            }

            int lastError = Kernel32.INSTANCE.GetLastError();
            if (lastError == WinError.ERROR_PIPE_BUSY) {
                // All pipe instances are busy; wait for one to become available
                Kernel32.INSTANCE.WaitNamedPipe(pipePath, IPCConstants.RETRY_INTERVAL_MS);
            } else {
                try {
                    Thread.sleep(IPCConstants.RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Client connection interrupted while waiting for pipe.", ie);
                }
            }
        }

        throw new IOException("Timed out connecting to Windows Named Pipe '" + pipePath + "' after " + timeout + " ms.");
    }

    @Override
    public void sendMessage(IPCMessage message) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException("Cannot send null IPCMessage.");
        }
        if (!connected || outputStream == null) {
            throw new IOException("Cannot send message: Pipe is not connected.");
        }

        synchronized (writeLock) {
            try {
                message.writeDelimitedTo(outputStream);
            } catch (IOException e) {
                this.connected = false;
                throw new IOException("Failed to send message over pipe '" + pipePath + "': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public IPCMessage receiveMessage() throws IOException {
        if (!connected || inputStream == null) {
            throw new IOException("Cannot receive message: Pipe is not connected.");
        }

        synchronized (readLock) {
            try {
                return IPCMessage.readDelimitedFrom(inputStream);
            } catch (EOFException e) {
                // Remote process closed or disconnected pipe
                this.connected = false;
                return null;
            } catch (IOException e) {
                this.connected = false;
                throw new IOException("Failed to read message from pipe '" + pipePath + "': " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected && !closed && pipeHandle != null && !WinBase.INVALID_HANDLE_VALUE.equals(pipeHandle);
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    public String getPipePath() {
        return pipePath;
    }

    public boolean isServer() {
        return isServer;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        connected = false;

        if (pipeHandle != null && !WinBase.INVALID_HANDLE_VALUE.equals(pipeHandle)) {
            if (isServer) {
                // Disconnect server pipe before closing handle
                try {
                    Kernel32.INSTANCE.DisconnectNamedPipe(pipeHandle);
                } catch (Exception ignored) {
                }
            }
            try {
                Kernel32.INSTANCE.CloseHandle(pipeHandle);
            } catch (Exception ignored) {
            }
            pipeHandle = WinBase.INVALID_HANDLE_VALUE;
        }

        inputStream = null;
        outputStream = null;
    }

    // =========================================================================
    // Win32 Named Pipe Native I/O Streams
    // =========================================================================

    /**
     * InputStream implementation reading raw bytes directly from the Win32 Named Pipe handle.
     */
    private static class NamedPipeInputStream extends InputStream {
        private final HANDLE handle;

        public NamedPipeInputStream(HANDLE handle) {
            this.handle = handle;
        }

        @Override
        public int read() throws IOException {
            byte[] singleByte = new byte[1];
            int read = read(singleByte, 0, 1);
            if (read == -1) {
                return -1;
            }
            return singleByte[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            if (handle == null || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
                throw new IOException("Pipe handle is closed.");
            }

            byte[] tempBuffer;
            if (off == 0) {
                tempBuffer = b;
            } else {
                tempBuffer = new byte[len];
            }

            IntByReference bytesRead = new IntByReference(0);
            boolean success = Kernel32.INSTANCE.ReadFile(
                    handle,
                    tempBuffer,
                    len,
                    bytesRead,
                    null
            );

            int count = bytesRead.getValue();
            if (!success) {
                int err = Kernel32.INSTANCE.GetLastError();
                if (err == WinError.ERROR_BROKEN_PIPE || err == WinError.ERROR_PIPE_NOT_CONNECTED || err == WinError.ERROR_HANDLE_EOF) {
                    return -1; // End of file / pipe disconnected
                }
                throw new IOException("Win32 ReadFile on Named Pipe failed with error: " + err);
            }

            if (count == 0) {
                return -1;
            }

            if (off != 0) {
                System.arraycopy(tempBuffer, 0, b, off, count);
            }

            return count;
        }

        @Override
        public void close() throws IOException {
            // Managed by outer WindowsNamedPipeIPC
        }
    }

    /**
     * OutputStream implementation writing raw bytes directly to the Win32 Named Pipe handle.
     */
    private static class NamedPipeOutputStream extends OutputStream {
        private final HANDLE handle;

        public NamedPipeOutputStream(HANDLE handle) {
            this.handle = handle;
        }

        @Override
        public void write(int b) throws IOException {
            byte[] single = new byte[]{(byte) b};
            write(single, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }

            if (handle == null || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
                throw new IOException("Pipe handle is closed.");
            }

            byte[] tempBuffer;
            if (off == 0 && len == b.length) {
                tempBuffer = b;
            } else {
                tempBuffer = new byte[len];
                System.arraycopy(b, off, tempBuffer, 0, len);
            }

            int bytesWrittenTotal = 0;
            IntByReference written = new IntByReference(0);

            while (bytesWrittenTotal < len) {
                int toWrite = len - bytesWrittenTotal;
                byte[] chunk;
                if (bytesWrittenTotal == 0 && toWrite == tempBuffer.length) {
                    chunk = tempBuffer;
                } else {
                    chunk = new byte[toWrite];
                    System.arraycopy(tempBuffer, bytesWrittenTotal, chunk, 0, toWrite);
                }

                boolean success = Kernel32.INSTANCE.WriteFile(
                        handle,
                        chunk,
                        toWrite,
                        written,
                        null
                );

                if (!success) {
                    int err = Kernel32.INSTANCE.GetLastError();
                    throw new IOException("Win32 WriteFile on Named Pipe failed with error: " + err);
                }

                int currentWritten = written.getValue();
                if (currentWritten <= 0) {
                    throw new IOException("Win32 WriteFile wrote 0 bytes to pipe.");
                }
                bytesWrittenTotal += currentWritten;
            }
        }

        @Override
        public void flush() throws IOException {
            if (handle != null && !WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
                Kernel32.INSTANCE.FlushFileBuffers(handle);
            }
        }

        @Override
        public void close() throws IOException {
            // Managed by outer WindowsNamedPipeIPC
        }
    }
}
