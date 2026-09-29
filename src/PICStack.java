import java.util.Arrays;

/**
 * PICStack.java
 *
 * Simulates the hardware/software return-address stack for the PIC16F72 microcontroller.
 *
 * Features:
 * - Fixed-size integer array implementation (suitable for PIC16F72 8-level stack)
 * - Stack Pointer (SP) tracking current depth
 * - PUSH and POP operations with bounds and overflow/underflow protection
 * - Empty / Full condition checks
 * - Stack contents retrieval
 * - State reset
 * - UI/CLI stack state formatting
 */
public class PICStack {

    // Default stack capacity for PIC16F72 (8-level deep hardware stack)
    public static final int DEFAULT_STACK_SIZE = 8;

    // Internal storage array for the stack
    private final int[] stack;

    // Current stack pointer (number of elements currently stored)
    private int stackPointer;

    // Maximum capacity of the stack
    private final int maxSize;

    /**
     * Default constructor for PICStack.
     * Initializes the stack with a fixed size of 8 levels suitable for PIC16F72.
     */
    public PICStack() {
        this(DEFAULT_STACK_SIZE);
    }

    /**
     * Parameterized constructor for PICStack.
     *
     * @param maxSize Maximum capacity of the stack
     */
    public PICStack(int maxSize) {
        this.maxSize = (maxSize > 0) ? maxSize : DEFAULT_STACK_SIZE;
        this.stack = new int[this.maxSize];
        reset();
    }

    /**
     * Clear all stack values and reset SP to the initial state.
     */
    public void reset() {
        this.stackPointer = 0;
        Arrays.fill(this.stack, 0);
    }

    /**
     * Push a value onto the stack.
     * 1. Check whether the stack is full.
     * 2. If full, do not insert and return false.
     * 3. Otherwise place the value on the stack.
     * 4. Update SP.
     * 5. Return true.
     *
     * @param value Value (such as return address) to push onto the stack
     * @return true if pushed successfully, false if stack is full
     */
    public boolean push(int value) {
        if (isFull()) {
            return false;
        }
        this.stack[this.stackPointer] = value;
        this.stackPointer++;
        return true;
    }

    /**
     * Pop the top value from the stack.
     * 1. Check whether stack is empty.
     * 2. If empty, return null.
     * 3. Otherwise remove the top value.
     * 4. Update SP.
     * 5. Return the popped value.
     *
     * @return Popped integer value, or null if stack is empty
     */
    public Integer pop() {
        if (isEmpty()) {
            return null;
        }
        this.stackPointer--;
        int value = this.stack[this.stackPointer];
        this.stack[this.stackPointer] = 0;
        return value;
    }

    /**
     * Check whether the stack is empty.
     *
     * @return true when no values are stored, false otherwise
     */
    public boolean isEmpty() {
        return this.stackPointer == 0;
    }

    /**
     * Check whether the stack is full.
     *
     * @return true when maximum stack capacity is reached, false otherwise
     */
    public boolean isFull() {
        return this.stackPointer >= this.maxSize;
    }

    /**
     * Get the current Stack Pointer (SP).
     *
     * @return Current stack pointer value
     */
    public int getStackPointer() {
        return this.stackPointer;
    }

    /**
     * Get the number of elements currently stored in the stack.
     *
     * @return Number of elements currently in the stack
     */
    public int getSize() {
        return this.stackPointer;
    }

    /**
     * Get the maximum capacity of the stack.
     *
     * @return Maximum stack capacity
     */
    public int getMaxSize() {
        return this.maxSize;
    }

    /**
     * Return a copy of the elements currently stored in the stack.
     * Elements are ordered from bottom (index 0) to top (index size - 1).
     *
     * @return Array containing currently stored stack values
     */
    public int[] getStackContents() {
        return Arrays.copyOf(this.stack, this.stackPointer);
    }

    /**
     * Return the current status of the stack.
     *
     * @return "EMPTY" if empty, "FULL" if full, "NORMAL" otherwise
     */
    public String getStatus() {
        if (isEmpty()) {
            return "EMPTY";
        } else if (isFull()) {
            return "FULL";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Return a formatted representation of the stack state suitable for the UI.
     * Displays current SP, capacity, status, and each level with top indicator.
     *
     * @return Human-readable stack state string for UI/CLI visualization
     */
    public String getStackState() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SP: %d/%d [%s]%n", this.stackPointer, this.maxSize, getStatus()));
        for (int i = this.maxSize - 1; i >= 0; i--) {
            String marker = (i == this.stackPointer - 1 && this.stackPointer > 0) ? " <-- TOP" : "";
            if (i >= this.stackPointer) {
                sb.append(String.format("Level %d: [Empty]%s%n", i, marker));
            } else {
                sb.append(String.format("Level %d: 0x%04X (%d)%s%n", i, this.stack[i], this.stack[i], marker));
            }
        }
        return sb.toString().trim();
    }
}
