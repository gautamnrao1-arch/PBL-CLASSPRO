import java.util.ArrayList;

/**
 * CoreProcess.java
 *
 * Week 4 Core Process.
 *
 * Controls:
 * CPU, Registers, Memory, Stack and FIFO Queue.
 */
public class CoreProcess {

    private final Registers registers;
    private final Memory memory;
    private final CPU cpu;

    private final ProcessorStack stack;
    private final FIFOQueue queue;

    public CoreProcess() {

        registers = new Registers();
        memory = new Memory();
        cpu = new CPU(registers, memory);

        stack = new ProcessorStack();
        queue = new FIFOQueue();
    }

    public void loadProgram(ArrayList<Instruction> program) {
        cpu.loadProgram(program);
    }

    public void reset() {
        cpu.reset();
        stack.reset();
        queue.reset();
    }

    public ExecutionResult step() {
        return cpu.step();
    }

    public void run() {
        cpu.run();
    }

    public CPU getCPU() {
        return cpu;
    }

    public Registers getRegisters() {
        return registers;
    }

    public Memory getMemory() {
        return memory;
    }

    public ProcessorStack getStack() {
        return stack;
    }

    public FIFOQueue getQueue() {
        return queue;
    }

    public boolean pushToStack(int value) {
        return stack.push(value);
    }

    public int popFromStack() {
        return stack.pop();
    }

    public boolean enqueue(int value) {
        return queue.enqueue(value);
    }

    public int dequeue() {
        return queue.dequeue();
    }

    public String getCoreState() {

        StringBuilder state = new StringBuilder();

        state.append("=== CORE STATE ===\n");

        state.append(registers.getRegisterState())
             .append("\n\n");

        state.append("W Register: ")
             .append(String.format("0x%02X", cpu.getW()))
             .append("\n\n");

        state.append(memory.getMemoryState())
             .append("\n\n");

        state.append(stack.getState())
             .append("\n\n");

        state.append(queue.getState())
             .append("\n");

        return state.toString();
    }
}