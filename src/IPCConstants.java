import java.io.File;

/**
 * IPCConstants.java
 *
 * Centralizes configuration, channel names, timeouts, framing parameters,
 * process roles, and state keys for the PIC16F72 Virtual Simulator.
 *
 * Architecture Channels:
 * - UI <-> Core: Bidirectional command/response channel (CHANNEL_UI_CORE)
 * - Core -> Logger: One-way logging event stream (CHANNEL_CORE_LOGGER)
 *
 * Cross-Platform Endpoint Mapping:
 * Logical channel names are decoupled from OS-specific endpoints.
 * - WindowsNamedPipeIPC maps logical channels to Windows Named Pipes (\\.\pipe\pic16f72_<channel>).
 * - LinuxFifoIPC maps logical channels to POSIX FIFO file paths (/tmp/pic16f72_<channel>.fifo).
 */
public final class IPCConstants {

    // Private constructor to prevent instantiation of utility class
    private IPCConstants() {}

    // =========================================================================
    // 1. Logical Channel Names
    // =========================================================================

    /**
     * Primary bidirectional command/response channel between UI and Core.
     * UI sends commands (LOAD, STEP, RUN, RESET, STATE); Core returns RESULT or ERROR.
     */
    public static final String CHANNEL_UI_CORE = "ui_core";

    /**
     * Unidirectional event logging channel from Core (and UI) to Logger process.
     * Core publishes timestamped audit logs; Logger persists them to disk.
     */
    public static final String CHANNEL_CORE_LOGGER = "core_logger";

    // =========================================================================
    // 2. OS-Specific Endpoint Path Resolvers
    // =========================================================================

    /**
     * Base prefix for Windows Named Pipes.
     */
    public static final String WINDOWS_PIPE_PREFIX = "\\\\.\\pipe\\";

    /**
     * Prefix added to named pipes to prevent naming collisions on the OS.
     */
    public static final String PIPE_NAMESPACE = "pic16f72_";

    /**
     * Maps a logical channel name to a valid Windows Named Pipe endpoint.
     * Example: "ui_core" -> "\\.\pipe\pic16f72_ui_core"
     *
     * @param channel Logical channel name
     * @return Windows Named Pipe path
     */
    public static String getWindowsPipeName(String channel) {
        String safeName = (channel != null) ? channel.trim().toLowerCase() : "default";
        return WINDOWS_PIPE_PREFIX + PIPE_NAMESPACE + safeName;
    }

    /**
     * Maps a logical channel name to a valid Linux / POSIX FIFO filesystem path.
     * Defaults to the system temp directory or /tmp.
     * Example: "ui_core" -> "/tmp/pic16f72_ui_core.fifo"
     *
     * @param channel Logical channel name
     * @return POSIX FIFO filesystem path
     */
    public static String getLinuxFifoPath(String channel) {
        String safeName = (channel != null) ? channel.trim().toLowerCase() : "default";
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        if (!tmpDir.endsWith(File.separator) && !tmpDir.endsWith("/")) {
            tmpDir += File.separator;
        }
        return tmpDir + PIPE_NAMESPACE + safeName + ".fifo";
    }

    /**
     * Maps a logical channel name to a secondary reverse-direction FIFO path for Linux,
     * allowing full-duplex communication over unidirectional POSIX FIFOs.
     * Example: "ui_core" -> "/tmp/pic16f72_ui_core_resp.fifo"
     *
     * @param channel Logical channel name
     * @return POSIX response FIFO filesystem path
     */
    public static String getLinuxFifoResponsePath(String channel) {
        String safeName = (channel != null) ? channel.trim().toLowerCase() : "default";
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        if (!tmpDir.endsWith(File.separator) && !tmpDir.endsWith("/")) {
            tmpDir += File.separator;
        }
        return tmpDir + PIPE_NAMESPACE + safeName + "_resp.fifo";
    }

    // =========================================================================
    // 3. Timeouts & Connection Parameters
    // =========================================================================

    /**
     * Default connection timeout in milliseconds when a client attempts to connect to a pipe.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 15000;

    /**
     * Default read timeout in milliseconds for blocking stream reads.
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 10000;

    /**
     * Interval in milliseconds between connection retry attempts.
     */
    public static final int RETRY_INTERVAL_MS = 250;

    /**
     * Maximum time in milliseconds to wait for child processes to exit gracefully during shutdown.
     */
    public static final int SHUTDOWN_TIMEOUT_MS = 5000;

    // =========================================================================
    // 4. Framing & Buffer Sizing
    // =========================================================================

    /**
     * Maximum frame size allowed for an individual IPC message (10 Megabytes).
     */
    public static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;

    /**
     * Default buffer size in bytes for stream read/write operations (64 KB).
     */
    public static final int PIPE_BUFFER_SIZE = 64 * 1024;

    // =========================================================================
    // 5. Standard Commands
    // =========================================================================

    public static final String CMD_LOAD      = "LOAD";
    public static final String CMD_STEP      = "STEP";
    public static final String CMD_RUN       = "RUN";
    public static final String CMD_RESET     = "RESET";
    public static final String CMD_STATE     = "STATE";
    public static final String CMD_SHUTDOWN  = "SHUTDOWN";

    // =========================================================================
    // 6. Process Roles & Command Line Arguments
    // =========================================================================

    public static final String ROLE_UI       = "ui";
    public static final String ROLE_CORE     = "core";
    public static final String ROLE_LOGGER   = "logger";
    public static final String ROLE_MANAGER  = "manager";

    public static final String ARG_ROLE_PREFIX = "--role=";

    // =========================================================================
    // 7. Logging Configuration
    // =========================================================================

    /**
     * Default log file name written by the Logger process.
     */
    public static final String DEFAULT_LOG_FILE = "pic16f72_simulator.log";

    public static final String LOG_LEVEL_INFO  = "INFO";
    public static final String LOG_LEVEL_WARN  = "WARN";
    public static final String LOG_LEVEL_ERROR = "ERROR";
    public static final String LOG_LEVEL_DEBUG = "DEBUG";

    // =========================================================================
    // 8. State Snapshot Keys (Core <-> UI Data Dictionary)
    // =========================================================================

    public static final String KEY_STATUS_STR   = "STATUS_STR";
    public static final String KEY_PC           = "PC";
    public static final String KEY_W            = "W";
    public static final String KEY_STATUS_REG   = "STATUS_REG";
    public static final String KEY_FLAG_Z       = "FLAG_Z";
    public static final String KEY_FLAG_C       = "FLAG_C";
    public static final String KEY_FLAG_DC      = "FLAG_DC";
    public static final String KEY_CURRENT_INST = "CURRENT_INST";
    public static final String KEY_TRACE        = "TRACE";
    public static final String KEY_REG_DUMP     = "REG_DUMP";
    public static final String KEY_MEM_DUMP     = "MEM_DUMP";
    public static final String KEY_STACK_STATE  = "STACK_STATE";
    public static final String KEY_QUEUE_STATE  = "QUEUE_STATE";
    public static final String KEY_PROGRAM_VIEW = "PROGRAM_VIEW";
}
