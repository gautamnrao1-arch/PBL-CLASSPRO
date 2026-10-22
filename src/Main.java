import java.io.IOException;
import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Central coordinator and entry point for the PIC16F72 Microcontroller Simulator.
 *
 * Implements role-based process bootstrapping across three independent operating-system processes:
 * 1. Logger Process (--role=logger):
 *    Spawns LoggerProcess, which owns the log file writer and listens on CHANNEL_CORE_LOGGER.
 * 2. Core Simulator Process (--role=core):
 *    Spawns CoreProcess, which owns the authoritative CPU, Registers, Memory, Stack, and Queue.
 * 3. UI Process (--role=ui):
 *    Spawns SimulatorUI, which contains no simulation state and communicates strictly via IPC.
 *
 * When launched without arguments (or with --role=manager), Main functions as the ProcessManager
 * coordinator, spawning the child processes in dependency order and supervising their lifecycle.
 */
public class Main {

    public static void main(String[] args) {
        String role = parseRole(args);

        if (IPCConstants.ROLE_LOGGER.equalsIgnoreCase(role)) {
            // Child Role 1: Dedicated Logger Process
            LoggerProcess.main(args);

        } else if (IPCConstants.ROLE_CORE.equalsIgnoreCase(role)) {
            // Child Role 2: Dedicated Core Simulator Process
            CoreProcess.main(args);

        } else if (IPCConstants.ROLE_UI.equalsIgnoreCase(role)) {
            // Child Role 3: Dedicated UI Process (Pure GUI, communicating over IPC)
            launchUIProcess();

        } else {
            // Default Master Role: ProcessManager coordinating independent OS processes
            launchProcessManager();
        }
    }

    /**
     * Parses the role command-line argument (--role=<role> or role=<role>).
     *
     * @param args Command-line arguments
     * @return Normalized lowercase role name, or null if none specified
     */
    public static String parseRole(String[] args) {
        if (args == null) return null;
        for (String arg : args) {
            if (arg == null) continue;
            String trimmed = arg.trim();
            if (trimmed.toLowerCase().startsWith(IPCConstants.ARG_ROLE_PREFIX)) {
                return trimmed.substring(IPCConstants.ARG_ROLE_PREFIX.length()).trim().toLowerCase();
            }
            if (trimmed.toLowerCase().startsWith("role=")) {
                return trimmed.substring("role=".length()).trim().toLowerCase();
            }
        }
        return null;
    }

    /**
     * Boots the UI process on the Swing Event Dispatch Thread (EDT).
     * The UI connects to Core over the shared IPC abstraction selected by IPCFactory.
     */
    private static void launchUIProcess() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[UI PROCESS] Connecting to Core simulator over channel: " + IPCConstants.CHANNEL_UI_CORE);
                    IPCManager ipcClient = IPCFactory.createClient(IPCConstants.CHANNEL_UI_CORE);
                    ipcClient.connectClient();
                    System.out.println("[UI PROCESS] Successfully connected to Core simulator.");

                    SimulatorUI ui = new SimulatorUI(ipcClient);
                    ui.setVisible(true);
                } catch (IOException ex) {
                    System.err.println("[UI PROCESS ERROR] Could not connect to Core process via IPC: " + ex.getMessage());
                    System.err.println("[UI PROCESS ERROR] Ensure the Core process is running before starting UI directly.");
                    System.exit(1);
                } catch (Exception ex) {
                    System.err.println("[UI PROCESS ERROR] Unexpected error initializing UI: " + ex.getMessage());
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }

    /**
     * Bootstraps the ProcessManager to spawn and coordinate the independent OS processes.
     */
    private static void launchProcessManager() {
        try {
            System.out.println("[MAIN] Initializing PIC16F72 Multi-Process Architecture...");
            ProcessManager pm = new ProcessManager();
            pm.launchAll();
            pm.waitForUI();
        } catch (Exception ex) {
            System.err.println("[MAIN ERROR] ProcessManager failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
