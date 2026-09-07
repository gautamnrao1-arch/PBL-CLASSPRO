import java.util.Arrays;

/**
 * Instruction.java
 *
 * This class represents a single instruction in the PIC16F72 simulator.
 * It encapsulates:
 * 1. The instruction opcode / mnemonic (e.g., MOVLW, ADDWF, BCF).
 * 2. An array of operand strings (e.g., register address, literal value, destination bit).
 * 3. The raw, unparsed instruction text as read from source.
 * 4. The assigned program-memory address of the instruction.
 *
 * DESIGN RESTRICTIONS:
 * - This class does NOT execute instructions.
 * - This class does NOT perform CPU fetch, decode, or execute cycles.
 * - This class is a clean, reusable data container.
 */
public class Instruction {

    // Opcode / mnemonic normalized for case-insensitive matching
    private String opcode;

    // Array of operands associated with this instruction
    private String[] operands;

    // Original raw instruction text
    private String rawInstruction;

    // Program memory address where this instruction resides
    private int address;

    /**
     * Constructs a new Instruction.
     *
     * @param opcode The instruction mnemonic (e.g., "MOVLW", "ADDWF")
     * @param operands Array of string operands (e.g., ["0x20", "0"])
     * @param rawInstruction The original unparsed line/string from the source program
     */
    public Instruction(String opcode, String[] operands, String rawInstruction) {
        this.opcode = (opcode != null) ? opcode.trim().toUpperCase() : "";
        this.operands = (operands != null) ? operands.clone() : new String[0];
        this.rawInstruction = (rawInstruction != null) ? rawInstruction : "";
        this.address = 0;
    }

    /**
     * Return the opcode/instruction name.
     * Normalized to uppercase for consistent case-insensitive comparisons.
     *
     * @return Uppercase opcode string
     */
    public String getOpcode() {
        return this.opcode;
    }

    /**
     * Return a copy of the instruction operands.
     *
     * @return Array of operand strings
     */
    public String[] getOperands() {
        return this.operands != null ? this.operands.clone() : new String[0];
    }

    /**
     * Return the original, unmodified instruction text.
     * Preserved for displaying the program and execution traces.
     *
     * @return Raw instruction string
     */
    public String getRawInstruction() {
        return this.rawInstruction;
    }

    /**
     * Return a single operand by its index.
     * Handles invalid or out-of-bounds indexes safely by returning null.
     *
     * @param index Zero-based operand index
     * @return Operand string at index, or null if index is invalid
     */
    public String getOperand(int index) {
        if (this.operands != null && index >= 0 && index < this.operands.length) {
            return this.operands[index];
        }
        return null;
    }

    /**
     * Return a human-readable representation of this instruction.
     * Displays the address alongside the raw instruction or formatted opcode and operands.
     *
     * @return Formatted string representing the instruction
     */
    @Override
    public String toString() {
        if (this.rawInstruction != null && !this.rawInstruction.trim().isEmpty()) {
            return String.format("[%04X] %s", this.address, this.rawInstruction.trim());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%04X] %s", this.address, this.opcode));
        if (this.operands != null && this.operands.length > 0) {
            sb.append(" ").append(String.join(", ", this.operands));
        }
        return sb.toString();
    }

    /**
     * Set the program-memory address of this instruction.
     *
     * @param address Program-memory address
     */
    public void setAddress(int address) {
        this.address = address;
    }

    /**
     * Return the program-memory address of this instruction.
     *
     * @return Program-memory address
     */
    public int getAddress() {
        return this.address;
    }
}
