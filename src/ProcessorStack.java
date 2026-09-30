import java.util.Arrays;

/**
 * ProcessorStack.java
 *
 * Stack used by the PIC16F72 Core Process.
 */
public class ProcessorStack {

    private static final int STACK_SIZE = 8;

    private final int[] stack;
    private int sp;

    public ProcessorStack() {
        stack = new int[STACK_SIZE];
        reset();
    }

    // PUSH operation
    public boolean push(int value) {
        if (isFull()) {
            return false;
        }

        sp++;
        stack[sp] = value & 0x1FFF;
        return true;
    }

    // POP operation
    public int pop() {
        if (isEmpty()) {
            return -1;
        }

        int value = stack[sp];
        stack[sp] = 0;
        sp--;

        return value;
    }

    // View top element
    public int peek() {
        if (isEmpty()) {
            return -1;
        }

        return stack[sp];
    }

    // Stack Pointer
    public int getSP() {
        return sp;
    }

    public boolean isEmpty() {
        return sp == -1;
    }

    public boolean isFull() {
        return sp == STACK_SIZE - 1;
    }

    public void reset() {
        Arrays.fill(stack, 0);
        sp = -1;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();

        sb.append("Stack Pointer (SP): ").append(sp).append("\n");
        sb.append("Stack: ");

        if (isEmpty()) {
            sb.append("EMPTY");
        } else {
            for (int i = sp; i >= 0; i--) {
                sb.append(String.format("0x%04X", stack[i]));

                if (i > 0) {
                    sb.append(" <- ");
                }
            }
        }

        return sb.toString();
    }
}