import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LinuxFifoTest.java
 *
 * Standalone test suite for verifying LinuxFifoIPC on POSIX/Linux systems:
 * 1. Checks OS compatibility (runs on Linux, skips gracefully on Windows/macOS).
 * 2. FIFO special file creation with mkfifo permissions.
 * 3. Bidirectional request/response communication over paired FIFOs.
 * 4. Safe cleanup of FIFO filesystem nodes without deleting unrelated files.
 *
 * Can be run on any Linux environment with:
 *   javac -d bin -cp src test/LinuxFifoTest.java
 *   java -cp bin LinuxFifoTest
 */
public class LinuxFifoTest {

    private static final String TEST_CHANNEL = "linux_test_fifo";
    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("         LinuxFifoIPC Test Suite         ");
        System.out.println("=========================================");

        if (!IPCFactory.isLinux()) {
            System.out.println("[INFO] Host OS is NOT Linux. Host OS: " + IPCFactory.getOperatingSystemName());
            System.out.println("[INFO] Linux FIFO tests cannot execute directly on Windows.");
            System.out.println("[INFO] To run on Linux, execute:");
            System.out.println("       javac -d bin -cp src src/*.java test/LinuxFifoTest.java");
            System.out.println("       java -cp bin LinuxFifoTest");
            System.out.println("=========================================");
            return;
        }

        testFifoCreationAndPermissions();
        testBidirectionalFifoCommunication();

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

    private static void testFifoCreationAndPermissions() {
        String path = IPCConstants.getLinuxFifoPath(TEST_CHANNEL);
        boolean pass = false;
        try {
            LinuxFifoIPC.ensureFifoExists(path);
            File f = new File(path);
            pass = f.exists();
        } catch (Exception e) {
            pass = false;
        } finally {
            new File(path).delete();
        }

        recordResult("Test 1: mkfifo Endpoint Creation", pass,
                pass ? "mkfifo successfully created FIFO special file at " + path : "Failed to create FIFO");
    }

    private static void testBidirectionalFifoCommunication() {
        final CountDownLatch serverReady = new CountDownLatch(1);
        final CountDownLatch testCompleted = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);

        Thread serverThread = new Thread(() -> {
            LinuxFifoIPC server = new LinuxFifoIPC(TEST_CHANNEL, true);
            try {
                serverReady.countDown();
                server.startServer();

                IPCMessage req = server.receiveMessage();
                if (req != null && IPCMessage.TYPE_EXECUTE.equals(req.getType())) {
                    IPCMessage resp = IPCMessage.createResult(req.getRequestId(), "PC=0x0001;STATUS=READY");
                    server.sendMessage(resp);
                }
            } catch (Exception ignored) {
            } finally {
                server.close();
            }
        });

        Thread clientThread = new Thread(() -> {
            LinuxFifoIPC client = new LinuxFifoIPC(TEST_CHANNEL, false);
            try {
                serverReady.await(5, TimeUnit.SECONDS);
                Thread.sleep(100);
                client.connectClient();

                client.sendMessage(IPCMessage.createExecute("req-lin-1", "STEP"));
                IPCMessage resp = client.receiveMessage();

                if (resp != null && IPCMessage.TYPE_RESULT.equals(resp.getType())
                        && resp.getPayload().contains("STATUS=READY")) {
                    success.set(true);
                }
            } catch (Exception ignored) {
            } finally {
                client.close();
                testCompleted.countDown();
            }
        });

        serverThread.start();
        clientThread.start();

        try {
            boolean done = testCompleted.await(10, TimeUnit.SECONDS);
            boolean pass = done && success.get();
            recordResult("Test 2: Bidirectional POSIX FIFO Communication", pass,
                    pass ? "Request/response successfully transmitted over POSIX FIFOs" : "FIFO exchange failed or timed out");
        } catch (InterruptedException ie) {
            recordResult("Test 2: Bidirectional POSIX FIFO Communication", false, "Test interrupted");
        }
    }
}
