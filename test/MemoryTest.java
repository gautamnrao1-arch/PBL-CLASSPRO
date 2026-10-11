/**
 * MemoryTest.java
 *
 * Standalone test suite for verifying Memory.java in the PIC16F72 simulator.
 * Tests:
 * 1. Writing a value to a memory address.
 * 2. Reading the same value.
 * 3. Verifying read == written value.
 * 4. Testing multiple addresses.
 * 5. Testing memory reset functionality.
 * 6. Handling invalid / out-of-bounds addresses safely.
 *
 * No external testing frameworks used; executable directly via main().
 */
public class MemoryTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("       PIC16F72 Memory Test Suite        ");
        System.out.println("=========================================");

        testWriteSingleAddress();
        testReadSingleAddress();
        testVerifyReadEqualsWritten();
        testMultipleAddresses();
        testReset();
        testInvalidAddressHandling();

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
     * Test 1: Write a value to a memory address.
     */
    private static void testWriteSingleAddress() {
        Memory memory = new Memory();
        int address = 0x20;
        int value = 0x55;

        memory.write(address, value);
        int readBack = memory.read(address);

        boolean pass = (readBack == value);
        recordResult("Test 1: Write Value to Address", pass,
                String.format("Wrote 0x%02X to 0x%02X, read back 0x%02X", value, address, readBack));
    }

    /**
     * Test 2: Read the value from a memory address.
     */
    private static void testReadSingleAddress() {
        Memory memory = new Memory();
        int address = 0x15;
        int initialValue = memory.read(address);

        boolean passInitial = (initialValue == 0);

        memory.write(address, 0xAB);
        int readValue = memory.read(address);
        boolean passRead = (readValue == 0xAB);

        recordResult("Test 2: Read Value from Address", passInitial && passRead,
                String.format("Initial read: 0x%02X, after write: 0x%02X", initialValue, readValue));
    }

    /**
     * Test 3: Verify read == written value.
     */
    private static void testVerifyReadEqualsWritten() {
        Memory memory = new Memory();
        int address = 0x30;
        int written = 0x7E;

        memory.write(address, written);
        int readBack = memory.read(address);

        boolean pass = (readBack == written);
        recordResult("Test 3: Verify read == written value", pass,
                String.format("Written = 0x%02X, Read = 0x%02X, Equals = %b", written, readBack, pass));
    }

    /**
     * Test 4: Test multiple addresses across data memory.
     */
    private static void testMultipleAddresses() {
        Memory memory = new Memory();
        int[] testAddresses = {0x00, 0x05, 0x1F, 0x20, 0x50, 0x80, 0xFA, 0xFF};
        int[] testValues    = {0x01, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE};

        boolean allMatched = true;
        for (int i = 0; i < testAddresses.length; i++) {
            memory.write(testAddresses[i], testValues[i]);
        }

        for (int i = 0; i < testAddresses.length; i++) {
            int read = memory.read(testAddresses[i]);
            if (read != testValues[i]) {
                allMatched = false;
                break;
            }
        }

        recordResult("Test 4: Multiple Addresses Storage", allMatched,
                String.format("Verified %d separate memory addresses", testAddresses.length));
    }

    /**
     * Test 5: Test reset clears all data memory.
     */
    private static void testReset() {
        Memory memory = new Memory();
        memory.write(0x10, 0xAA);
        memory.write(0x20, 0xBB);
        memory.write(0x30, 0xCC);

        memory.reset();

        boolean allZero = true;
        for (int addr = 0; addr < Memory.DATA_MEMORY_SIZE; addr++) {
            if (memory.read(addr) != 0) {
                allZero = false;
                break;
            }
        }

        recordResult("Test 5: Memory Reset", allZero,
                "All data memory locations verified zero after reset()");
    }

    /**
     * Test 6: Test invalid address handling (no crashes, returns 0).
     */
    private static void testInvalidAddressHandling() {
        Memory memory = new Memory();
        boolean safe = true;

        try {
            // Negative addresses
            int valNeg = memory.read(-1);
            int valNegLarge = memory.read(-100);
            memory.write(-1, 0x55);
            memory.write(-50, 0x99);

            // Out-of-bounds addresses
            int valOOB1 = memory.read(Memory.DATA_MEMORY_SIZE);
            int valOOB2 = memory.read(9999);
            memory.write(Memory.DATA_MEMORY_SIZE, 0x77);
            memory.write(9999, 0x88);

            // Validations
            safe = (valNeg == 0) && (valNegLarge == 0) && (valOOB1 == 0) && (valOOB2 == 0);
        } catch (Exception ex) {
            safe = false;
        }

        recordResult("Test 6: Invalid Address Handling", safe,
                "Negative and out-of-bounds addresses handled safely without crash");
    }
}
