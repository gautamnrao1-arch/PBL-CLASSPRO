import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * ProcessManagerTest.java
 *
 * Verifies that ProcessManager spawns genuinely independent operating system processes
 * (distinct OS PIDs) for Logger and Core, and that inter-process communication occurs
 * cleanly across process boundaries.
 */
public class ProcessManagerTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("         ProcessManager Real OS Process Test     ");
        System.out.println("=================================================");

        ProcessManager pm = new ProcessManager();
        boolean pass = false;

        try {
            // 1. Spawn Logger OS Process
            System.out.println("[TEST] Launching Logger as independent OS process...");
            Process loggerProc = pm.launchRole(IPCConstants.ROLE_LOGGER);
            long loggerPid = loggerProc.pid();
            System.out.println("[TEST] Logger OS PID: " + loggerPid);
            Thread.sleep(800);

            // 2. Spawn Core as independent OS process
            System.out.println("[TEST] Launching Core as independent OS process...");
            Process coreProc = pm.launchRole(IPCConstants.ROLE_CORE);
            long corePid = coreProc.pid();
            System.out.println("[TEST] Core OS PID: " + corePid);
            Thread.sleep(800);

            // Verify both processes are alive and have distinct PIDs
            boolean distinctPids = loggerPid != corePid && loggerProc.isAlive() && coreProc.isAlive();
            System.out.println("[TEST] Distinct and alive OS processes: " + distinctPids);

            // 3. Connect as UI client to the spawned Core OS process
            System.out.println("[TEST] Connecting to Core OS process over IPC pipe...");
            try (IPCManager ui = IPCFactory.createClient(IPCConstants.CHANNEL_UI_CORE)) {
                ui.connectClient();

                // Send LOAD to remote Core OS process
                ui.sendMessage(IPCMessage.createLoad(UUID.randomUUID().toString(), "MOVLW 42\nMOVWF 20\nSLEEP\n"));
                IPCMessage loadResp = ui.receiveMessage();
                System.out.println("[TEST] Received from Core: " + loadResp);

                // Send STEP to remote Core OS process
                ui.sendMessage(IPCMessage.createExecute(UUID.randomUUID().toString(), IPCConstants.CMD_STEP));
                IPCMessage stepResp = ui.receiveMessage();
                Map<String, String> stepMap = CoreProcess.decodeDictionary(stepResp.getPayload());
                String wVal = stepMap.get(IPCConstants.KEY_W);
                System.out.println("[TEST] W register after STEP from remote Core: " + wVal);

                // Send SHUTDOWN to remote Core OS process
                ui.sendMessage(IPCMessage.createShutdown("Test shutdown"));
                IPCMessage ack = ui.receiveMessage();
                System.out.println("[TEST] Shutdown ACK: " + ack);

                pass = distinctPids && "42".equals(wVal) && loadResp != null;
            }

            // Wait for clean exit
            Thread.sleep(500);
            pm.shutdownAll();

        } catch (Exception e) {
            e.printStackTrace();
            pass = false;
            pm.shutdownAll();
        }

        System.out.println("=================================================");
        if (pass) {
            System.out.println("[PASS] ProcessManager OS Process Test -> PASSED");
        } else {
            System.out.println("[FAIL] ProcessManager OS Process Test -> FAILED");
        }
        System.out.println("=================================================");
    }
}
