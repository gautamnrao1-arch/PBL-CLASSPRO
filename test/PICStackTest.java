/**
 * PICStackTest.java
 *
 * Standalone test suite for verifying PICStack.java in the PIC16F72 simulator.
 * Tests:
 * 1. Empty stack behavior.
 * 2. PUSH one value.
 * 3. PUSH multiple values.
 * 4. POP one value.
 * 5. Verify LIFO (Last-In, First-Out) ordering.
 * 6. Empty condition detection.
 * 7. Full condition and overflow prevention.
 * 8. Stack reset functionality.
 * 9. Stack Pointer (SP) synchronization and tracking.
 *
 * No external testing libraries used; executable directly via main().
 */
public class PICStackTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("       PIC16F72 PICStack Test Suite      ");
        System.out.println("=========================================");

        testEmptyStack();
        testPushOneValue();
        testPushMultipleValues();
        testPopOneValue();
        testVerifyLIFOOrdering();
        testEmptyCondition();
        testFullCondition();
        testReset();
        testStackPointerBehavior();

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
     * Test 1: Empty stack initial properties.
     */
    private static void testEmptyStack() {
        PICStack stack = new PICStack();
        boolean pass = stack.isEmpty()
                && !stack.isFull()
                && stack.getSize() == 0
                && stack.getStackPointer() == 0
                && stack.pop() == null
                && "EMPTY".equals(stack.getStatus());

        recordResult("Test 1: Empty Stack Initialization", pass,
                String.format("isEmpty=%b, size=%d, SP=%d, pop=%s, status=%s",
                        stack.isEmpty(), stack.getSize(), stack.getStackPointer(), stack.pop(), stack.getStatus()));
    }

    /**
     * Test 2: PUSH a single value onto the stack.
     */
    private static void testPushOneValue() {
        PICStack stack = new PICStack();
        int value = 0x0120;

        boolean pushed = stack.push(value);
        boolean pass = pushed
                && !stack.isEmpty()
                && stack.getSize() == 1
                && stack.getStackPointer() == 1
                && "NORMAL".equals(stack.getStatus());

        recordResult("Test 2: PUSH One Value", pass,
                String.format("pushed=%b, size=%d, SP=%d, status=%s",
                        pushed, stack.getSize(), stack.getStackPointer(), stack.getStatus()));
    }

    /**
     * Test 3: PUSH multiple values onto the stack.
     */
    private static void testPushMultipleValues() {
        PICStack stack = new PICStack();
        int[] values = {0x0010, 0x0020, 0x0030, 0x0040};

        boolean allPushed = true;
        for (int v : values) {
            if (!stack.push(v)) {
                allPushed = false;
            }
        }

        boolean pass = allPushed
                && stack.getSize() == values.length
                && stack.getStackPointer() == values.length
                && !stack.isEmpty();

        recordResult("Test 3: PUSH Multiple Values", pass,
                String.format("Pushed %d values successfully, size=%d, SP=%d",
                        values.length, stack.getSize(), stack.getStackPointer()));
    }

    /**
     * Test 4: POP a single value from the stack.
     */
    private static void testPopOneValue() {
        PICStack stack = new PICStack();
        stack.push(0x0500);
        stack.push(0x0600);

        Integer popped = stack.pop();
        boolean pass = (popped != null && popped == 0x0600)
                && stack.getSize() == 1
                && stack.getStackPointer() == 1;

        recordResult("Test 4: POP One Value", pass,
                String.format("Popped value=0x%04X, remaining size=%d, SP=%d",
                        popped, stack.getSize(), stack.getStackPointer()));
    }

    /**
     * Test 5: Verify strict LIFO (Last-In, First-Out) ordering.
     */
    private static void testVerifyLIFOOrdering() {
        PICStack stack = new PICStack();
        int[] input = {100, 200, 300, 400, 500};

        for (int v : input) {
            stack.push(v);
        }

        boolean lifoCorrect = true;
        for (int i = input.length - 1; i >= 0; i--) {
            Integer popped = stack.pop();
            if (popped == null || popped != input[i]) {
                lifoCorrect = false;
                break;
            }
        }

        recordResult("Test 5: Verify LIFO Ordering", lifoCorrect,
                "Popped items matched inverse push order: 500 -> 400 -> 300 -> 200 -> 100");
    }

    /**
     * Test 6: Empty condition verification.
     */
    private static void testEmptyCondition() {
        PICStack stack = new PICStack();
        stack.push(42);
        stack.pop();

        boolean pass = stack.isEmpty()
                && stack.getSize() == 0
                && stack.getStackPointer() == 0
                && stack.pop() == null
                && "EMPTY".equals(stack.getStatus());

        recordResult("Test 6: Empty Condition", pass,
                String.format("After push and pop: isEmpty=%b, pop()=%s", stack.isEmpty(), stack.pop()));
    }

    /**
     * Test 7: Full condition and overflow prevention.
     */
    private static void testFullCondition() {
        PICStack stack = new PICStack();
        int max = stack.getMaxSize();

        for (int i = 0; i < max; i++) {
            stack.push(0x1000 + i);
        }

        boolean fullBefore = stack.isFull() && "FULL".equals(stack.getStatus());
        boolean overflowBlocked = !stack.push(0x9999);
        boolean sizeIntact = (stack.getSize() == max) && (stack.getStackPointer() == max);

        boolean pass = fullBefore && overflowBlocked && sizeIntact;
        recordResult("Test 7: Full Condition & Overflow Protection", pass,
                String.format("isFull=%b, overflow push returned=%b, size preserved=%d",
                        fullBefore, !overflowBlocked, stack.getSize()));
    }

    /**
     * Test 8: Stack reset clears contents and pointers.
     */
    private static void testReset() {
        PICStack stack = new PICStack();
        stack.push(11);
        stack.push(22);
        stack.push(33);

        stack.reset();

        boolean pass = stack.isEmpty()
                && !stack.isFull()
                && stack.getSize() == 0
                && stack.getStackPointer() == 0
                && stack.pop() == null
                && "EMPTY".equals(stack.getStatus());

        recordResult("Test 8: Stack Reset", pass,
                "Stack completely cleared: SP=0, size=0, isEmpty=true");
    }

    /**
     * Test 9: Stack Pointer behavior throughout push and pop sequence.
     */
    private static void testStackPointerBehavior() {
        PICStack stack = new PICStack();
        boolean spTracks = true;

        if (stack.getStackPointer() != 0) spTracks = false;

        stack.push(10);
        if (stack.getStackPointer() != 1) spTracks = false;

        stack.push(20);
        if (stack.getStackPointer() != 2) spTracks = false;

        stack.pop();
        if (stack.getStackPointer() != 1) spTracks = false;

        stack.pop();
        if (stack.getStackPointer() != 0) spTracks = false;

        recordResult("Test 9: Stack Pointer (SP) Behavior", spTracks,
                "SP accurately tracked sequence: 0 -> 1 -> 2 -> 1 -> 0");
    }
}
