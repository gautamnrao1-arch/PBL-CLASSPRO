import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger.java
 *
 * Dedicated logging utility for the independent Logger process in the
 * PIC16F72 Virtual Simulator.
 *
 * Architectural & Design Characteristics:
 * 1. Provides a clean, typed API for recording timestamped events, warnings,
 *    errors, and simulator actions.
 * 2. Writes logs to a configurable file location (defaults to {@link IPCConstants#DEFAULT_LOG_FILE}).
 * 3. Formats entries with timestamp, level/event type, requestId (when available), and message text.
 * 4. Gracefully handles file creation and I/O failures with fallback reporting without throwing unhandled exceptions.
 * 5. Guarantees immediate flushing to disk to prevent loss of log messages during crashes or shutdowns.
 * 6. Directly compatible with {@link IPCMessage} structure and {@link LoggerProcess}.
 * 7. Strictly isolated: does NOT communicate with UI directly, and does NOT create, modify,
 *    or own any CPU, register, memory, or stack state.
 * 8. Zero third-party dependencies (pure standard Java SE runtime).
 */
public class Logger implements AutoCloseable {

    public static final String LEVEL_INFO    = "INFO";
    public static final String LEVEL_WARN    = "WARN";
    public static final String LEVEL_ERROR   = "ERROR";
    public static final String LEVEL_ACTION  = "ACTION";
    public static final String LEVEL_LOG     = "LOG";
    public static final String LEVEL_INIT    = "LOGGER_INIT";
    public static final String LEVEL_SHUTDOWN= "LOGGER_SHUTDOWN";

    private final String logFilePath;
    private final boolean echoToConsole;
    private final SimpleDateFormat dateFormat;
    private final Object writeLock = new Object();

    private PrintWriter writer;
    private boolean initialized = false;
    private boolean failed = false;

    /**
     * Constructs a Logger pointing to the default log file location (pic16f72_simulator.log)
     * with console echoing enabled.
     */
    public Logger() {
        this(IPCConstants.DEFAULT_LOG_FILE, true);
    }

    /**
     * Constructs a Logger with a configurable log file path and console echoing enabled.
     *
     * @param logFilePath Path to the target log file
     */
    public Logger(String logFilePath) {
        this(logFilePath, true);
    }

    /**
     * Constructs a Logger with a configurable log file path and selectable console echoing.
     *
     * @param logFilePath Path to the target log file
     * @param echoToConsole Whether to mirror log entries to System.out
     */
    public Logger(String logFilePath, boolean echoToConsole) {
        this.logFilePath = (logFilePath != null && !logFilePath.trim().isEmpty())
                ? logFilePath.trim()
                : IPCConstants.DEFAULT_LOG_FILE;
        this.echoToConsole = echoToConsole;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        initWriter();
    }

    /**
     * Initializes the underlying file writer, creating parent directories if needed.
     * Fails gracefully without terminating the application.
     */
    private void initWriter() {
        synchronized (writeLock) {
            try {
                File file = new File(logFilePath);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                FileWriter fw = new FileWriter(file, true);
                BufferedWriter bw = new BufferedWriter(fw);
                this.writer = new PrintWriter(bw, true); // autoflush = true
                this.initialized = true;
                this.failed = false;
            } catch (IOException e) {
                this.failed = true;
                this.writer = null;
                System.err.println("[LOGGER ERROR] Failed to initialize log file '" + logFilePath + "': " + e.getMessage());
            }
        }
    }

    /**
     * Records a generic event with an explicit level/type and message.
     *
     * @param level Event type / severity level
     * @param message Text description of the event
     */
    public void log(String level, String message) {
        log(level, null, message);
    }

    /**
     * Records an event with level, request ID (correlation ID), and message.
     *
     * @param level Event type / severity level
     * @param requestId Correlation ID from incoming request, or null if none
     * @param message Text description of the event
     */
    public void log(String level, String requestId, String message) {
        log(System.currentTimeMillis(), level, requestId, message);
    }

    /**
     * Records an event with full metadata including explicit epoch timestamp.
     *
     * @param timestampMillis Epoch millisecond timestamp
     * @param level Event type / severity level
     * @param requestId Correlation ID, or null
     * @param message Text description of the event
     */
    public void log(long timestampMillis, String level, String requestId, String message) {
        String safeLevel = (level != null && !level.trim().isEmpty()) ? level.trim() : LEVEL_INFO;
        String safeMsg = (message != null) ? message : "";
        String timestampStr;
        synchronized (dateFormat) {
            timestampStr = dateFormat.format(new Date(timestampMillis));
        }

        StringBuilder entry = new StringBuilder();
        entry.append("[").append(timestampStr).append("] ");
        entry.append("[").append(safeLevel).append("] ");
        if (requestId != null && !requestId.trim().isEmpty()) {
            entry.append("[req=").append(requestId.trim()).append("] ");
        }
        entry.append(safeMsg);

        String formatted = entry.toString();

        synchronized (writeLock) {
            if (echoToConsole) {
                System.out.println(formatted);
            }

            if (writer != null) {
                try {
                    writer.println(formatted);
                    writer.flush();
                } catch (Exception e) {
                    System.err.println("[LOGGER ERROR] Writing entry failed: " + e.getMessage());
                }
            } else if (!failed) {
                // Try lazy re-init once if previously uninitialized
                initWriter();
                if (writer != null) {
                    writer.println(formatted);
                    writer.flush();
                }
            }
        }
    }

    /**
     * Logs directly from an incoming IPCMessage envelope, preserving the original
     * message timestamp, type, request ID, and payload.
     *
     * @param msg The received IPCMessage
     */
    public void logIPCMessage(IPCMessage msg) {
        if (msg == null) return;
        log(msg.getTimestamp(), msg.getType(), msg.getRequestId(), msg.getPayload());
    }

    /**
     * Records an informational event.
     */
    public void info(String message) {
        log(LEVEL_INFO, message);
    }

    /**
     * Records a warning event.
     */
    public void warn(String message) {
        log(LEVEL_WARN, message);
    }

    /**
     * Records an error event.
     */
    public void error(String message) {
        log(LEVEL_ERROR, message);
    }

    /**
     * Records an error event with an associated throwable.
     */
    public void error(String message, Throwable throwable) {
        String detail = (throwable != null)
                ? message + " (" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ")"
                : message;
        log(LEVEL_ERROR, detail);
    }

    /**
     * Records a simulator action (e.g. LOAD, STEP, RUN, RESET).
     */
    public void action(String actionName, String details) {
        String msg = (details != null && !details.isEmpty()) ? actionName + ": " + details : actionName;
        log(LEVEL_ACTION, msg);
    }

    /**
     * Flushes buffered log lines to the persistent file immediately.
     */
    public void flush() {
        synchronized (writeLock) {
            if (writer != null) {
                writer.flush();
            }
        }
    }

    /**
     * Returns the configured path to the log file.
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Checks whether the underlying file writer is currently open and ready.
     */
    public boolean isInitialized() {
        return initialized && writer != null;
    }

    /**
     * Closes the logger, flushing all pending writes and releasing file handles.
     */
    @Override
    public void close() {
        synchronized (writeLock) {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (Exception ignored) {
                } finally {
                    writer = null;
                    initialized = false;
                }
            }
        }
    }
}
