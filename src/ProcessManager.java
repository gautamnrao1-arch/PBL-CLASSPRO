import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ProcessManager.java
 *
 * Responsible for launching, monitoring, capturing output from, and cleanly
 * terminating the independent operating-system processes (Logger, Core, and UI)
 * for the PIC16F72 Virtual Simulator.
 *
 * Architectural & Operational Capabilities:
 * 1. Resolves java executable, working directory, and classpath dynamically without
 *    hardcoding machine-specific absolute paths.
 * 2. Launches child JVMs using {@link ProcessBuilder} with role flags (--role=logger, --role=core, --role=ui).
 * 3. Prevents recursive application launching by enforcing explicit single-role execution.
 * 4. Captures standard output and standard error using dedicated background reader threads,
 *    preventing OS pipe buffer deadlocks.
 * 5. Handles partial startup failure by cleaning up any already-started child processes.
 * 6. Implements bounded graceful shutdown (destroy() with configurable timeout), falling
 *    back to forceful termination (destroyForcibly()) if unresponsive.
 * 7. Closes native streams and releases resources cleanly.
 */
public class ProcessManager {

    private final Map<String, Process> processes = Collections.synchronizedMap(new LinkedHashMap<String, Process>());
    private final List<Thread> outputReaderThreads = Collections.synchronizedList(new ArrayList<Thread>());
    private final String javaExecutable;
    private final String classpath;
    private final File workingDirectory;
    private volatile boolean shuttingDown = false;

    public ProcessManager() {
        this.javaExecutable = resolveJavaExecutable();
        this.workingDirectory = resolveWorkingDirectory();
        this.classpath = resolveClasspath();
        registerShutdownHook();
    }

    /**
     * Resolves the java binary for the active runtime.
     */
    public static String resolveJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            File binDir = new File(javaHome, "bin");
            File javaExe = new File(binDir, "java.exe");
            if (javaExe.exists()) {
                return javaExe.getAbsolutePath();
            }
            File javaBin = new File(binDir, "java");
            if (javaBin.exists()) {
                return javaBin.getAbsolutePath();
            }
        }
        return "java";
    }

    /**
     * Resolves the current working directory.
     */
    public static File resolveWorkingDirectory() {
        return new File(System.getProperty("user.dir", "."));
    }

    /**
     * Resolves the classpath including project bin and lib directories portably.
     */
    public static String resolveClasspath() {
        String baseCp = System.getProperty("java.class.path");
        StringBuilder cpBuilder = new StringBuilder();

        // Include bin directory
        File binDir = new File("bin");
        if (binDir.isDirectory()) {
            cpBuilder.append("bin");
        }

        // Include all JARs in lib directory
        File libDir = new File("lib");
        if (libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    if (cpBuilder.length() > 0) {
                        cpBuilder.append(File.pathSeparator);
                    }
                    cpBuilder.append(jar.getPath());
                }
            }
        }

        // Append base JVM classpath if present
        if (baseCp != null && !baseCp.trim().isEmpty()) {
            if (cpBuilder.length() > 0) {
                cpBuilder.append(File.pathSeparator);
            }
            cpBuilder.append(baseCp);
        }

        return cpBuilder.length() > 0 ? cpBuilder.toString() : ".";
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
        pb.directory(workingDirectory);

        System.out.println(String.format("[PROCESS_MANAGER] Spawning process for role '%s'...", cleanRole));
        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            System.err.println(String.format("[PROCESS_MANAGER ERROR] Failed to start '%s': %s", cleanRole, e.getMessage()));
            throw e;
        }

        processes.put(cleanRole, proc);

        // Capture stdout and stderr asynchronously to prevent buffer blocking
        startStreamDrainer(proc.getInputStream(), "[" + cleanRole.toUpperCase() + " OUT] ");
        startStreamDrainer(proc.getErrorStream(), "[" + cleanRole.toUpperCase() + " ERR] ");

        return proc;
    }

    /**
     * Drains an InputStream in a background daemon thread to prevent process stalls.
     */
    private void startStreamDrainer(InputStream stream, String prefix) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!shuttingDown) {
                        System.out.println(prefix + line);
                    }
                }
            } catch (IOException ignored) {
            }
        }, "StreamDrainer-" + prefix.trim());
        t.setDaemon(true);
        t.start();
        outputReaderThreads.add(t);
    }

    /**
     * Orchestrates startup for all three independent processes in dependency order:
     * 1. Logger Process (binds "core_logger" server)
     * 2. Core Simulator Process (connects to Logger, binds "ui_core" server)
     * 3. UI Process (connects to Core)
     *
     * In the event of a partial failure, any started processes are terminated cleanly.
     *
     * @throws IOException if any process fails to spawn or crashes immediately
     * @throws InterruptedException if interrupted during startup delays
     */
    public void launchAll() throws IOException, InterruptedException {
        System.out.println("=======================================================");
        System.out.println(" PIC16F72 Virtual Simulator - Multi-Process Launch    ");
        System.out.println("=======================================================");
        System.out.println("Host Operating System: " + IPCFactory.getOperatingSystemName());
        System.out.println("Java Executable:       " + javaExecutable);
        System.out.println("Classpath:             " + classpath);
        System.out.println("-------------------------------------------------------");

        try {
            // 1. Launch Logger process
            System.out.println("[1/3] Starting Logger Process...");
            Process loggerProc = launchRole(IPCConstants.ROLE_LOGGER);
            Thread.sleep(800); // Allow Logger server pipe to bind

            if (!loggerProc.isAlive()) {
                throw new IOException("Logger process terminated unexpectedly during startup. Exit code: " + loggerProc.exitValue());
            }

            // 2. Launch Core process
            System.out.println("[2/3] Starting Core Simulator Process...");
            Process coreProc = launchRole(IPCConstants.ROLE_CORE);
            Thread.sleep(800); // Allow Core server pipe to bind and connect to Logger

            if (!coreProc.isAlive()) {
                throw new IOException("Core process terminated unexpectedly during startup. Exit code: " + coreProc.exitValue());
            }

            // 3. Launch UI process
            System.out.println("[3/3] Starting UI Process...");
            Process uiProc = launchRole(IPCConstants.ROLE_UI);
            Thread.sleep(300);

            if (!uiProc.isAlive()) {
                throw new IOException("UI process terminated unexpectedly during startup. Exit code: " + uiProc.exitValue());
            }

            System.out.println("=======================================================");
            System.out.println(" All three independent OS processes successfully launched.");
            System.out.println("=======================================================");

        } catch (Exception e) {
            System.err.println("[PROCESS_MANAGER ERROR] Multi-process startup failure: " + e.getMessage());
            // Cleanup partial startup
            shutdownAll();
            throw e;
        }
    }

    /**
     * Waits for the UI process to terminate (indicating the user has closed the application),
     * and coordinates graceful termination of remaining child processes.
     */
    public void waitForUI() {
        Process uiProc = processes.get(IPCConstants.ROLE_UI);
        if (uiProc != null) {
            try {
                int exitCode = uiProc.waitFor();
                System.out.println("[PROCESS_MANAGER] UI process exited with code " + exitCode + ". Shutting down remaining processes...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        shutdownAll();
    }

    /**
     * Gracefully terminates all spawned processes within {@link IPCConstants#SHUTDOWN_TIMEOUT_MS},
     * falling back to destroyForcibly() if unresponsive, and closes process streams.
     */
    public synchronized void shutdownAll() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;

        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            String role = entry.getKey();
            Process proc = entry.getValue();

            if (proc.isAlive()) {
                System.out.println("[PROCESS_MANAGER] Requesting graceful termination of " + role + " process...");
                proc.destroy();
                try {
                    boolean exited = proc.waitFor(IPCConstants.SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (!exited) {
                        System.out.println("[PROCESS_MANAGER] Process " + role + " did not terminate in time. Forcing termination...");
                        proc.destroyForcibly();
                        proc.waitFor(1000, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException e) {
                    proc.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }

            // Close standard streams
            try { proc.getInputStream().close(); } catch (Exception ignored) {}
            try { proc.getErrorStream().close(); } catch (Exception ignored) {}
            try { proc.getOutputStream().close(); } catch (Exception ignored) {}
        }

        processes.clear();
        System.out.println("[PROCESS_MANAGER] All child processes stopped and resources released.");
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

    public Map<String, Process> getActiveProcesses() {
        return Collections.unmodifiableMap(processes);
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
