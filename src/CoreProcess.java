import java.util.ArrayList;

/**
 * CoreProcess.java
 *
 * Week 4 Core Process for the PIC16F72 simulator.
 *
 * Responsible for controlling:
 * - CPU execution
 * - Registers
 * - Memory
 *
 * Stack and FIFO Queue will be integrated into the Core
 * as their implementation is added.
 */
public class CoreProcess {

    private final Registers registers;
    private final Memory memory;
    private final CPU cpu;

    /**
     * Creates the Core Process and its CPU components.
     */
    public CoreProcess() {
        this.registers = new Registers();
        this.memory = new Memory();
        this.cpu = new CPU(registers, memory);
    }

    /**
     * Loads a program into the CPU.
     */
    public void loadProgram(ArrayList<Instruction> program) {
        cpu.loadProgram(program);
    }

    /**
     * Resets the complete CPU state.
     */
    public void reset() {
        cpu.reset();
    }

    /**
     * Executes one instruction.
     */
    public ExecutionResult step() {
        return cpu.step();
    }

    /**
     * Runs the loaded program until termination or error.
     */
    public void run() {
        cpu.run();
    }

    /**
     * Returns the CPU controlled by this Core Process.
     */
    public CPU getCPU() {
        return cpu;
    }

    /**
     * Returns the Registers subsystem.
     */
    public Registers getRegisters() {
        return registers;
    }

    /**
     * Returns the Memory subsystem.
     */
    public Memory getMemory() {
        return memory;
    }
}
