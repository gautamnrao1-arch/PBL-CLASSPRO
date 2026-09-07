import java.util.Arrays;

/**
 * Registers.java
 * Stores 256 8-bit registers, 
 *Program Counter and STATUS flags.
 */
public class Registers {

    //Total number of registers
    public static final int TOTAL_REGISTERS = 256;

    //STATUS register addresses
    public static final int STATUS_ADDR = 0x03;         // STATUS register in Bank 0
    public static final int STATUS_ADDR_BANK1 = 0x83;   // Mirrored STATUS register in Bank 1

    //STATUS flag bits
    public static final int CARRY_FLAG_MASK = 0x01;     // Bit 0: Carry / Borrow (C)
    public static final int DIGIT_CARRY_MASK = 0x02;    // Bit 1: Digit Carry / Half Carry (DC)
    public static final int ZERO_FLAG_MASK = 0x04;       // Bit 2: Zero (Z)

    //Program Counter range
    public static final int PC_MASK = 0x1FFF;

    // Register storage of 8-bit registers
    private final int[] registers;

    // 13-bit Program Counter
    private int pc;

    // 8-bit STATUS register
    private int status;

    /**
     * Creates the register storage and resets the values.
     * Initializes all registers, PC, and flags to their default reset state.
     */
    public Registers() {
        this.registers = new int[TOTAL_REGISTERS];
        reset();
    }

    // Return the value stored at the specified register address.
     
    public int getRegister(int address) {
        if (address >= 0 && address < registers.length) {
            return registers[address] & 0xFF;
        }
        return 0;
    }

    /**
     * Store an 8-bit value into the specified register.
     * Keeps values constrained within the appropriate 8-bit range.
     * Automatically keeps the STATUS register and its bank mirror in sync if written to.
     */
    public void setRegister(int address, int value) {
        if (address >= 0 && address < registers.length) {
            int val8 = value & 0xFF;
            registers[address] = val8;

            // Synchronize STATUS if the STATUS address is directly written to
            if (address == STATUS_ADDR || address == STATUS_ADDR_BANK1) {
                this.status = val8;
                registers[STATUS_ADDR] = val8;
                registers[STATUS_ADDR_BANK1] = val8;
            }
        }
    }

    /**
     * Return the current Program Counter (PC).
     */
    public int getPC() {
        return this.pc;
    }

    /**
     * Set the Program Counter (PC).
     * Clamps the value to the PIC16F72 13-bit addressable range (0x0000 - 0x1FFF).
     */
    public void setPC(int pc) {
        this.pc = pc & PC_MASK;
    }

    /**
     * Increment the Program Counter by one instruction position.
     * Wraps around within the 13-bit program memory space.
     */
    public void incrementPC() {
        this.pc = (this.pc + 1) & PC_MASK;
    }

    /**
     * Return the STATUS register value.
     */
    public int getStatus() {
        return this.status & 0xFF;
    }

    /**
     * Set the STATUS register.
     * Keeps values within 8 bits and updates both the field and register memory locations.
     */
    public void setStatus(int status) {
        this.status = status & 0xFF;
        registers[STATUS_ADDR] = this.status;
        registers[STATUS_ADDR_BANK1] = this.status;
    }

    /**
     * Reset all registers, PC, and STATUS to their initial simulator state.
     * Clears all memory to 0 and resets PC to 0x0000.
     */
    public void reset() {
        Arrays.fill(registers, 0);
        this.pc = 0;
        this.status = 0;
        registers[STATUS_ADDR] = 0;
        registers[STATUS_ADDR_BANK1] = 0;
    }

    /**
     * Return the current Zero flag (Z) value.
     * Bit 2 of the STATUS register.
     */
    public int getZeroFlag() {
        return (this.status & ZERO_FLAG_MASK) != 0 ? 1 : 0;
    }

    /**
     * Set the Zero flag (Z).
     * Updates Bit 2 in the STATUS register.
     */
    public void setZeroFlag(int value) {
        if (value != 0) {
            setStatus(this.status | ZERO_FLAG_MASK);
        } else {
            setStatus(this.status & ~ZERO_FLAG_MASK);
        }
    }

    /**
     * Return the current Carry flag (C) value.
     * Bit 0 of the STATUS register.
     */
    public int getCarryFlag() {
        return (this.status & CARRY_FLAG_MASK) != 0 ? 1 : 0;
    }

    /**
     * Set the Carry flag (C).
     * Updates Bit 0 in the STATUS register.
     */
    public void setCarryFlag(int value) {
        if (value != 0) {
            setStatus(this.status | CARRY_FLAG_MASK);
        } else {
            setStatus(this.status & ~CARRY_FLAG_MASK);
        }
    }

    /**
     * Return the current Digit Carry flag (DC) value.
     * Bit 1 of the STATUS register.
     */
    public int getDigitCarryFlag() {
        return (this.status & DIGIT_CARRY_MASK) != 0 ? 1 : 0;
    }

    /**
     * Sets or clears the Zero flag (Z) in the STATUS register.
     */
    public void setDigitCarryFlag(int value) {
        if (value != 0) {
            setStatus(this.status | DIGIT_CARRY_MASK);
        } else {
            setStatus(this.status & ~DIGIT_CARRY_MASK);
        }
    }

    // Return a readable String representing the important register state.
    public String getRegisterState() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("PC: 0x%04X (%d)%n", pc, pc));
        sb.append(String.format("STATUS: 0x%02X [%s]%n", status, getFlagState()));
        sb.append(String.format("INDF: 0x%02X | TMR0: 0x%02X | PCL: 0x%02X | FSR: 0x%02X%n",
                getRegister(0x00), getRegister(0x01), getRegister(0x02), getRegister(0x04)));
        sb.append(String.format("PORTA: 0x%02X | PORTB: 0x%02X | PORTC: 0x%02X",
                getRegister(0x05), getRegister(0x06), getRegister(0x07)));
        return sb.toString();
    }

    //Return the current flag values
    public String getFlagState() {
        return String.format("Z=%d, C=%d, DC=%d", getZeroFlag(), getCarryFlag(), getDigitCarryFlag());
    }
}
