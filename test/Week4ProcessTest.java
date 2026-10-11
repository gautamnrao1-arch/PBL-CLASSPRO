import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Week4ProcessTest.java
 *
 * Comprehensive integration and unit test suite for Week 4 requirements:
 * 1. WindowsNamedPipeIPC: Genuine Windows Named Pipe IPC round-trip.
 * 2. CoreProcess: Command execution (LOAD, STEP, RUN, RESET, STATE) over IPC.
 * 3. LoggerProcess: Audit logging persistence without owning or mutating CPU state.
 * 4. State snapshot dictionary serialization round-trip with special characters.
 * 5. Multi-Process coordination and parameter validation.
 */
public class Week4ProcessTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       Week 4 Multi-Process IPC Test Suite       ");
        System.out.println("=================================================");

        testNamedPipeRoundTrip();
        testSnapshotDictionaryRoundTrip();
        testCoreAndLoggerIPCIntegration();
        testLoggerStateIsolation();

        System.out.println("=================================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL WEEK 4 TESTS PASSED");
        } else {
            System.out.println("Result: SOME TESTS FAILED");
        }
        System.out.println("=================================================");
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
     * Test 1: Verify WindowsNamedPipeIPC establishes genuine server/client connection
     * and performs bidirectional message exchange.
     */
    private static void testNamedPipeRoundTrip() {
        String channel = "test_roundtrip_" + System.currentTimeMillis() % 10000;
        final boolean[] success = {false};

        try (IPCManager server = IPCFactory.createServer(channel)) {

            Thread clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(150);
                        try (IPCManager client = IPCFactory.createClient(channel)) {
                            client.connectClient();

                            // Client sends message
                            client.sendMessage(new IPCMessage(IPCMessage.TYPE_EXECUTE, "req-pipe-1", "STEP"));

                            // Client receives response
                            IPCMessage resp = client.receiveMessage();
                            if (resp != null && IPCMessage.TYPE_RESULT.equals(resp.getType()) && "STEP_OK".equals(resp.getPayload())) {
                                success[0] = true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            clientThread.start();
            server.startServer();

            IPCMessage received = server.receiveMessage();
            if (received != null && IPCMessage.TYPE_EXECUTE.equals(received.getType()) && "STEP".equals(received.getPayload())) {
                server.sendMessage(new IPCMessage(IPCMessage.TYPE_RESULT, received.getRequestId(), "STEP_OK"));
            }

            clientThread.join(5000);

            recordResult("Test 1: Genuine Pipe Round-Trip", success[0],
                    "Server and client successfully exchanged bidirectional framed messages over OS pipe");

        } catch (Exception ex) {
            recordResult("Test 1: Genuine Pipe Round-Trip", false, "Exception: " + ex.getMessage());
        }
    }

    /**
     * Test 2: Verify state dictionary serialization preserves formatting, newlines, and values.
     */
    private static void testSnapshotDictionaryRoundTrip() {
        Map<String, String> sample = new java.util.HashMap<String, String>();
        sample.put(IPCConstants.KEY_STATUS_STR, "EXECUTING");
        sample.put(IPCConstants.KEY_PC, "15");
        sample.put(IPCConstants.KEY_W, "255");
        sample.put(IPCConstants.KEY_FLAG_Z, "1");
        sample.put(IPCConstants.KEY_TRACE, "Line 1\nLine 2 with | pipes and ; comments\nLine 3");
        sample.put(IPCConstants.KEY_PROGRAM_VIEW, "--> 0: MOVLW 5\n    1: MOVWF 20\n");

        String encoded = CoreProcess.encodeDictionary(sample);
        Map<String, String> decoded = CoreProcess.decodeDictionary(encoded);

        boolean match = "EXECUTING".equals(decoded.get(IPCConstants.KEY_STATUS_STR))
                && "15".equals(decoded.get(IPCConstants.KEY_PC))
                && "255".equals(decoded.get(IPCConstants.KEY_W))
                && "1".equals(decoded.get(IPCConstants.KEY_FLAG_Z))
                && sample.get(IPCConstants.KEY_TRACE).equals(decoded.get(IPCConstants.KEY_TRACE))
                && sample.get(IPCConstants.KEY_PROGRAM_VIEW).equals(decoded.get(IPCConstants.KEY_PROGRAM_VIEW));

        recordResult("Test 2: State Snapshot Dictionary Integrity", match,
                "Keys, multiline traces, and assembly pointers encoded and decoded without loss");
    }

    /**
     * Test 3: Integration of Core and Logger over IPC.
     * Starts Logger and Core, sends LOAD, STEP, RUN, RESET commands, verifies execution and logs.
     */
    private static void testCoreAndLoggerIPCIntegration() {
        String testLogFile = "test_audit_" + System.currentTimeMillis() % 10000 + ".log";
        new File(testLogFile).delete();

        final LoggerProcess logger = new LoggerProcess(testLogFile);
        Thread loggerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                logger.run();
            }
        });
        loggerThread.start();

        final CoreProcess core = new CoreProcess();
        Thread coreThread = new Thread(new Runnable() {
            @Override
            public void run() {
                core.run();
            }
        });
        coreThread.start();

        boolean allStepsPassed = false;

        try {
            // Wait for Core to connect to Logger and bind UI server
            Thread.sleep(800);

            try (IPCManager uiClient = IPCFactory.createClient(IPCConstants.CHANNEL_UI_CORE)) {
                uiClient.connectClient();

                // 1. Send LOAD command
                String asm = "MOVLW 10\nMOVWF 20\nINCF 20\nSLEEP\n";
                uiClient.sendMessage(IPCMessage.createLoad(UUID.randomUUID().toString(), asm));
                IPCMessage loadResp = uiClient.receiveMessage();
                boolean loadOk = loadResp != null && IPCMessage.TYPE_RESULT.equals(loadResp.getType());

                // 2. Send STEP command (MOVLW 10)
                uiClient.sendMessage(IPCMessage.createExecute(UUID.randomUUID().toString(), IPCConstants.CMD_STEP));
                IPCMessage stepResp = uiClient.receiveMessage();
                Map<String, String> stepDict = CoreProcess.decodeDictionary(stepResp.getPayload());
                boolean stepOk = "10".equals(stepDict.get(IPCConstants.KEY_W)); // W should be 10

                // 3. Send STEP command (MOVWF 20)
                uiClient.sendMessage(IPCMessage.createExecute(UUID.randomUUID().toString(), IPCConstants.CMD_STEP));
                IPCMessage step2Resp = uiClient.receiveMessage();

                // 4. Send RUN command to complete
                uiClient.sendMessage(IPCMessage.createExecute(UUID.randomUUID().toString(), IPCConstants.CMD_RUN));
                IPCMessage runResp = uiClient.receiveMessage();
                Map<String, String> runDict = CoreProcess.decodeDictionary(runResp.getPayload());
                boolean runOk = "TERMINATED".equals(runDict.get(IPCConstants.KEY_STATUS_STR))
                        || "READY".equals(runDict.get(IPCConstants.KEY_STATUS_STR));

                // 5. Send RESET command
                uiClient.sendMessage(IPCMessage.createReset(UUID.randomUUID().toString()));
                IPCMessage resetResp = uiClient.receiveMessage();
                Map<String, String> resetDict = CoreProcess.decodeDictionary(resetResp.getPayload());
                boolean resetOk = "0".equals(resetDict.get(IPCConstants.KEY_PC))
                        && "0".equals(resetDict.get(IPCConstants.KEY_W));

                // 6. Send SHUTDOWN
                uiClient.sendMessage(IPCMessage.createShutdown("Test completed"));
                IPCMessage shutdownResp = uiClient.receiveMessage();
                boolean shutdownOk = shutdownResp != null;

                allStepsPassed = loadOk && stepOk && runOk && resetOk && shutdownOk;
            }

            coreThread.join(5000);
            logger.stop();
            loggerThread.join(5000);

            // Verify log file was written by LoggerProcess
            File logFile = new File(testLogFile);
            boolean logFileWritten = logFile.exists() && logFile.length() > 0;
            logFile.delete();

            allStepsPassed = allStepsPassed && logFileWritten;

            recordResult("Test 3: Core and Logger Multi-Channel Integration", allStepsPassed,
                    "LOAD, STEP (W=10 verified), RUN, RESET, SHUTDOWN executed via IPC; audit log written by Logger");

        } catch (Exception ex) {
            recordResult("Test 3: Core and Logger Multi-Channel Integration", false, "Exception: " + ex.getMessage());
            coreThread.interrupt();
            loggerThread.interrupt();
        }
    }

    /**
     * Test 4: Verify Logger Process does not own or modify CPU state.
     */
    private static void testLoggerStateIsolation() {
        boolean pass = true;
        try {
            Class<?> loggerClass = Class.forName("LoggerProcess");
            for (java.lang.reflect.Field field : loggerClass.getDeclaredFields()) {
                String fieldTypeName = field.getType().getSimpleName();
                if ("CPU".equals(fieldTypeName) || "Registers".equals(fieldTypeName) || "Memory".equals(fieldTypeName)) {
                    pass = false;
                    break;
                }
            }
            recordResult("Test 4: Logger Architecture Isolation", pass,
                    "LoggerProcess contains no CPU, Registers, or Memory state fields");
        } catch (Exception e) {
            recordResult("Test 4: Logger Architecture Isolation", false, e.getMessage());
        }
    }
}
