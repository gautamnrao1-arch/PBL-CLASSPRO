import java.util.ArrayList;
import java.util.Arrays;

/**
 * Memory.java
 * This class represents the simulated program and data memory for the PIC16F72 simulator.
 */
public class Memory {

    //Size of data memory (8-bit registers, 256 bytes)
    public static final int DATA_MEMORY_SIZE = 256;

    // Simulated program memory storing Instruction objects
    private final ArrayList<Instruction> programMemory;

    // Simulated data memory storing 8-bit values
    private final int[] dataMemory;

    /**
     * Constructs a new Memory instance.
     * Initializes empty program memory and zeroed data memory.
     */
    public Memory() {
        this.programMemory = new ArrayList<Instruction>();
        this.dataMemory = new int[DATA_MEMORY_SIZE];
        reset();
    }

    /**
     * Load a list of decoded/represented instructions into program memory.
     * Replaces any existing loaded program and stores instructions starting from address 0.
     */
    public void loadProgram(ArrayList<Instruction> program) {
        this.programMemory.clear();
        if (program != null) {
            this.programMemory.addAll(program);
        }
    }

    /**
     * Return the instruction stored at the specified program-memory address.
     */
    public Instruction getInstruction(int address) {
        if (address >= 0 && address < this.programMemory.size()) {
            return this.programMemory.get(address);
        }
        return null;
    }

    /**
     * Validate whether an address is within the valid data-memory range (0 to 255).
     *
     * @param address Data memory address to validate
     * @return true if the address is within valid data memory range, false otherwise
     */
    public boolean isValidAddress(int address) {
        return address >= 0 && address < this.dataMemory.length;
    }

    /**
     * Read an 8-bit value from simulated data memory at the specified address.
     * Safely checks address validity to prevent crashes on out-of-bounds addresses.
     *
     * @param address Data memory address
     * @return 8-bit value stored at address, or 0 if address is invalid
     */
    public int read(int address) {
        if (isValidAddress(address)) {
            return this.dataMemory[address] & 0xFF;
        }
        return 0;
    }

    /**
     * Write an 8-bit value into simulated data memory at the specified address.
     * Validates the address and masks value to 8 bits.
     * Safely ignores writes to invalid addresses without crashing.
     *
     * @param address Data memory address
     * @param value 8-bit value to write
     */
    public void write(int address, int value) {
        if (isValidAddress(address)) {
            this.dataMemory[address] = value & 0xFF;
        }
    }

    /**
     * Clear and reset both program memory and data memory.
     */
    public void reset() {
        this.programMemory.clear();
        Arrays.fill(this.dataMemory, 0);
    }

    /**
     * Return the number of loaded program instructions.
     */
    public int getProgramSize() {
        return this.programMemory.size();
    }

    /**
     * Return a readable representation of important memory values for the UI.
     * This will be displayed by SimulatorUI.java.
     */
    public String getMemoryState() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Program Memory: %d instruction(s) loaded%n", this.programMemory.size()));
        sb.append("Data Memory (0x00 - 0x0F):").append(System.lineSeparator());
        for (int i = 0; i < 16 && i < this.dataMemory.length; i++) {
            sb.append(String.format("[%02X: %02X] ", i, read(i)));
            if ((i + 1) % 8 == 0) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString().trim();
    }

    // Return the currently loaded program instruction list
     
    public ArrayList<Instruction> getProgram() {
        return this.programMemory;
    }
}
