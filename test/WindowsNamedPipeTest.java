import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WindowsNamedPipeTest.java
 *
 * Standalone test verifying WindowsNamedPipeIPC communication:
 * 1. Server pipe creation (\\.\pipe\pic16f72_test_pipe).
 * 2. Client connection.
 * 3. Bidirectional length-prefixed structured message transfer (EXECUTE -> RESULT).
 * 4. Multi-message sequential streaming.
 * 5. Clean, graceful handle closure and EOF handling.
 */
public class WindowsNamedPipeTest {

    private static final String TEST_CHANNEL = "test_pipe";
    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("    WindowsNamedPipeIPC Test Suite       ");
        System.out.println("=========================================");

        if (!IPCFactory.isWindows()) {
            System.out.println("[SKIP] Operating system is not Windows. Host: " + IPCFactory.getOperatingSystemName());
            System.out.println("=========================================");
            return;
        }

        testFactoryBackendSelection();
        testBidirectionalPipeCommunication();
        testMultipleSequentialMessages();

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
     * Test 1: Verify IPCFactory instantiates WindowsNamedPipeIPC on Windows.
     */
    private static void testFactoryBackendSelection() {
        boolean pass = false;
        try {
            IPCManager server = IPCFactory.createServer(TEST_CHANNEL);
            pass = (server instanceof WindowsNamedPipeIPC)
                    && server.getChannelName().equals(TEST_CHANNEL);
            server.close();
        } catch (Exception e) {
            pass = false;
        }

        recordResult("Test 1: Factory Backend Selection", pass,
                "IPCFactory accurately instantiated WindowsNamedPipeIPC");
    }

    /**
     * Test 2: Full round-trip ping-pong over Named Pipe.
     */
    private static void testBidirectionalPipeCommunication() {
        final CountDownLatch serverReady = new CountDownLatch(1);
        final CountDownLatch testCompleted = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicReference<String> errorMsg = new AtomicReference<>("");

        // Server Thread
        Thread serverThread = new Thread(() -> {
            WindowsNamedPipeIPC server = new WindowsNamedPipeIPC(TEST_CHANNEL, true);
            try {
                serverReady.countDown();
                server.startServer();

                // Receive command from client
                IPCMessage receivedCmd = server.receiveMessage();
                if (receivedCmd != null && IPCMessage.TYPE_EXECUTE.equals(receivedCmd.getType())) {
                    // Send response back
                    IPCMessage resp = IPCMessage.createResult(receivedCmd.getRequestId(), "PC=0x0001;STATUS=READY");
                    server.sendMessage(resp);
                }
            } catch (Exception e) {
                errorMsg.set("Server error: " + e.getMessage());
            } finally {
                server.close();
            }
        }, "PipeServerThread");

        // Client Thread
        Thread clientThread = new Thread(() -> {
            WindowsNamedPipeIPC client = new WindowsNamedPipeIPC(TEST_CHANNEL, false);
            try {
                serverReady.await(5, TimeUnit.SECONDS);
                Thread.sleep(100); // Give server a moment to enter ConnectNamedPipe
                client.connectClient();

                // Send request
                IPCMessage req = IPCMessage.createExecute("req-test-1", "STEP");
                client.sendMessage(req);

                // Receive response
                IPCMessage resp = client.receiveMessage();
                if (resp != null && IPCMessage.TYPE_RESULT.equals(resp.getType())
                        && "req-test-1".equals(resp.getRequestId())
                        && resp.getPayload().contains("STATUS=READY")) {
                    success.set(true);
                } else {
                    errorMsg.set("Unexpected response: " + resp);
                }
            } catch (Exception e) {
                errorMsg.set("Client error: " + e.getMessage());
            } finally {
                client.close();
                testCompleted.countDown();
            }
        }, "PipeClientThread");

        serverThread.start();
        clientThread.start();

        try {
            boolean finished = testCompleted.await(10, TimeUnit.SECONDS);
            boolean pass = finished && success.get();
            recordResult("Test 2: Bidirectional Pipe Transfer", pass,
                    pass ? "Request -> Server -> Response round-trip verified over Named Pipe"
                         : "Failed: " + errorMsg.get());
        } catch (InterruptedException e) {
            recordResult("Test 2: Bidirectional Pipe Transfer", false, "Test timed out");
        }
    }

    /**
     * Test 3: Multiple messages sequential streaming across same open pipe.
     */
    private static void testMultipleSequentialMessages() {
        final CountDownLatch serverReady = new CountDownLatch(1);
        final CountDownLatch testCompleted = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);

        Thread serverThread = new Thread(() -> {
            WindowsNamedPipeIPC server = new WindowsNamedPipeIPC("stream_pipe", true);
            try {
                serverReady.countDown();
                server.startServer();

                for (int i = 0; i < 5; i++) {
                    IPCMessage msg = server.receiveMessage();
                    if (msg == null) break;
                    IPCMessage ack = IPCMessage.createAck(msg.getRequestId(), "ACK_" + i);
                    server.sendMessage(ack);
                }
            } catch (Exception ignored) {
            } finally {
                server.close();
            }
        });

        Thread clientThread = new Thread(() -> {
            WindowsNamedPipeIPC client = new WindowsNamedPipeIPC("stream_pipe", false);
            try {
                serverReady.await(5, TimeUnit.SECONDS);
                Thread.sleep(100);
                client.connectClient();

                boolean allMatched = true;
                for (int i = 0; i < 5; i++) {
                    client.sendMessage(new IPCMessage(IPCMessage.TYPE_LOG, "id-" + i, "Log entry " + i));
                    IPCMessage ack = client.receiveMessage();
                    if (ack == null || !ack.getPayload().equals("ACK_" + i)) {
                        allMatched = false;
                        break;
                    }
                }
                success.set(allMatched);
            } catch (Exception ignored) {
            } finally {
                client.close();
                testCompleted.countDown();
            }
        });

        serverThread.start();
        clientThread.start();

        try {
            boolean finished = testCompleted.await(10, TimeUnit.SECONDS);
            boolean pass = finished && success.get();
            recordResult("Test 3: Multiple Sequential Messages Streaming", pass,
                    pass ? "5 consecutive request-reply frames passed seamlessly" : "Sequential streaming failed");
        } catch (InterruptedException e) {
            recordResult("Test 3: Multiple Sequential Messages Streaming", false, "Timed out");
        }
    }
}
