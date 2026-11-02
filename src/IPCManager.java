import java.io.IOException;

/**
 * IPCManager.java
 *
 * Defines the shared Inter-Process Communication (IPC) interface for the
 * PIC16F72 Virtual Simulator across the UI, Core, and Logger operating-system processes.
 *
 * Architecture & Channel Role:
 * - UI Process <-> Core Process: Bidirectional command/response channel ("ui_core")
 * - Core Process -> Logger Process: Event/log publishing channel ("core_logger")
 *
 * Design Decision - Channel Scope:
 * Each IPCManager instance encapsulates a single dedicated channel endpoint.
 * Using separate channel instances rather than passing a channel identifier per-call
 * keeps OS pipe handles, buffers, and stream lifecycles isolated, modular, and leak-free.
 *
 * Communication Lifecycle:
 * 1. Creation: Instantiated via IPCFactory with a logical channel name and role.
 * 2. Initialization:
 *    - Server role calls {@link #startServer()} to bind the OS named pipe / FIFO and accept a connection.
 *    - Client role calls {@link #connectClient()} to connect to the active server pipe.
 * 3. Message Transfer:
 *    - {@link #sendMessage(IPCMessage)} serializes and transmits a structured frame.
 *    - {@link #receiveMessage()} reads and deserializes the next structured frame.
 * 4. Termination:
 *    - {@link #close()} shuts down streams and frees underlying OS pipe resources.
 *
 * Blocking Behavior:
 * - {@link #startServer()} blocks until a client connects.
 * - {@link #connectClient()} blocks until the server pipe is ready or connection fails.
 * - {@link #receiveMessage()} blocks until a complete framed message is received or EOF occurs.
 * - Callers that require non-blocking behavior (such as SimulatorUI) must dispatch calls
 *   on background threads (e.g., SwingWorker or dedicated worker threads).
 *
 * Error Handling:
 * - Broken pipes, unreadable streams, or malformed frames throw {@link java.io.IOException}.
 * - {@link #receiveMessage()} returns {@code null} if the peer shuts down cleanly (EOF).
 *
 * Thread-Safety:
 * - Implementations must ensure thread-safety for concurrent send operations, preventing
 *   interleaved wire bytes.
 */
public interface IPCManager extends AutoCloseable {

    /**
     * Binds the server-side named pipe or FIFO and blocks until a peer connects.
     *
     * @throws IOException if creating the pipe or accepting the connection fails
     */
    void startServer() throws IOException;

    /**
     * Connects as a client to an active server-side named pipe or FIFO.
     * Blocks until the connection is successfully established or fails.
     *
     * @throws IOException if the server pipe cannot be reached or connection fails
     */
    void connectClient() throws IOException;

    /**
     * Sends a structured message across the established IPC channel.
     *
     * @param message The structured message to transmit
     * @throws IOException if the channel is not connected or a write error occurs
     */
    void sendMessage(IPCMessage message) throws IOException;

    /**
     * Reads and decodes the next structured message from the channel.
     * Blocks until a complete framed message arrives.
     *
     * @return The parsed IPCMessage, or {@code null} if the peer cleanly closed the connection (EOF)
     * @throws IOException if a network/pipe read error occurs or a frame is malformed
     */
    IPCMessage receiveMessage() throws IOException;

    /**
     * Checks whether the channel is currently connected and ready for I/O operations.
     *
     * @return {@code true} if connected and open, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Returns the logical name of this communication channel (e.g. "ui_core", "core_logger").
     *
     * @return Channel identifier name
     */
    String getChannelName();

    /**
     * Closes the IPC channel, releasing all underlying streams and OS pipe handles.
     * This method is idempotent.
     *
     * @throws IOException if an error occurs during resource cleanup
     */
    @Override
    void close() throws IOException;
}
