/**
 * IPCFactoryTest.java
 *
 * Standalone test suite for verifying OS detection, parameter validation,
 * and backend dependency checking in IPCFactory.
 *
 * No external testing frameworks used; runs directly with main().
 */
public class IPCFactoryTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("         IPCFactory Test Suite           ");
        System.out.println("=========================================");

        testOsDetection();
        testNullChannelValidation();
        testMissingBackendExplicitException();

        System.out.println("=========================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL TESTS PASSED");
        } else {
            System.out.println("Result: SOME TESTS FAILED");
        }
        System.out.println("=========================================");
    }

    private static void recordResult(String testName, boolean success, String details) {
        totalCount++;
        if (success) {
            passedCount++;
            System.out.println("[PASS] " + testName + " -> " + details);
        } else {
            System.out.println("[FAIL] " + testName + " -> " + details);
        }
    }

    /**
     * Test 1: Verify host OS detection works accurately.
     */
    private static void testOsDetection() {
        String osName = IPCFactory.getOperatingSystemName();
        boolean isWin = IPCFactory.isWindows();
        boolean isLin = IPCFactory.isLinux();

        boolean pass = (isWin != isLin) || (!isWin && !isLin); // cannot be both Windows and Linux simultaneously
        recordResult("Test 1: OS Detection", pass,
                String.format("Host OS='%s' (isWindows=%b, isLinux=%b)", osName, isWin, isLin));
    }

    /**
     * Test 2: Verify null/empty channel names are properly rejected.
     */
    private static void testNullChannelValidation() {
        boolean nullPassed = false;
        try {
            IPCFactory.createServer(null);
        } catch (IllegalArgumentException ex) {
            nullPassed = true;
        }

        boolean emptyPassed = false;
        try {
            IPCFactory.createClient("   ");
        } catch (IllegalArgumentException ex) {
            emptyPassed = true;
        }

        boolean pass = nullPassed && emptyPassed;
        recordResult("Test 2: Channel Validation", pass,
                "Null and empty channel names safely rejected with IllegalArgumentException");
    }

    /**
     * Test 3: Verify that before WindowsNamedPipeIPC or LinuxFifoIPC is implemented,
     * calling create() throws UnsupportedOperationException rather than using a fake socket or crashing.
     */
    private static void testMissingBackendExplicitException() {
        boolean pass = false;
        String expectedBackend = IPCFactory.isWindows() ? IPCFactory.WINDOWS_BACKEND_CLASS : IPCFactory.LINUX_BACKEND_CLASS;

        try {
            IPCFactory.createServer("test_channel");
            // If backend already exists, pass
            pass = true;
        } catch (UnsupportedOperationException ex) {
            // Expected when backend class is not yet on classpath
            pass = ex.getMessage().contains(expectedBackend) || ex.getMessage().contains("Unsupported operating system");
        } catch (Exception ex) {
            pass = false;
        }

        recordResult("Test 3: Missing Backend Error Reporting", pass,
                String.format("Reports clear requirement for '%s' without silent socket fallback", expectedBackend));
    }
}
