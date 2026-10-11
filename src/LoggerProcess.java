import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LoggerProcess.java
 *
 * Runs as an independent operating-system process responsible for central audit logging.
 *
 * Architectural Constraints:
 * - Does NOT own or modify CPU state.
 * - Receives log and status events from the Core simulator over {@link IPCConstants#CHANNEL_CORE_LOGGER}.
 * - Persists all events to disk in {@link IPCConstants#DEFAULT_LOG_FILE}.
 */
public class LoggerProcess {

    private final String logFileName;
    private volatile boolean running = true;

    public LoggerProcess() {
        this(IPCConstants.DEFAULT_LOG_FILE);
    }

    public LoggerProcess(String logFileName) {
        this.logFileName = (logFileName != null && !logFileName.trim().isEmpty())
                ? logFileName.trim()
                : IPCConstants.DEFAULT_LOG_FILE;
    }

    /**
     * Main entry point when invoked as a standalone process.
     */
    public static void main(String[] args) {
        System.out.println("[LOGGER] Starting Logger Process...");
        LoggerProcess logger = new LoggerProcess();
        logger.run();
    }

    /**
     * Executes the logger process lifecycle: binds IPC server, receives log frames,
     * writes formatted entries to file and stdout, and terminates cleanly on SHUTDOWN.
     */
    public void run() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        System.out.println("[LOGGER] Binding server on channel: " + IPCConstants.CHANNEL_CORE_LOGGER);

        try (IPCManager ipcServer = IPCFactory.createServer(IPCConstants.CHANNEL_CORE_LOGGER);
             FileWriter fw = new FileWriter(logFileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter logWriter = new PrintWriter(bw, true)) {

            ipcServer.startServer();
            String startMsg = String.format("[%s] [LOGGER_INIT] Connected to Core process. Logging to '%s'",
                    dateFormat.format(new Date()), logFileName);
            System.out.println(startMsg);
            logWriter.println(startMsg);

            while (running && ipcServer.isConnected()) {
                IPCMessage msg = ipcServer.receiveMessage();
                if (msg == null) {
                    System.out.println("[LOGGER] End-of-stream reached. Remote process disconnected.");
                    break;
                }

                String timestampStr = dateFormat.format(new Date(msg.getTimestamp()));
                String logEntry = String.format("[%s] [%s] %s", timestampStr, msg.getType(), msg.getPayload());

                System.out.println(logEntry);
                logWriter.println(logEntry);

                if (IPCMessage.TYPE_SHUTDOWN.equalsIgnoreCase(msg.getType())) {
                    System.out.println("[LOGGER] Shutdown message received. Terminating Logger process.");
                    break;
                }
            }

            String stopMsg = String.format("[%s] [LOGGER_SHUTDOWN] Logger process cleanly shut down.", dateFormat.format(new Date()));
            System.out.println(stopMsg);
            logWriter.println(stopMsg);

        } catch (IOException e) {
            System.err.println("[LOGGER ERROR] " + e.getMessage());
        }
    }

    public void stop() {
        this.running = false;
    }
}
