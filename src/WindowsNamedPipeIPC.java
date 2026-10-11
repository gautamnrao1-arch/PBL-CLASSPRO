import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * WindowsNamedPipeIPC.java
 *
 * Implements genuine Windows Named Pipes for the PIC16F72 Virtual Simulator
 * using the Win32 Kernel32 API via Java Native Linker (FFM API).
 *
 * Conforms to the {@link IPCManager} interface.
 */
public class WindowsNamedPipeIPC implements IPCManager {

    // Win32 Named Pipe & File Constants
    private static final int PIPE_ACCESS_DUPLEX        = 0x00000003;
    private static final int PIPE_TYPE_BYTE            = 0x00000000;
    private static final int PIPE_READMODE_BYTE        = 0x00000000;
    private static final int PIPE_WAIT                 = 0x00000000;
    private static final int PIPE_UNLIMITED_INSTANCES  = 255;

    private static final int GENERIC_READ              = 0x80000000;
    private static final int GENERIC_WRITE             = 0x40000000;
    private static final int OPEN_EXISTING             = 3;

    private static final int ERROR_FILE_NOT_FOUND      = 2;
    private static final int ERROR_BROKEN_PIPE         = 109;
    private static final int ERROR_PIPE_BUSY           = 231;
    private static final int ERROR_PIPE_NOT_CONNECTED  = 233;
    private static final int ERROR_PIPE_CONNECTED      = 535;

    private static final long INVALID_HANDLE_VALUE     = -1L;

    // Kernel32 native method handles
    private static final MethodHandle createNamedPipeWHandle;
    private static final MethodHandle connectNamedPipeHandle;
    private static final MethodHandle disconnectNamedPipeHandle;
    private static final MethodHandle createFileWHandle;
    private static final MethodHandle waitNamedPipeWHandle;
    private static final MethodHandle readFileHandle;
    private static final MethodHandle writeFileHandle;
    private static final MethodHandle flushFileBuffersHandle;
    private static final MethodHandle closeHandleHandle;
    private static final MethodHandle getLastErrorHandle;

    static {
        Linker linker = Linker.nativeLinker();
        SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

        createNamedPipeWHandle = linker.downcallHandle(
            kernel32.find("CreateNamedPipeW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );

        connectNamedPipeHandle = linker.downcallHandle(
            kernel32.find("ConnectNamedPipe").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        disconnectNamedPipeHandle = linker.downcallHandle(
            kernel32.find("DisconnectNamedPipe").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );

        createFileWHandle = linker.downcallHandle(
            kernel32.find("CreateFileW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS)
        );

        waitNamedPipeWHandle = linker.downcallHandle(
            kernel32.find("WaitNamedPipeW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );

        readFileHandle = linker.downcallHandle(
            kernel32.find("ReadFile").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        writeFileHandle = linker.downcallHandle(
            kernel32.find("WriteFile").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        flushFileBuffersHandle = linker.downcallHandle(
            kernel32.find("FlushFileBuffers").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );

        closeHandleHandle = linker.downcallHandle(
            kernel32.find("CloseHandle").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );

        getLastErrorHandle = linker.downcallHandle(
            kernel32.find("GetLastError").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT)
        );
    }

    private final String channelName;
    private final boolean isServer;
    private final String fullPipePath;

    private MemorySegment pipeHandle = null;
    private volatile boolean connected = false;
    private volatile boolean closed = false;

    private InputStream inputStream;
    private OutputStream outputStream;

    private final Object sendLock = new Object();
    private final Object receiveLock = new Object();

    /**
     * Constructs a Windows Named Pipe IPC endpoint.
     *
     * @param channelName Logical channel name (e.g. "ui_core", "core_logger")
     * @param isServer True for server role, false for client role
     */
    public WindowsNamedPipeIPC(String channelName, boolean isServer) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty.");
        }
        this.channelName = channelName.trim();
        this.isServer = isServer;
        this.fullPipePath = IPCConstants.getWindowsPipeName(this.channelName);
    }

    @Override
    public synchronized void startServer() throws IOException {
        if (closed) {
            throw new IOException("Cannot start server on closed channel: " + channelName);
        }
        if (connected) {
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pipeNameSeg = arena.allocateFrom(fullPipePath, StandardCharsets.UTF_16LE);

            int openMode = PIPE_ACCESS_DUPLEX;
            int pipeMode = PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT;
            int bufferSize = IPCConstants.PIPE_BUFFER_SIZE;

            pipeHandle = (MemorySegment) createNamedPipeWHandle.invokeExact(
                pipeNameSeg,
                openMode,
                pipeMode,
                PIPE_UNLIMITED_INSTANCES,
                bufferSize,
                bufferSize,
                5000,
                MemorySegment.NULL
            );

            if (pipeHandle == null || pipeHandle.address() == INVALID_HANDLE_VALUE || pipeHandle.address() == 0) {
                int err = getLastError();
                throw new IOException("Failed to create named pipe '" + fullPipePath + "' (Win32 Error: " + err + ")");
            }

            // Block and wait for client to connect
            int connectResult = (int) connectNamedPipeHandle.invokeExact(pipeHandle, MemorySegment.NULL);
            if (connectResult == 0) {
                int err = getLastError();
                // ERROR_PIPE_CONNECTED indicates the client connected between CreateNamedPipe and ConnectNamedPipe
                if (err != ERROR_PIPE_CONNECTED) {
                    throw new IOException("ConnectNamedPipe failed for '" + fullPipePath + "' (Win32 Error: " + err + ")");
                }
            }

            inputStream = new NamedPipeInputStream();
            outputStream = new NamedPipeOutputStream();
            connected = true;
        } catch (Throwable t) {
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException("Native error starting named pipe server: " + t.getMessage(), t);
        }
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
        int desiredAccess = GENERIC_READ | GENERIC_WRITE;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pipeNameSeg = arena.allocateFrom(fullPipePath, StandardCharsets.UTF_16LE);

            while (System.currentTimeMillis() < deadline) {
                MemorySegment handle = (MemorySegment) createFileWHandle.invokeExact(
                    pipeNameSeg,
                    desiredAccess,
                    0,
                    MemorySegment.NULL,
                    OPEN_EXISTING,
                    0,
                    MemorySegment.NULL
                );

                if (handle != null && handle.address() != INVALID_HANDLE_VALUE && handle.address() != 0) {
                    this.pipeHandle = handle;
                    this.inputStream = new NamedPipeInputStream();
                    this.outputStream = new NamedPipeOutputStream();
                    this.connected = true;
                    return;
                }

                int err = getLastError();
                if (err == ERROR_PIPE_BUSY) {
                    // Wait for pipe instance to become available
                    try {
                        waitNamedPipeWHandle.invokeExact(pipeNameSeg, IPCConstants.RETRY_INTERVAL_MS);
                    } catch (Throwable ignored) {}
                } else if (err == ERROR_FILE_NOT_FOUND) {
                    // Server pipe not yet created, wait before retrying
                    try {
                        Thread.sleep(IPCConstants.RETRY_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted while waiting for pipe server", e);
                    }
                } else {
                    try {
                        Thread.sleep(IPCConstants.RETRY_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted", e);
                    }
                }
            }

            throw new IOException("Timed out connecting to named pipe server '" + fullPipePath +
                    "' after " + IPCConstants.DEFAULT_CONNECT_TIMEOUT_MS + " ms");
        } catch (Throwable t) {
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException("Native error connecting to named pipe: " + t.getMessage(), t);
        }
    }

    @Override
    public void sendMessage(IPCMessage message) throws IOException {
        if (!isConnected()) {
            throw new IOException("Cannot send message: pipe '" + channelName + "' is not connected.");
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
            throw new IOException("Cannot receive message: pipe '" + channelName + "' is not connected.");
        }
        synchronized (receiveLock) {
            try {
                return IPCMessage.readDelimitedFrom(inputStream);
            } catch (IOException e) {
                // If remote peer cleanly disconnected
                if (closed || !connected) {
                    return null;
                }
                throw e;
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected && !closed && pipeHandle != null && pipeHandle.address() != 0 && pipeHandle.address() != INVALID_HANDLE_VALUE;
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

        if (pipeHandle != null && pipeHandle.address() != 0 && pipeHandle.address() != INVALID_HANDLE_VALUE) {
            try {
                if (isServer) {
                    disconnectNamedPipeHandle.invokeExact(pipeHandle);
                }
                closeHandleHandle.invokeExact(pipeHandle);
            } catch (Throwable ignored) {
            } finally {
                pipeHandle = null;
            }
        }
    }

    private static int getLastError() {
        try {
            return (int) getLastErrorHandle.invokeExact();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Input stream backed by Win32 ReadFile.
     */
    private class NamedPipeInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int n = read(single, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return single[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) throw new NullPointerException("Buffer is null");
            if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
            if (len == 0) return 0;
            if (closed || pipeHandle == null) throw new IOException("Pipe handle is closed");

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment readBuf = arena.allocate(len);
                MemorySegment bytesRead = arena.allocate(ValueLayout.JAVA_INT);

                int ok = (int) readFileHandle.invokeExact(pipeHandle, readBuf, len, bytesRead, MemorySegment.NULL);
                int count = bytesRead.get(ValueLayout.JAVA_INT, 0);

                if (ok == 0) {
                    int err = getLastError();
                    if (err == ERROR_BROKEN_PIPE || err == ERROR_PIPE_NOT_CONNECTED) {
                        connected = false;
                        return -1; // Clean EOF
                    }
                    throw new IOException("ReadFile failed on pipe '" + channelName + "' (Win32 Error: " + err + ")");
                }

                if (count == 0) {
                    return -1; // EOF
                }

                MemorySegment.copy(readBuf, 0, MemorySegment.ofArray(b), off, count);
                return count;
            } catch (Throwable t) {
                if (t instanceof IOException) throw (IOException) t;
                throw new IOException("Native error during ReadFile: " + t.getMessage(), t);
            }
        }
    }

    /**
     * Output stream backed by Win32 WriteFile.
     */
    private class NamedPipeOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) throw new NullPointerException("Buffer is null");
            if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
            if (len == 0) return;
            if (closed || pipeHandle == null) throw new IOException("Pipe handle is closed");

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment writeBuf = arena.allocate(len);
                MemorySegment.copy(MemorySegment.ofArray(b), off, writeBuf, 0, len);
                MemorySegment bytesWritten = arena.allocate(ValueLayout.JAVA_INT);

                int ok = (int) writeFileHandle.invokeExact(pipeHandle, writeBuf, len, bytesWritten, MemorySegment.NULL);
                if (ok == 0) {
                    int err = getLastError();
                    if (err == ERROR_BROKEN_PIPE || err == ERROR_PIPE_NOT_CONNECTED) {
                        connected = false;
                        throw new IOException("Broken pipe during write on '" + channelName + "'");
                    }
                    throw new IOException("WriteFile failed on pipe '" + channelName + "' (Win32 Error: " + err + ")");
                }
            } catch (Throwable t) {
                if (t instanceof IOException) throw (IOException) t;
                throw new IOException("Native error during WriteFile: " + t.getMessage(), t);
            }
        }

        @Override
        public void flush() throws IOException {
            if (pipeHandle != null && isConnected()) {
                try {
                    flushFileBuffersHandle.invokeExact(pipeHandle);
                } catch (Throwable ignored) {}
            }
        }
    }
}
