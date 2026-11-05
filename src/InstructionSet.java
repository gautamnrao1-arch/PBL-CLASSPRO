import java.util.ArrayList;
import java.util.Arrays;

/**
 * InstructionSet.java
 *
 * Responsible for identifying and describing the supported PIC16F72 instructions
 * for the simulator.
 *
 * NOTE: This class is solely an information repository and catalog.
 * It does NOT execute instructions (CPU.java handles execution).
 */
public class InstructionSet {

    // List of supported canonical opcodes
    private static final ArrayList<String> SUPPORTED_OPCODES = new ArrayList<String>(
        Arrays.asList(
            "MOVLW", "MOVWF", "ADDWF", "SUBWF", "ANDWF", "INCF", "GOTO", "SLEEP",
            "PUSH", "POP", "ENQUEUE", "DEQUEUE",
            "XORWF", "XORLW", "RLF", "RRF", "BCF", "BSF", "BTFSC", "BTFSS", "DECFSZ", "MOVF", "CLRF"
        )
    );

    /**
     * Helper to normalize an opcode to uppercase and trimmed format.
     *
     * @param opcode The raw opcode string
     * @return Uppercase trimmed string, or empty string if null
     */
    private static String normalize(String opcode) {
        return (opcode != null) ? opcode.trim().toUpperCase() : "";
    }

    /**
     * Check whether an opcode is supported by the PIC16F72 simulator.
     * Matching is case-insensitive.
     *
     * @param opcode The opcode to validate (e.g., "movlw", "MOVLW")
     * @return True if supported, false otherwise
     */
    public static boolean isValidOpcode(String opcode) {
        String canonical = normalize(opcode);
        return SUPPORTED_OPCODES.contains(canonical);
    }

    /**
     * Return the category of the instruction.
     * Matching is case-insensitive.
     *
     * @param opcode The instruction opcode
     * @return Category name, or "Unknown" if opcode is not recognized
     */
    public static String getCategory(String opcode) {
        String canonical = normalize(opcode);
        switch (canonical) {
            case "MOVLW":
            case "MOVWF":
            case "MOVF":
            case "CLRF":
                return "Data Transfer";
            case "ADDWF":
            case "SUBWF":
                return "Arithmetic";
            case "ANDWF":
            case "XORWF":
            case "XORLW":
                return "Logical";
            case "INCF":
            case "DECFSZ":
                return "Increment/Decrement";
            case "RLF":
            case "RRF":
                return "Shift/Rotate";
            case "BCF":
            case "BSF":
            case "BTFSC":
            case "BTFSS":
                return "Bit-oriented";
            case "GOTO":
                return "Control Flow";
            case "SLEEP":
                return "Program Termination";
            case "PUSH":
            case "POP":
                return "Stack";
            case "ENQUEUE":
            case "DEQUEUE":
                return "FIFO Queue";
            default:
                return "Unknown";
        }
    }

    /**
     * Return a clear description of the instruction's operation.
     * Matching is case-insensitive.
     *
     * @param opcode The instruction opcode
     * @return Description text, or "Unknown instruction" if not recognized
     */
    public static String getDescription(String opcode) {
        String canonical = normalize(opcode);
        switch (canonical) {
            case "MOVLW":
                return "Load literal value into W register.";
            case "MOVWF":
                return "Move W register value into a file/data register.";
            case "MOVF":
                return "Move file register into destination (W or f).";
            case "CLRF":
                return "Clear file register to 0.";
            case "ADDWF":
                return "Add W register and file register.";
            case "SUBWF":
                return "Subtract W register from the file register according to the selected implementation semantics.";
            case "ANDWF":
                return "Logical AND between W and file register.";
            case "XORWF":
                return "Exclusive OR W with file register.";
            case "XORLW":
                return "Exclusive OR literal with W.";
            case "INCF":
                return "Increment a file register.";
            case "DECFSZ":
                return "Decrement file register, skip next instruction if zero.";
            case "RLF":
                return "Rotate file register left through Carry flag.";
            case "RRF":
                return "Rotate file register right through Carry flag.";
            case "BCF":
                return "Bit clear in file register.";
            case "BSF":
                return "Bit set in file register.";
            case "BTFSC":
                return "Bit test in file register, skip if clear (0).";
            case "BTFSS":
                return "Bit test in file register, skip if set (1).";
            case "GOTO":
                return "Change program counter to target address.";
            case "SLEEP":
                return "Terminate simulator execution.";
            case "PUSH":
                return "Push a value onto the simulator stack.";
            case "POP":
                return "Pop the top value from the simulator stack.";
            case "ENQUEUE":
                return "Insert a value at the rear of the FIFO queue.";
            case "DEQUEUE":
                return "Remove and return the value from the front of the FIFO queue.";
            default:
                return "Unknown instruction.";
        }
    }

    /**
     * Check whether the instruction requires operands.
     * Matching is case-insensitive.
     *
     * @param opcode The instruction opcode
     * @return True if the instruction accepts one or more operands, false otherwise
     */
    public static boolean hasOperands(String opcode) {
        return getOperandCount(opcode) > 0;
    }

    /**
     * Return the expected number of operands for the instruction.
     * Matching is case-insensitive.
     *
     * @param opcode The instruction opcode
     * @return Number of operands (0, 1, or 2), or 0 if unknown
     */
    public static int getOperandCount(String opcode) {
        String canonical = normalize(opcode);
        switch (canonical) {
            case "SLEEP":
            case "POP":
            case "DEQUEUE":
                return 0;
            case "MOVLW":
            case "MOVWF":
            case "GOTO":
            case "PUSH":
            case "ENQUEUE":
            case "XORLW":
            case "CLRF":
                return 1;
            case "ADDWF":
            case "SUBWF":
            case "ANDWF":
            case "INCF":
            case "XORWF":
            case "RLF":
            case "RRF":
            case "BCF":
            case "BSF":
            case "BTFSC":
            case "BTFSS":
            case "DECFSZ":
            case "MOVF":
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Return a copy of all supported opcodes in canonical uppercase form.
     *
     * @return ArrayList of supported opcode strings
     */
    public static ArrayList<String> getSupportedOpcodes() {
        return new ArrayList<String>(SUPPORTED_OPCODES);
    }
}
