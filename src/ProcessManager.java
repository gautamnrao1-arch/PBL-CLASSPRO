import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ProcessManager.java
 *
 * Responsible for launching, monitoring, and cleanly terminating the independent
 * operating-system processes (Logger, Core, and UI) for the PIC16F72 Virtual Simulator.
 */
public class ProcessManager {

    private final Map<String, Process> processes = Collections.synchronizedMap(new LinkedHashMap<String, Process>());
    private final String javaExecutable;
    private final String classpath;

    public ProcessManager() {
        this.javaExecutable = resolveJavaExecutable();
        this.classpath = resolveClasspath();
        registerShutdownHook();
    }

    /**
     * Resolves the full path to the java executable powering the current runtime.
     */
    private static String resolveJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String binDir = javaHome + File.separator + "bin" + File.separator;
        File javaExe = new File(binDir + "java.exe");
        if (javaExe.exists()) {
            return javaExe.getAbsolutePath();
        }
        File javaBin = new File(binDir + "java");
        if (javaBin.exists()) {
            return javaBin.getAbsolutePath();
        }
        return "java";
    }

    /**
     * Resolves the current application classpath.
     */
    private static String resolveClasspath() {
        String cp = System.getProperty("java.class.path");
        if (cp == null || cp.trim().isEmpty()) {
            return "bin" + File.pathSeparator + ".";
        }
        // Ensure bin is included if working from project root
        if (!cp.contains("bin") && new File("bin").isDirectory()) {
            return "bin" + File.pathSeparator + cp;
        }
        return cp;
    }

    /**
     * Launches an individual operating system process for the specified role.
     *
     * @param role Target process role ("logger", "core", "ui")
     * @return The spawned {@link Process}
     * @throws IOException if process spawning fails
     */
    public Process launchRole(String role) throws IOException {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Process role cannot be null or empty.");
        }
        String cleanRole = role.trim().toLowerCase();

        List<String> command = new ArrayList<String>();
        command.add(javaExecutable);
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-cp");
        command.add(classpath);
        command.add("Main");
        command.add(IPCConstants.ARG_ROLE_PREFIX + cleanRole);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();

        System.out.println(String.format("[PROCESS_MANAGER] Spawning process for role '%s'...", cleanRole));
        Process proc = pb.start();
        processes.put(cleanRole, proc);
        return proc;
    }

    /**
     * Orchestrates startup for all three independent processes in the correct dependency order:
     * 1. Logger Process (binds "core_logger" server)
     * 2. Core Simulator Process (connects to Logger, binds "ui_core" server)
     * 3. UI Process (connects to Core)
     *
     * @throws IOException if any process fails to spawn
     * @throws InterruptedException if interrupted during inter-process startup spacing
     */
    public void launchAll() throws IOException, InterruptedException {
        System.out.println("=======================================================");
        System.out.println(" PIC16F72 Virtual Simulator - Multi-Process Launch    ");
        System.out.println("=======================================================");
        System.out.println("Host Operating System: " + IPCFactory.getOperatingSystemName());
        System.out.println("Using Classpath:       " + classpath);
        System.out.println("-------------------------------------------------------");

        // 1. Launch Logger process
        System.out.println("[1/3] Starting Logger Process...");
        launchRole(IPCConstants.ROLE_LOGGER);
        Thread.sleep(600); // Allow Logger server pipe to bind

        // 2. Launch Core process
        System.out.println("[2/3] Starting Core Simulator Process...");
        launchRole(IPCConstants.ROLE_CORE);
        Thread.sleep(600); // Allow Core server pipe to bind and connect to Logger

        // 3. Launch UI process
        System.out.println("[3/3] Starting UI Process...");
        launchRole(IPCConstants.ROLE_UI);

        System.out.println("=======================================================");
        System.out.println(" All three independent OS processes successfully launched.");
        System.out.println("=======================================================");
    }

    /**
     * Waits for the UI process to terminate (indicating the user has closed the application),
     * and coordinates graceful termination of all child processes.
     */
    public void waitForUI() {
        Process uiProc = processes.get(IPCConstants.ROLE_UI);
        if (uiProc != null) {
            try {
                uiProc.waitFor();
                System.out.println("[PROCESS_MANAGER] UI process terminated. Shutting down child processes...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        shutdownAll();
    }

    /**
     * Gracefully shuts down all spawned processes, falling back to forceful termination
     * if they do not exit within {@link IPCConstants#SHUTDOWN_TIMEOUT_MS}.
     */
    public synchronized void shutdownAll() {
        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            String role = entry.getKey();
            Process proc = entry.getValue();
            if (proc.isAlive()) {
                System.out.println("[PROCESS_MANAGER] Terminating " + role + " process...");
                proc.destroy();
                try {
                    boolean exited = proc.waitFor(IPCConstants.SHUTDOWN_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (!exited) {
                        System.out.println("[PROCESS_MANAGER] Forcefully destroying " + role + " process...");
                        proc.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    proc.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
        }
        processes.clear();
    }

    /**
     * Checks if all launched processes are currently alive.
     */
    public boolean areProcessesAlive() {
        if (processes.isEmpty()) return false;
        for (Process proc : processes.values()) {
            if (!proc.isAlive()) return false;
        }
        return true;
    }

    public Process getProcess(String role) {
        return processes.get(role);
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownAll();
            }
        }, "ProcessManager-ShutdownHook"));
    }
}
