/**
 * CoreCommandTest.java
 *
 * Tests the command handling of the Week 4 Core Process.
 */
public class CoreCommandTest {

    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println("      WEEK 4 CORE COMMAND TEST");
        System.out.println("=================================");

        CoreProcess core = new CoreProcess();

        CoreCommandHandler handler =
                new CoreCommandHandler(core);

        // =====================================================
        // TEST 1 - STATE
        // =====================================================

        System.out.println();
        System.out.println("TEST 1: STATE");

        String stateResult =
                handler.handleCommand("STATE");

        System.out.println(stateResult);

        if (stateResult.contains("CORE STATE")) {
            System.out.println("STATE TEST: PASS");
        } else {
            System.out.println("STATE TEST: FAIL");
        }

        // =====================================================
        // TEST 2 - RESET
        // =====================================================

        System.out.println();
        System.out.println("TEST 2: RESET");

        String resetResult =
                handler.handleCommand("RESET");

        System.out.println(resetResult);

        if ("RESET SUCCESS".equals(resetResult)) {
            System.out.println("RESET TEST: PASS");
        } else {
            System.out.println("RESET TEST: FAIL");
        }

        // =====================================================
        // TEST 3 - UNKNOWN COMMAND
        // =====================================================

        System.out.println();
        System.out.println("TEST 3: INVALID COMMAND");

        String invalidResult =
                handler.handleCommand("ABC");

        System.out.println(invalidResult);

        if (invalidResult.startsWith("ERROR:")) {
            System.out.println("INVALID COMMAND TEST: PASS");
        } else {
            System.out.println("INVALID COMMAND TEST: FAIL");
        }

        // =====================================================
        // TEST 4 - STACK
        // =====================================================

        System.out.println();
        System.out.println("TEST 4: STACK");

        core.pushToStack(10);
        core.pushToStack(20);

        int popped =
                core.popFromStack();

        System.out.println("Popped value: " + popped);

        if (popped == 20) {
            System.out.println("STACK TEST: PASS");
        } else {
            System.out.println("STACK TEST: FAIL");
        }

        // =====================================================
        // TEST 5 - FIFO QUEUE
        // =====================================================

        System.out.println();
        System.out.println("TEST 5: FIFO QUEUE");

        core.enqueue(10);
        core.enqueue(20);

        int dequeued =
                core.dequeue();

        System.out.println("Dequeued value: " + dequeued);

        if (dequeued == 10) {
            System.out.println("FIFO QUEUE TEST: PASS");
        } else {
            System.out.println("FIFO QUEUE TEST: FAIL");
        }

        // =====================================================
        // TEST COMPLETE
        // =====================================================

        System.out.println();
        System.out.println("=================================");
        System.out.println("       CORE COMMAND TEST DONE");
        System.out.println("=================================");
    }
}