import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * IPCFactory.java
 *
 * Operating-system-aware factory for creating IPCManager instances in the PIC16F72
 * Virtual Simulator.
 *
 * Responsibilities:
 * 1. Detects the host operating system (Windows vs. Linux).
 * 2. Instantiates the appropriate genuine OS pipe backend:
 *    - Windows -> WindowsNamedPipeIPC (Windows Named Pipes via JNA Kernel32)
 *    - Linux   -> LinuxFifoIPC (POSIX FIFOs / mkfifo)
 * 3. Rejects unsupported operating systems (e.g., macOS, BSD, Solaris) with explicit errors.
 * 4. Strictly avoids non-pipe fallbacks (TCP/UDP sockets and in-memory queues are prohibited).
 * 5. Uses dynamic loading so this factory compiles and validates dependencies cleanly
 *    without requiring dummy or fake backend implementations before they are built.
 */
public final class IPCFactory {

    public static final String WINDOWS_BACKEND_CLASS = "WindowsNamedPipeIPC";
    public static final String LINUX_BACKEND_CLASS   = "LinuxFifoIPC";

    // Private constructor for utility factory class
    private IPCFactory() {}

    /**
     * Creates an IPCManager configured for the given channel as a server (listening/accepting).
     *
     * @param channelName Logical channel name (e.g. IPCConstants.CHANNEL_UI_CORE)
     * @return OS-specific IPCManager server instance
     * @throws UnsupportedOperationException if OS is unsupported or backend is not implemented
     */
    public static IPCManager createServer(String channelName) {
        return create(channelName, true);
    }

    /**
     * Creates an IPCManager configured for the given channel as a client (connecting to server).
     *
     * @param channelName Logical channel name (e.g. IPCConstants.CHANNEL_UI_CORE)
     * @return OS-specific IPCManager client instance
     * @throws UnsupportedOperationException if OS is unsupported or backend is not implemented
     */
    public static IPCManager createClient(String channelName) {
        return create(channelName, false);
    }

    /**
     * Creates an IPCManager instance for the detected operating system.
     *
     * @param channelName Logical channel name
     * @param isServer True to configure as pipe server, false as client
     * @return Concrete IPCManager implementation
     * @throws UnsupportedOperationException if OS is unsupported or required backend is missing
     */
    public static IPCManager create(String channelName, boolean isServer) {
        if (channelName == null || channelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty.");
        }

        String osName = getOperatingSystemName();

        if (isWindows()) {
            return instantiateBackend(WINDOWS_BACKEND_CLASS, channelName.trim(), isServer, "Windows Named Pipes");
        } else if (isLinux()) {
            return instantiateBackend(LINUX_BACKEND_CLASS, channelName.trim(), isServer, "Linux POSIX FIFOs");
        } else {
            throw new UnsupportedOperationException(
                String.format("Unsupported operating system: '%s'. " +
                    "The PIC16F72 Virtual Simulator requires Windows (Named Pipes via JNA) or Linux (POSIX FIFOs). " +
                    "TCP/UDP socket substitutions and in-memory fallbacks are strictly prohibited.", osName)
            );
        }
    }

    /**
     * Returns the host operating system name string reported by the JVM.
     *
     * @return Operating system name (e.g. "Windows 11", "Linux")
     */
    public static String getOperatingSystemName() {
        return System.getProperty("os.name", "Unknown");
    }

    /**
     * Returns true if running on a Microsoft Windows operating system.
     *
     * @return true if Windows
     */
    public static boolean isWindows() {
        String os = getOperatingSystemName().toLowerCase();
        return os.contains("win");
    }

    /**
     * Returns true if running on a Linux operating system.
     *
     * @return true if Linux
     */
    public static boolean isLinux() {
        String os = getOperatingSystemName().toLowerCase();
        return os.contains("linux");
    }

    /**
     * Checks if the required backend class for the current operating system is present
     * and loadable on the application classpath.
     *
     * @return true if the backend class is available, false otherwise
     */
    public static boolean isBackendAvailable() {
        String targetClass = isWindows() ? WINDOWS_BACKEND_CLASS : (isLinux() ? LINUX_BACKEND_CLASS : null);
        if (targetClass == null) {
            return false;
        }
        try {
            Class.forName(targetClass);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Instantiates the requested backend via reflection.
     * Clearly identifies missing implementations with informative exceptions rather than
     * masking errors or inventing fake implementations.
     */
    private static IPCManager instantiateBackend(String className, String channelName, boolean isServer, String backendDescription) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!IPCManager.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Class " + className + " does not implement IPCManager.");
            }
            Constructor<?> ctor = clazz.getConstructor(String.class, boolean.class);
            return (IPCManager) ctor.newInstance(channelName, isServer);
        } catch (ClassNotFoundException ex) {
            throw new UnsupportedOperationException(
                String.format("The required %s backend class '%s' is not yet implemented or not present on the classpath. " +
                    "Please compile the %s backend before starting processes.",
                    backendDescription, className, className), ex);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(
                String.format("Backend class '%s' is missing the required constructor: (String channelName, boolean isServer).", className), ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new RuntimeException("Failed to initialize " + className + ": " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Reflection error initializing " + className + ": " + ex.getMessage(), ex);
        }
    }
}
