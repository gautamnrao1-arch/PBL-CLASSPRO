import java.util.Arrays;

/**
 * FIFOQueue.java
 *
 * Simulates a First-In, First-Out (FIFO) circular queue for the PIC16F72 simulator.
 *
 * Features:
 * - Fixed-size circular array implementation (avoids java.util.Queue)
 * - Head (front) and tail (rear) pointer tracking
 * - Circular index wrapping for position reuse
 * - Enqueue and Dequeue operations with overflow and underflow handling
 * - Empty / Full status checks
 * - Queue contents extraction in FIFO order
 * - State reset
 * - UI/CLI friendly state formatting
 */
public class FIFOQueue {

    // Default queue capacity for the simulator
    public static final int DEFAULT_CAPACITY = 8;

    // Internal storage array
    private final int[] queue;

    // Index of the front element (next to dequeue)
    private int front;

    // Index of the next insertion slot (rear)
    private int rear;

    // Current number of elements stored in the queue
    private int size;

    // Maximum capacity of the queue
    private final int maxSize;

    /**
     * Default constructor.
     * Initializes the queue with default capacity suitable for the simulator.
     */
    public FIFOQueue() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Parameterized constructor.
     *
     * @param maxSize Maximum capacity of the circular queue
     */
    public FIFOQueue(int maxSize) {
        this.maxSize = (maxSize > 0) ? maxSize : DEFAULT_CAPACITY;
        this.queue = new int[this.maxSize];
        reset();
    }

    /**
     * Clear queue state and restore front, rear, and size to their initial values.
     */
    public void reset() {
        this.front = 0;
        this.rear = 0;
        this.size = 0;
        Arrays.fill(this.queue, 0);
    }

    /**
     * Insert an element at the rear of the queue.
     * 1. Check whether queue is full.
     * 2. If full, do not insert and return false.
     * 3. Otherwise insert at rear.
     * 4. Update rear and size.
     * 5. Return true.
     *
     * @param value Value to enqueue
     * @return true if enqueued successfully, false if queue is full
     */
    public boolean enqueue(int value) {
        if (isFull()) {
            return false;
        }
        this.queue[this.rear] = value;
        this.rear = (this.rear + 1) % this.maxSize;
        this.size++;
        return true;
    }

    /**
     * Remove and return the element at the front of the queue.
     * 1. Check whether queue is empty.
     * 2. If empty, return null.
     * 3. Remove the item at front.
     * 4. Update front and size.
     * 5. Return the removed value.
     *
     * @return The dequeued Integer value, or null if the queue is empty
     */
    public Integer dequeue() {
        if (isEmpty()) {
            return null;
        }
        int removedValue = this.queue[this.front];
        this.queue[this.front] = 0;
        this.front = (this.front + 1) % this.maxSize;
        this.size--;
        return removedValue;
    }

    /**
     * Check whether the queue is empty.
     *
     * @return true when size == 0, false otherwise
     */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * Check whether the queue is full.
     *
     * @return true when size == maxSize, false otherwise
     */
    public boolean isFull() {
        return this.size == this.maxSize;
    }

    /**
     * Return the current front pointer index.
     *
     * @return Index of front element
     */
    public int getFront() {
        return this.front;
    }

    /**
     * Return the current rear pointer index.
     *
     * @return Index of next insertion position
     */
    public int getRear() {
        return this.rear;
    }

    /**
     * Return the current number of elements in the queue.
     *
     * @return Current size
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Return the maximum capacity of the queue.
     *
     * @return Maximum capacity
     */
    public int getMaxSize() {
        return this.maxSize;
    }

    /**
     * Return the elements currently stored in the queue in FIFO order (from front to rear).
     *
     * @return Array containing elements in FIFO order
     */
    public int[] getQueueContents() {
        int[] contents = new int[this.size];
        for (int i = 0; i < this.size; i++) {
            contents[i] = this.queue[(this.front + i) % this.maxSize];
        }
        return contents;
    }

    /**
     * Return the status of the queue.
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
     * Return a formatted representation of the queue state suitable for UI or CLI logging.
     *
     * @return Formatted queue state string
     */
    public String getQueueState() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Queue [Front: %d, Rear: %d, Size: %d/%d, Status: %s]%n",
                this.front, this.rear, this.size, this.maxSize, getStatus()));
        sb.append("Contents (Front -> Rear): ");
        if (isEmpty()) {
            sb.append("[Empty]");
        } else {
            sb.append("[");
            for (int i = 0; i < this.size; i++) {
                int val = this.queue[(this.front + i) % this.maxSize];
                sb.append(String.format("0x%02X (%d)", val, val));
                if (i < this.size - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString().trim();
    }
}
