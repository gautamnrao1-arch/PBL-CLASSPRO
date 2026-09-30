import java.util.ArrayList;

/**
 * CoreProcess.java
 *
 * Week 4 Core Process for the PIC16F72 simulator.
 *
 * Responsible for:
 * - CPU execution
 * - Registers
 * - Memory
 * - Stack
 * - FIFO Queue
 * - Command handling
 */
public class CoreProcess {

    private final Registers registers;
    private final Memory memory;
    private final CPU cpu;

    private final ProcessorStack stack;
    private final FIFOQueue queue;

    private final CoreCommandHandler commandHandler;

    /**
     * Creates the Core Process and all its components.
     */
    public CoreProcess() {

        registers = new Registers();
        memory = new Memory();

        cpu = new CPU(registers, memory);

        stack = new ProcessorStack();
        queue = new FIFOQueue();

        commandHandler = new CoreCommandHandler(this);
    }

    // =========================================================
    // PROGRAM CONTROL
    // =========================================================

    /**
     * Load a program into the CPU.
     */
    public void loadProgram(ArrayList<Instruction> program) {
        cpu.loadProgram(program);
    }

    /**
     * Reset CPU, memory, stack and queue.
     */
    public void reset() {
        cpu.reset();
        stack.reset();
        queue.reset();
    }

    /**
     * Execute one instruction.
     */
    public ExecutionResult step() {
        return cpu.step();
    }

    /**
     * Run program until termination or error.
     */
    public void run() {
        cpu.run();
    }

    // =========================================================
    // CPU
    // =========================================================

    public CPU getCPU() {
        return cpu;
    }

    // =========================================================
    // REGISTERS
    // =========================================================

    public Registers getRegisters() {
        return registers;
    }

    // =========================================================
    // MEMORY
    // =========================================================

    public Memory getMemory() {
        return memory;
    }

    // =========================================================
    // STACK
    // =========================================================

    /**
     * Push a value onto the processor stack.
     */
    public boolean pushToStack(int value) {
        return stack.push(value);
    }

    /**
     * Pop a value from the processor stack.
     */
    public int popFromStack() {
        return stack.pop();
    }

    /**
     * Return the stack object.
     */
    public ProcessorStack getStack() {
        return stack;
    }

    // =========================================================
    // FIFO QUEUE
    // =========================================================

    /**
     * Add a value to the FIFO queue.
     */
    public boolean enqueue(int value) {
        return queue.enqueue(value);
    }

    /**
     * Remove a value from the FIFO queue.
     */
    public int dequeue() {
        return queue.dequeue();
    }

    /**
     * Return the FIFO queue object.
     */
    public FIFOQueue getQueue() {
        return queue;
    }

    // =========================================================
    // COMMAND HANDLING
    // =========================================================

    /**
     * Handle a command received by the Core.
     *
     * @param command command string
     * @return command result
     */
    public String handleCommand(String command) {
        return commandHandler.handleCommand(command);
    }

    // =========================================================
    // CORE STATE
    // =========================================================

    /**
     * Return the current state of the Core.
     */
    public String getCoreState() {

        StringBuilder state = new StringBuilder();

        state.append("=== CORE STATE ===")
             .append(System.lineSeparator());

        state.append(registers.getRegisterState())
             .append(System.lineSeparator())
             .append(System.lineSeparator());

        state.append("W Register: ")
             .append(String.format("0x%02X", cpu.getW()))
             .append(System.lineSeparator())
             .append(System.lineSeparator());

        state.append(memory.getMemoryState())
             .append(System.lineSeparator())
             .append(System.lineSeparator());

        state.append(stack.getState())
             .append(System.lineSeparator())
             .append(System.lineSeparator());

        state.append(queue.getState())
             .append(System.lineSeparator());

        return state.toString();
    }
}