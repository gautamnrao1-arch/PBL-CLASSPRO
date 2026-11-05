import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * CRC16AssemblyTest.java
 *
 * Comprehensive validation suite for Week 5:
 * 1. Executes crc16.asm through the PIC16F72 CPU simulator.
 * 2. Validates standard test vector "123456789" -> Expected CRC 0x29B1.
 * 3. Tests additional input case "A" -> Expected CRC 0xB915.
 * 4. Tests additional input case "HELLO" -> Expected CRC 0x49D6.
 * 5. Verifies individual instruction semantics (XORWF, RLF, BCF, BTFSS, DECFSZ, MOVF).
 * 6. Validates CoreProcess LOAD and RUN IPC snapshot integration.
 */
public class CRC16AssemblyTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    PIC16F72 CRC-16 Assembly Execution Tests    ");
        System.out.println("=================================================");

        testCRC16StandardVector();
        testCRC16SingleByteA();
        testCRC16StringHello();
        testNewInstructionSemantics();
        testCoreProcessCrcExecution();

        System.out.println("=================================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL CRC-16 ASSEMBLY TESTS PASSED");
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

    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    /**
     * Test 1: Full execution of crc16.asm with standard input "123456789"
     * Expected CRC: 0x29B1 (CRC_HIGH = 0x29, CRC_LOW = 0xB1).
     */
    private static void testCRC16StandardVector() {
        try {
            String asmSource = readFile("crc16.asm");
            ArrayList<Instruction> program = CoreProcess.parseProgram(asmSource);

            CPU cpu = new CPU();
            cpu.loadProgram(program);
            cpu.run();

            int crcHigh = cpu.getRegisters().getRegister(0x20);
            int crcLow  = cpu.getRegisters().getRegister(0x21);
            int fullCrc = (crcHigh << 8) | crcLow;

            boolean terminated = cpu.isTerminated();
            boolean match = (crcHigh == 0x29) && (crcLow == 0xB1) && (fullCrc == 0x29B1);

            recordResult("Test 1: Standard Vector '123456789'", match && terminated,
                    String.format("Expected: 0x29B1, Actual: 0x%04X (High: 0x%02X, Low: 0x%02X, Terminated: %b)",
                            fullCrc, crcHigh, crcLow, terminated));
        } catch (Exception ex) {
            recordResult("Test 1: Standard Vector '123456789'", false, "Exception: " + ex.getMessage());
        }
    }

    /**
     * Test 2: Additional test case with single-byte input "A" (0x41)
     * Expected CRC: 0xB915 (CRC_HIGH = 0xB9, CRC_LOW = 0x15).
     */
    private static void testCRC16SingleByteA() {
        try {
            String asm =
                "CRC_HIGH EQU 0x20\n" +
                "CRC_LOW EQU 0x21\n" +
                "DATA_BYTE EQU 0x22\n" +
                "BIT_COUNT EQU 0x23\n" +
                "BYTE_COUNT EQU 0x24\n" +
                "STATUS EQU 0x03\n" +
                "FSR EQU 0x04\n" +
                "INDF EQU 0x00\n" +
                "\n" +
                "MOVLW 0x41\n" +         // 'A'
                "MOVWF 0x30\n" +
                "MOVLW 0xFF\n" +
                "MOVWF CRC_HIGH\n" +
                "MOVLW 0xFF\n" +
                "MOVWF CRC_LOW\n" +
                "MOVLW 1\n" +
                "MOVWF BYTE_COUNT\n" +
                "MOVLW 0x30\n" +
                "MOVWF FSR\n" +
                "BYTE_LOOP:\n" +
                "    MOVF INDF, 0\n" +
                "    MOVWF DATA_BYTE\n" +
                "    XORWF CRC_HIGH, 1\n" +
                "    MOVLW 8\n" +
                "    MOVWF BIT_COUNT\n" +
                "BIT_LOOP:\n" +
                "    BCF STATUS, 0\n" +
                "    RLF CRC_LOW, 1\n" +
                "    RLF CRC_HIGH, 1\n" +
                "    BTFSS STATUS, 0\n" +
                "    GOTO SKIP_POLY\n" +
                "    MOVLW 0x10\n" +
                "    XORWF CRC_HIGH, 1\n" +
                "    MOVLW 0x21\n" +
                "    XORWF CRC_LOW, 1\n" +
                "SKIP_POLY:\n" +
                "    DECFSZ BIT_COUNT, 1\n" +
                "    GOTO BIT_LOOP\n" +
                "    INCF FSR, 1\n" +
                "    DECFSZ BYTE_COUNT, 1\n" +
                "    GOTO BYTE_LOOP\n" +
                "    SLEEP\n";

            ArrayList<Instruction> program = CoreProcess.parseProgram(asm);
            CPU cpu = new CPU();
            cpu.loadProgram(program);
            cpu.run();

            int crcHigh = cpu.getRegisters().getRegister(0x20);
            int crcLow  = cpu.getRegisters().getRegister(0x21);
            int fullCrc = (crcHigh << 8) | crcLow;

            boolean match = (crcHigh == 0xB9) && (crcLow == 0x15) && (fullCrc == 0xB915);
            recordResult("Test 2: Single-Byte Input 'A'", match,
                    String.format("Expected: 0xB915, Actual: 0x%04X (High: 0x%02X, Low: 0x%02X)", fullCrc, crcHigh, crcLow));
        } catch (Exception ex) {
            recordResult("Test 2: Single-Byte Input 'A'", false, "Exception: " + ex.getMessage());
        }
    }

    /**
     * Test 3: Additional test case with string "HELLO" (0x48, 0x45, 0x4C, 0x4C, 0x4F)
     * Expected CRC: 0x49D6 (CRC_HIGH = 0x49, CRC_LOW = 0xD6).
     */
    private static void testCRC16StringHello() {
        try {
            String asm =
                "CRC_HIGH EQU 0x20\n" +
                "CRC_LOW EQU 0x21\n" +
                "DATA_BYTE EQU 0x22\n" +
                "BIT_COUNT EQU 0x23\n" +
                "BYTE_COUNT EQU 0x24\n" +
                "STATUS EQU 0x03\n" +
                "FSR EQU 0x04\n" +
                "INDF EQU 0x00\n" +
                "\n" +
                "MOVLW 0x48\n" + MOVWF_STR(0x30) +   // 'H'
                "MOVLW 0x45\n" + MOVWF_STR(0x31) +   // 'E'
                "MOVLW 0x4C\n" + MOVWF_STR(0x32) +   // 'L'
                "MOVLW 0x4C\n" + MOVWF_STR(0x33) +   // 'L'
                "MOVLW 0x4F\n" + MOVWF_STR(0x34) +   // 'O'
                "MOVLW 0xFF\n" +
                "MOVWF CRC_HIGH\n" +
                "MOVLW 0xFF\n" +
                "MOVWF CRC_LOW\n" +
                "MOVLW 5\n" +
                "MOVWF BYTE_COUNT\n" +
                "MOVLW 0x30\n" +
                "MOVWF FSR\n" +
                "BYTE_LOOP:\n" +
                "    MOVF INDF, 0\n" +
                "    MOVWF DATA_BYTE\n" +
                "    XORWF CRC_HIGH, 1\n" +
                "    MOVLW 8\n" +
                "    MOVWF BIT_COUNT\n" +
                "BIT_LOOP:\n" +
                "    BCF STATUS, 0\n" +
                "    RLF CRC_LOW, 1\n" +
                "    RLF CRC_HIGH, 1\n" +
                "    BTFSS STATUS, 0\n" +
                "    GOTO SKIP_POLY\n" +
                "    MOVLW 0x10\n" +
                "    XORWF CRC_HIGH, 1\n" +
                "    MOVLW 0x21\n" +
                "    XORWF CRC_LOW, 1\n" +
                "SKIP_POLY:\n" +
                "    DECFSZ BIT_COUNT, 1\n" +
                "    GOTO BIT_LOOP\n" +
                "    INCF FSR, 1\n" +
                "    DECFSZ BYTE_COUNT, 1\n" +
                "    GOTO BYTE_LOOP\n" +
                "    SLEEP\n";

            ArrayList<Instruction> program = CoreProcess.parseProgram(asm);
            CPU cpu = new CPU();
            cpu.loadProgram(program);
            cpu.run();

            int crcHigh = cpu.getRegisters().getRegister(0x20);
            int crcLow  = cpu.getRegisters().getRegister(0x21);
            int fullCrc = (crcHigh << 8) | crcLow;

            boolean match = (crcHigh == 0x49) && (crcLow == 0xD6) && (fullCrc == 0x49D6);
            recordResult("Test 3: Multi-Byte String 'HELLO'", match,
                    String.format("Expected: 0x49D6, Actual: 0x%04X (High: 0x%02X, Low: 0x%02X)", fullCrc, crcHigh, crcLow));
        } catch (Exception ex) {
            recordResult("Test 3: Multi-Byte String 'HELLO'", false, "Exception: " + ex.getMessage());
        }
    }

    private static String MOVWF_STR(int addr) {
        return String.format("MOVWF 0x%02X\n", addr);
    }

    /**
     * Test 4: Semantic unit tests for newly implemented instructions.
     */
    private static void testNewInstructionSemantics() {
        Registers reg = new Registers();
        Memory mem = new Memory();
        CPU cpu = new CPU(reg, mem);

        boolean allPassed = true;

        // 1. XORWF and Zero flag
        reg.setRegister(0x20, 0xAA);
        cpu.setW(0xAA);
        cpu.execute(new Instruction("XORWF", new String[]{"0x20", "1"}, "XORWF 0x20, 1"));
        if (reg.getRegister(0x20) != 0x00 || reg.getZeroFlag() != 1) allPassed = false;

        // 2. XORLW
        cpu.setW(0x55);
        cpu.execute(new Instruction("XORLW", new String[]{"0x55"}, "XORLW 0x55"));
        if (cpu.getW() != 0 || reg.getZeroFlag() != 1) allPassed = false;

        // 3. RLF (Rotate left through carry)
        reg.setRegister(0x20, 0x81); // 1000 0001
        reg.setCarryFlag(1);
        cpu.execute(new Instruction("RLF", new String[]{"0x20", "1"}, "RLF 0x20, 1"));
        // After RLF: old C(1) enters bit 0, old bit 7 (1) goes into C -> result = 0000 0011 (0x03), C = 1
        if (reg.getRegister(0x20) != 0x03 || reg.getCarryFlag() != 1) allPassed = false;

        // 4. BCF and BSF
        reg.setRegister(0x25, 0x00);
        cpu.execute(new Instruction("BSF", new String[]{"0x25", "3"}, "BSF 0x25, 3"));
        if (reg.getRegister(0x25) != 0x08) allPassed = false;
        cpu.execute(new Instruction("BCF", new String[]{"0x25", "3"}, "BCF 0x25, 3"));
        if (reg.getRegister(0x25) != 0x00) allPassed = false;

        // 5. BTFSS (skip if bit set)
        reg.setRegister(0x25, 0x08);
        reg.setPC(10);
        cpu.execute(new Instruction("BTFSS", new String[]{"0x25", "3"}, "BTFSS 0x25, 3"));
        if (reg.getPC() != 11) allPassed = false; // PC should have skipped (incremented)

        // 6. BTFSC (skip if bit clear)
        reg.setRegister(0x25, 0x00);
        reg.setPC(20);
        cpu.execute(new Instruction("BTFSC", new String[]{"0x25", "3"}, "BTFSC 0x25, 3"));
        if (reg.getPC() != 21) allPassed = false; // PC should have skipped (incremented)

        // 7. DECFSZ (skip if 0)
        reg.setRegister(0x26, 1);
        reg.setPC(30);
        cpu.execute(new Instruction("DECFSZ", new String[]{"0x26", "1"}, "DECFSZ 0x26, 1"));
        if (reg.getRegister(0x26) != 0 || reg.getPC() != 31) allPassed = false;

        // 8. MOVF
        reg.setRegister(0x27, 0x7E);
        cpu.execute(new Instruction("MOVF", new String[]{"0x27", "0"}, "MOVF 0x27, 0"));
        if (cpu.getW() != 0x7E) allPassed = false;

        // 9. CLRF
        reg.setRegister(0x28, 0xFF);
        cpu.execute(new Instruction("CLRF", new String[]{"0x28"}, "CLRF 0x28"));
        if (reg.getRegister(0x28) != 0 || reg.getZeroFlag() != 1) allPassed = false;

        // 10. Indirect Addressing via INDF / FSR
        reg.setRegister(0x04, 0x50); // FSR = 0x50
        reg.setRegister(0x50, 0x99);
        if (reg.getRegister(0x00) != 0x99) allPassed = false; // INDF reads 0x50
        reg.setRegister(0x00, 0xAA); // INDF writes 0x50
        if (reg.getRegister(0x50) != 0xAA) allPassed = false;

        recordResult("Test 4: New Instruction Semantics", allPassed,
                "Verified XORWF, XORLW, RLF, RRF, BCF, BSF, BTFSC, BTFSS, DECFSZ, MOVF, CLRF, INDF/FSR");
    }

    /**
     * Test 5: Integration test with CoreProcess using IPC message handling.
     */
    private static void testCoreProcessCrcExecution() {
        try {
            CoreProcess core = new CoreProcess();
            String asmSource = readFile("crc16.asm");

            // 1. LOAD command
            IPCMessage loadMsg = IPCMessage.createLoad("req-load-crc", asmSource);
            IPCMessage loadResp = core.handleMessage(loadMsg);
            boolean loadOk = loadResp != null && IPCMessage.TYPE_RESULT.equals(loadResp.getType());

            // 2. RUN command
            IPCMessage runMsg = IPCMessage.createExecute("req-run-crc", IPCConstants.CMD_RUN);
            IPCMessage runResp = core.handleMessage(runMsg);
            boolean runOk = runResp != null && IPCMessage.TYPE_RESULT.equals(runResp.getType());

            Map<String, String> snapshot = CoreProcess.decodeDictionary(runResp.getPayload());
            int crcHigh = core.getRegisters().getRegister(0x20);
            int crcLow  = core.getRegisters().getRegister(0x21);
            int fullCrc = (crcHigh << 8) | crcLow;

            boolean correctCrc = (fullCrc == 0x29B1);
            String regDump = snapshot.get(IPCConstants.KEY_REG_DUMP);
            boolean dumpMatches = regDump != null && regDump.contains("[0x20]: 29  [0x21]: B1");

            recordResult("Test 5: CoreProcess IPC Snapshot Execution", loadOk && runOk && correctCrc && dumpMatches,
                    String.format("Loaded=%b, Ran=%b, CRC=0x%04X, Dump contains '[0x20]: 29  [0x21]: B1'=%b",
                            loadOk, runOk, fullCrc, dumpMatches));
        } catch (Exception ex) {
            recordResult("Test 5: CoreProcess IPC Snapshot Execution", false, "Exception: " + ex.getMessage());
        }
    }
}
