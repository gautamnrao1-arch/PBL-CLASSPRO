import java.util.ArrayDeque;

/**
 * FIFOQueue.java
 *
 * FIFO Queue used by the Core Process.
 */
public class FIFOQueue {

    private static final int QUEUE_CAPACITY = 8;

    private final ArrayDeque<Integer> queue;

    public FIFOQueue() {
        queue = new ArrayDeque<>();
    }

    // ENQUEUE operation
    public boolean enqueue(int value) {
        if (isFull()) {
            return false;
        }

        queue.addLast(value & 0xFF);
        return true;
    }

    // DEQUEUE operation
    public int dequeue() {
        if (isEmpty()) {
            return -1;
        }

        return queue.removeFirst();
    }

    public int peek() {
        if (isEmpty()) {
            return -1;
        }

        return queue.peekFirst();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isFull() {
        return queue.size() >= QUEUE_CAPACITY;
    }

    public int size() {
        return queue.size();
    }

    public void reset() {
        queue.clear();
    }

    public String getState() {
        if (queue.isEmpty()) {
            return "Queue: EMPTY";
        }

        return "Queue: " + queue.toString();
    }
}