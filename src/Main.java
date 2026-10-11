import java.io.IOException;
import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Central coordinator and entry point for the PIC16F72 Microcontroller Simulator.
 *
 * Coordinates application startup and role-based process execution across the
 * three independent operating-system processes:
 * 1. Logger Process (--role=logger)
 * 2. Core Simulator Process (--role=core)
 * 3. UI Process (--role=ui)
 *
 * When executed without role arguments or with --role=manager, this class functions
 * as the Process Manager, bootstrapping and monitoring the three independent OS processes.
 */
public class Main {

    public static void main(String[] args) {
        String role = parseRole(args);

        if (IPCConstants.ROLE_LOGGER.equalsIgnoreCase(role)) {
            // Role: Dedicated Logger Process
            LoggerProcess.main(args);

        } else if (IPCConstants.ROLE_CORE.equalsIgnoreCase(role)) {
            // Role: Dedicated Core Simulator Process
            CoreProcess.main(args);

        } else if (IPCConstants.ROLE_UI.equalsIgnoreCase(role)) {
            // Role: Dedicated UI Process
            launchUIProcess();

        } else if ("standalone".equalsIgnoreCase(role)) {
            // Optional fallback: Run entire simulator inside a single process
            launchStandaloneUI();

        } else {
            // Default role: Process Manager coordinating multi-process execution
            launchMultiProcessCoordinator();
        }
    }

    /**
     * Parses the role command-line argument (--role=<role>).
     */
    private static String parseRole(String[] args) {
        if (args == null) return null;
        for (String arg : args) {
            if (arg != null && arg.toLowerCase().startsWith(IPCConstants.ARG_ROLE_PREFIX)) {
                return arg.substring(IPCConstants.ARG_ROLE_PREFIX.length()).trim().toLowerCase();
            }
        }
        return null;
    }

    /**
     * Launches the UI process communicating with the Core simulator over IPC.
     */
    private static void launchUIProcess() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[UI] Connecting to Core simulator on channel: " + IPCConstants.CHANNEL_UI_CORE);
                    IPCManager ipcClient = IPCFactory.createClient(IPCConstants.CHANNEL_UI_CORE);
                    ipcClient.connectClient();
                    SimulatorUI ui = new SimulatorUI(ipcClient);
                    ui.setVisible(true);
                } catch (IOException ex) {
                    System.err.println("[UI ERROR] Failed to connect to Core via IPC: " + ex.getMessage());
                    System.err.println("[UI] Falling back to local in-memory CPU controller...");
                    launchStandaloneUI();
                }
            }
        });
    }

    /**
     * Fallback single-process execution for local testing without IPC pipes.
     */
    private static void launchStandaloneUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Registers registers = new Registers();
                Memory memory = new Memory();
                CPU cpu = new CPU(registers, memory);
                SimulatorUI ui = new SimulatorUI(cpu);
                ui.setVisible(true);
            }
        });
    }

    /**
     * Coordinates startup and lifecycle for all three independent OS processes.
     */
    private static void launchMultiProcessCoordinator() {
        try {
            ProcessManager pm = new ProcessManager();
            pm.launchAll();
            pm.waitForUI();
        } catch (Exception ex) {
            System.err.println("[MAIN ERROR] Error in Multi-Process Coordinator: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
