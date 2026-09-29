/**
 * FIFOQueueTest.java
 *
 * Standalone test suite for verifying FIFOQueue.java in the PIC16F72 simulator.
 * Tests:
 * 1. Empty queue initialization.
 * 2. ENQUEUE one value.
 * 3. ENQUEUE multiple values.
 * 4. DEQUEUE one value.
 * 5. Verify FIFO (First-In, First-Out) ordering.
 * 6. Verify the first enqueued value is the first dequeued.
 * 7. Empty condition detection and null returns.
 * 8. Full condition and overflow prevention.
 * 9. Queue reset functionality.
 * 10. Front/Rear index pointer and circular wrapping behavior.
 *
 * No external testing libraries used; executable directly via main().
 */
public class FIFOQueueTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("       PIC16F72 FIFOQueue Test Suite     ");
        System.out.println("=========================================");

        testEmptyQueue();
        testEnqueueOneValue();
        testEnqueueMultipleValues();
        testDequeueOneValue();
        testVerifyFIFOOrdering();
        testFirstInFirstOutExplicit();
        testEmptyCondition();
        testFullCondition();
        testReset();
        testFrontRearCircularBehavior();

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
     * Test 1: Empty queue initial properties.
     */
    private static void testEmptyQueue() {
        FIFOQueue q = new FIFOQueue();
        boolean pass = q.isEmpty()
                && !q.isFull()
                && q.getSize() == 0
                && q.getFront() == 0
                && q.getRear() == 0
                && q.dequeue() == null
                && "EMPTY".equals(q.getStatus());

        recordResult("Test 1: Empty Queue Initialization", pass,
                String.format("isEmpty=%b, isFull=%b, size=%d, dequeue=%s, status=%s",
                        q.isEmpty(), q.isFull(), q.getSize(), q.dequeue(), q.getStatus()));
    }

    /**
     * Test 2: ENQUEUE a single value.
     */
    private static void testEnqueueOneValue() {
        FIFOQueue q = new FIFOQueue();
        boolean enqueued = q.enqueue(42);

        boolean pass = enqueued
                && !q.isEmpty()
                && q.getSize() == 1
                && q.getRear() == 1
                && "NORMAL".equals(q.getStatus());

        recordResult("Test 2: ENQUEUE One Value", pass,
                String.format("enqueued=%b, size=%d, front=%d, rear=%d, status=%s",
                        enqueued, q.getSize(), q.getFront(), q.getRear(), q.getStatus()));
    }

    /**
     * Test 3: ENQUEUE multiple values.
     */
    private static void testEnqueueMultipleValues() {
        FIFOQueue q = new FIFOQueue();
        int[] values = {10, 20, 30, 40};

        boolean allEnqueued = true;
        for (int v : values) {
            if (!q.enqueue(v)) {
                allEnqueued = false;
            }
        }

        boolean pass = allEnqueued
                && q.getSize() == values.length
                && q.getRear() == values.length
                && !q.isEmpty();

        recordResult("Test 3: ENQUEUE Multiple Values", pass,
                String.format("Enqueued %d values successfully, size=%d, rear=%d",
                        values.length, q.getSize(), q.getRear()));
    }

    /**
     * Test 4: DEQUEUE one value.
     */
    private static void testDequeueOneValue() {
        FIFOQueue q = new FIFOQueue();
        q.enqueue(100);
        q.enqueue(200);

        Integer dequeued = q.dequeue();
        boolean pass = (dequeued != null && dequeued == 100)
                && q.getSize() == 1
                && q.getFront() == 1;

        recordResult("Test 4: DEQUEUE One Value", pass,
                String.format("Dequeued value=%d, remaining size=%d, front=%d",
                        dequeued, q.getSize(), q.getFront()));
    }

    /**
     * Test 5: Verify strict FIFO (First-In, First-Out) ordering.
     */
    private static void testVerifyFIFOOrdering() {
        FIFOQueue q = new FIFOQueue();
        int[] input = {11, 22, 33, 44, 55};

        for (int v : input) {
            q.enqueue(v);
        }

        boolean fifoOrderMatches = true;
        for (int expected : input) {
            Integer actual = q.dequeue();
            if (actual == null || actual != expected) {
                fifoOrderMatches = false;
                break;
            }
        }

        recordResult("Test 5: Verify FIFO Ordering", fifoOrderMatches,
                "Elements dequeued in exact arrival order: 11 -> 22 -> 33 -> 44 -> 55");
    }

    /**
     * Test 6: Verify the first enqueued value is always the first dequeued.
     */
    private static void testFirstInFirstOutExplicit() {
        FIFOQueue q = new FIFOQueue();
        q.enqueue(777); // First enqueued
        q.enqueue(888);
        q.enqueue(999);

        Integer firstOut = q.dequeue();
        boolean pass = (firstOut != null && firstOut == 777);

        recordResult("Test 6: First In Is First Out", pass,
                String.format("First enqueued (777) == First dequeued (%s)", firstOut));
    }

    /**
     * Test 7: Empty condition verification.
     */
    private static void testEmptyCondition() {
        FIFOQueue q = new FIFOQueue();
        q.enqueue(5);
        q.dequeue();

        boolean pass = q.isEmpty()
                && q.getSize() == 0
                && q.dequeue() == null
                && "EMPTY".equals(q.getStatus());

        recordResult("Test 7: Empty Condition Detection", pass,
                String.format("isEmpty=%b, size=%d, subsequent dequeue()=%s, status=%s",
                        q.isEmpty(), q.getSize(), q.dequeue(), q.getStatus()));
    }

    /**
     * Test 8: Full condition and overflow prevention.
     */
    private static void testFullCondition() {
        FIFOQueue q = new FIFOQueue();
        int max = q.getMaxSize();

        for (int i = 0; i < max; i++) {
            q.enqueue(i * 10);
        }

        boolean fullBefore = q.isFull() && "FULL".equals(q.getStatus());
        boolean overflowPrevented = !q.enqueue(999);
        boolean sizePreserved = (q.getSize() == max);

        boolean pass = fullBefore && overflowPrevented && sizePreserved;
        recordResult("Test 8: Full Condition & Overflow Protection", pass,
                String.format("isFull=%b, overflow rejected=%b, size preserved=%d",
                        fullBefore, overflowPrevented, q.getSize()));
    }

    /**
     * Test 9: Queue reset restores initial front, rear, and size.
     */
    private static void testReset() {
        FIFOQueue q = new FIFOQueue();
        q.enqueue(1);
        q.enqueue(2);
        q.enqueue(3);
        q.dequeue();

        q.reset();

        boolean pass = q.isEmpty()
                && !q.isFull()
                && q.getSize() == 0
                && q.getFront() == 0
                && q.getRear() == 0
                && q.dequeue() == null
                && "EMPTY".equals(q.getStatus());

        recordResult("Test 9: Queue Reset", pass,
                "Queue reset successfully: front=0, rear=0, size=0, isEmpty=true");
    }

    /**
     * Test 10: Front/Rear pointer and circular array wrapping behavior.
     */
    private static void testFrontRearCircularBehavior() {
        FIFOQueue q = new FIFOQueue(); // default capacity = 8

        // Fill 8 elements: front=0, rear wraps to 0, size=8
        for (int i = 0; i < 8; i++) {
            q.enqueue(100 + i);
        }

        // Dequeue 3 elements: front advances to 3, size=5
        Integer d0 = q.dequeue(); // 100
        Integer d1 = q.dequeue(); // 101
        Integer d2 = q.dequeue(); // 102

        boolean dequeueCheck = (d0 == 100 && d1 == 101 && d2 == 102) && (q.getFront() == 3);

        // Enqueue 3 elements: rear advances from 0 to 3 (wrapping around)
        boolean e1 = q.enqueue(200);
        boolean e2 = q.enqueue(201);
        boolean e3 = q.enqueue(202);

        boolean wrapCheck = e1 && e2 && e3 && (q.getRear() == 3) && q.isFull() && (q.getSize() == 8);

        // Dequeue all and verify continuous FIFO order across the wrap boundary
        int[] expectedAfterWrap = {103, 104, 105, 106, 107, 200, 201, 202};
        boolean allMatched = true;
        for (int expected : expectedAfterWrap) {
            Integer actual = q.dequeue();
            if (actual == null || actual != expected) {
                allMatched = false;
                break;
            }
        }

        boolean pass = dequeueCheck && wrapCheck && allMatched && q.isEmpty();
        recordResult("Test 10: Front/Rear Circular Wrapping Behavior", pass,
                "Front and rear successfully wrapped around circular array with perfect FIFO sequence");
    }
}
