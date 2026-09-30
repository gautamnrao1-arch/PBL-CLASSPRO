/**
 * CoreIPCTest.java
 *
 * Tests the Core IPC interface.
 */
public class CoreIPCTest {

    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println("        WEEK 4 CORE IPC TEST");
        System.out.println("=================================");

        // Create Core
        CoreProcess core = new CoreProcess();

        // Create IPC interface
        CoreIPC ipc = new CoreIPC(core);

        // -----------------------------------------
        // TEST 1: PIPE NAME
        // -----------------------------------------

        System.out.println();
        System.out.println("TEST 1: PIPE NAME");

        System.out.println(
                "Pipe Name: " + ipc.getPipeName()
        );

        if ("tata_core_pipe".equals(ipc.getPipeName())) {

            System.out.println("PIPE NAME TEST: PASS");

        } else {

            System.out.println("PIPE NAME TEST: FAIL");
        }

        // -----------------------------------------
        // TEST 2: RESET
        // -----------------------------------------

        System.out.println();
        System.out.println("TEST 2: RESET");

        String resetResponse =
                ipc.processCommand("RESET");

        System.out.println(
                "Response: " + resetResponse
        );

        if (resetResponse != null
                && resetResponse.contains("RESET SUCCESS")) {

            System.out.println("RESET IPC TEST: PASS");

        } else {

            System.out.println("RESET IPC TEST: FAIL");
        }

        // -----------------------------------------
        // TEST 3: STATE
        // -----------------------------------------

        System.out.println();
        System.out.println("TEST 3: STATE");

        String stateResponse =
                ipc.processCommand("STATE");

        System.out.println(
                "Response:"
        );

        System.out.println(stateResponse);

        if (stateResponse != null
                && stateResponse.contains("CORE STATE")) {

            System.out.println("STATE IPC TEST: PASS");

        } else {

            System.out.println("STATE IPC TEST: FAIL");
        }

        // -----------------------------------------
        // TEST 4: INVALID COMMAND
        // -----------------------------------------

        System.out.println();
        System.out.println("TEST 4: INVALID COMMAND");

        String invalidResponse =
                ipc.processCommand("ABC");

        System.out.println(
                "Response: " + invalidResponse
        );

        if (invalidResponse != null
                && invalidResponse.contains("Unknown command")) {

            System.out.println(
                    "INVALID COMMAND IPC TEST: PASS"
            );

        } else {

            System.out.println(
                    "INVALID COMMAND IPC TEST: FAIL"
            );
        }

        // -----------------------------------------
        // FINISH
        // -----------------------------------------

        System.out.println();
        System.out.println("=================================");
        System.out.println("       CORE IPC TEST COMPLETE");
        System.out.println("=================================");
    }
}