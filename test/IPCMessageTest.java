import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * IPCMessageTest.java
 *
 * Standalone test suite for verifying IPCMessage serialization, deserialization,
 * stream framing, special character safety, and validation.
 *
 * No external testing frameworks used; runs directly with main().
 */
public class IPCMessageTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("        IPCMessage Test Suite            ");
        System.out.println("=========================================");

        testStandardCategoriesRoundTrip();
        testSpecialCharactersAndDelimiters();
        testMultilineAssemblyPayload();
        testStreamFramingRoundTrip();
        testInvalidEnvelopeRejection();
        testCorruptedBase64Rejection();
        testInvalidTypeRejection();

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
     * Test 1: Verify all required standard message categories serialize and deserialize accurately.
     */
    private static void testStandardCategoriesRoundTrip() {
        String[] types = {
            IPCMessage.TYPE_EXECUTE,
            IPCMessage.TYPE_RESET,
            IPCMessage.TYPE_RESULT,
            IPCMessage.TYPE_ERROR,
            IPCMessage.TYPE_LOG,
            IPCMessage.TYPE_SHUTDOWN
        };

        boolean allPassed = true;
        for (String type : types) {
            String payload = "Payload for " + type;
            IPCMessage original = new IPCMessage(type, "req-" + type.toLowerCase(), 123456789L, payload);
            String wire = original.serialize();
            IPCMessage restored = IPCMessage.deserialize(wire);

            boolean match = restored.getType().equals(type)
                    && restored.getRequestId().equals(original.getRequestId())
                    && restored.getTimestamp() == original.getTimestamp()
                    && restored.getPayload().equals(payload);

            if (!match) {
                allPassed = false;
                break;
            }
        }

        recordResult("Test 1: Standard Categories Round-Trip", allPassed,
                "EXECUTE, RESET, RESULT, ERROR, LOG, SHUTDOWN verified");
    }

    /**
     * Test 2: Verify delimiters, spaces, quotes, and special symbols do not corrupt envelope.
     */
    private static void testSpecialCharactersAndDelimiters() {
        String complexPayload = "DATA|WITH|PIPES & \"QUOTES\" & 'SINGLE' \t TABS; ;COMMENTS; #$@! 100% == != \r\n UTF8: \u2192 \u2713";
        IPCMessage original = new IPCMessage(IPCMessage.TYPE_LOG, "req-special-1", complexPayload);

        String wire = original.serialize();
        IPCMessage restored = IPCMessage.deserialize(wire);

        boolean pass = restored.getPayload().equals(complexPayload)
                && restored.getRequestId().equals("req-special-1")
                && restored.getType().equals(IPCMessage.TYPE_LOG);

        recordResult("Test 2: Special Characters & Delimiters", pass,
                "Pipes (|), quotes, symbols, and Unicode restored without corruption");
    }

    /**
     * Test 3: Verify multiline assembly code is preserved intact.
     */
    private static void testMultilineAssemblyPayload() {
        String asm = "; PIC16F72 Assembly\nMOVLW 0x25\nMOVWF 0x20\nINCF 0x20, 1\nSLEEP\n";
        IPCMessage original = IPCMessage.createLoad("req-asm", asm);

        String wire = original.serialize();
        IPCMessage restored = IPCMessage.deserialize(wire);

        boolean pass = restored.getType().equals(IPCMessage.TYPE_LOAD)
                && restored.getPayload().equals(asm);

        recordResult("Test 3: Multiline Assembly Preservation", pass,
                "Newlines, indentation, and comments preserved exactly");
    }

    /**
     * Test 4: Length-prefixed stream framing round-trip.
     */
    private static void testStreamFramingRoundTrip() {
        boolean pass = false;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPCMessage msg1 = new IPCMessage(IPCMessage.TYPE_EXECUTE, "r1", "STEP");
            IPCMessage msg2 = new IPCMessage(IPCMessage.TYPE_RESULT, "r1", "PC=0x0001; W=0x05");

            // Write 2 framed messages sequentially
            msg1.writeDelimitedTo(baos);
            msg2.writeDelimitedTo(baos);

            // Read them back from byte stream
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            IPCMessage read1 = IPCMessage.readDelimitedFrom(bais);
            IPCMessage read2 = IPCMessage.readDelimitedFrom(bais);
            IPCMessage readEof = IPCMessage.readDelimitedFrom(bais);

            pass = read1 != null && read1.getType().equals(IPCMessage.TYPE_EXECUTE) && read1.getPayload().equals("STEP")
                && read2 != null && read2.getType().equals(IPCMessage.TYPE_RESULT) && read2.getPayload().equals("PC=0x0001; W=0x05")
                && readEof == null; // clean EOF
        } catch (IOException ex) {
            pass = false;
        }

        recordResult("Test 4: Length-Prefixed Stream Framing", pass,
                "Multiple sequential stream frames read/written without fragmentation");
    }

    /**
     * Test 5: Rejection of malformed envelope strings.
     */
    private static void testInvalidEnvelopeRejection() {
        boolean pass = false;
        try {
            IPCMessage.deserialize("INCOMPLETE|FORMAT|ONLY");
        } catch (IllegalArgumentException ex) {
            pass = true;
        }

        recordResult("Test 5: Malformed Envelope Rejection", pass,
                "Incomplete field count properly caught and rejected");
    }

    /**
     * Test 6: Rejection of corrupted Base64 payload.
     */
    private static void testCorruptedBase64Rejection() {
        boolean pass = false;
        try {
            IPCMessage.deserialize("EXECUTE|req-1|12345|%%%INVALID_BASE64%%%");
        } catch (IllegalArgumentException ex) {
            pass = true;
        }

        recordResult("Test 6: Corrupted Base64 Rejection", pass,
                "Illegal Base64 characters properly detected and rejected");
    }

    /**
     * Test 7: Rejection of invalid message type.
     */
    private static void testInvalidTypeRejection() {
        boolean pass = false;
        try {
            new IPCMessage("INVALID TYPE WITH SPACES", "payload");
        } catch (IllegalArgumentException ex) {
            pass = true;
        }

        recordResult("Test 7: Invalid Message Type Rejection", pass,
                "Non-alphanumeric message types safely rejected");
    }
}
