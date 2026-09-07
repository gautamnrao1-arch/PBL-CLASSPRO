import java.util.ArrayList;
import java.util.Arrays;

/**
 * InstructionSet.java
 *
 * Responsible for identifying and describing the supported PIC16F72 instructions
 * for the Week 2 simulator.
 *
 * NOTE: This class is solely an information repository and catalog.
 * It does NOT execute instructions (CPU.java handles execution).
 *
 * Supported Instructions (8 total):
 * 1. MOVLW - Data Transfer
 * 2. MOVWF - Data Transfer
 * 3. ADDWF - Arithmetic
 * 4. SUBWF - Arithmetic
 * 5. ANDWF - Logical
 * 6. INCF  - Increment/Decrement
 * 7. GOTO  - Control Flow
 * 8. SLEEP - Program Termination
 */
public class InstructionSet {

    // List of supported canonical opcodes
    private static final ArrayList<String> SUPPORTED_OPCODES = new ArrayList<String>(
        Arrays.asList("MOVLW", "MOVWF", "ADDWF", "SUBWF", "ANDWF", "INCF", "GOTO", "SLEEP")
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
                return "Data Transfer";
            case "ADDWF":
            case "SUBWF":
                return "Arithmetic";
            case "ANDWF":
                return "Logical";
            case "INCF":
                return "Increment/Decrement";
            case "GOTO":
                return "Control Flow";
            case "SLEEP":
                return "Program Termination";
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
            case "ADDWF":
                return "Add W register and file register.";
            case "SUBWF":
                return "Subtract W register from the file register according to the selected implementation semantics.";
            case "ANDWF":
                return "Logical AND between W and file register.";
            case "INCF":
                return "Increment a file register.";
            case "GOTO":
                return "Change program counter to target address.";
            case "SLEEP":
                return "Terminate simulator execution.";
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
                return 0;
            case "MOVLW":
            case "MOVWF":
            case "GOTO":
                return 1;
            case "ADDWF":
            case "SUBWF":
            case "ANDWF":
            case "INCF":
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
