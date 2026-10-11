import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IPCMessage.java
 *
 * Encapsulates a structured, framed message exchanged between the UI, Core, and Logger
 * processes in the PIC16F72 Virtual Simulator.
 *
 * Supported Standard Message Types:
 * - EXECUTE   : UI requests Core to step/run an instruction or batch
 * - RESET     : UI requests Core to reset CPU, registers, memory, and peripherals
 * - RESULT    : Core returns execution outcome and state snapshot to UI
 * - ERROR     : Error notification (e.g., execution failure, syntax error)
 * - LOG       : Core/UI sends timestamped logging event to Logger process
 * - SHUTDOWN  : Process coordination message requesting clean termination
 * - LOAD      : UI requests Core to load an assembly program into memory
 * - STATE     : UI requests current state snapshot from Core
 * - ACK       : Generic receipt acknowledgment
 *
 * Wire Protocol & Framing:
 * 1. Stream Framing:
 *    [4-byte Big-Endian Integer: Length N] followed by [N bytes: UTF-8 Envelope]
 *    This length prefix guarantees reliable framing across stream reads, preventing
 *    partial-read or buffer-fragmentation issues on both Windows Named Pipes and POSIX FIFOs.
 * 2. Envelope Format:
 *    TYPE|REQUEST_ID|TIMESTAMP|BASE64_PAYLOAD
 *    The payload is Base64 encoded, guaranteeing that spaces, newlines, pipes (|),
 *    quotes, comments, or special characters can never corrupt envelope parsing.
 *
 * Example Wire Envelope:
 *    EXECUTE|req-101|1730000000000|TU9WTFcgNQ==
 */
public class IPCMessage {

    // Maximum allowable message payload size (10 MB) to guard against corrupted frames
    public static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;

    // Field delimiter used in envelope
    public static final String FIELD_DELIMITER_REGEX = "\\|";
    public static final String WIRE_DELIMITER = "|";

    // Standard message categories
    public static final String TYPE_EXECUTE   = "EXECUTE";
    public static final String TYPE_RESET     = "RESET";
    public static final String TYPE_RESULT    = "RESULT";
    public static final String TYPE_ERROR     = "ERROR";
    public static final String TYPE_LOG       = "LOG";
    public static final String TYPE_SHUTDOWN  = "SHUTDOWN";
    public static final String TYPE_LOAD      = "LOAD";
    public static final String TYPE_STATE     = "STATE";
    public static final String TYPE_ACK       = "ACK";

    private static final Set<String> KNOWN_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        TYPE_EXECUTE, TYPE_RESET, TYPE_RESULT, TYPE_ERROR, TYPE_LOG,
        TYPE_SHUTDOWN, TYPE_LOAD, TYPE_STATE, TYPE_ACK
    )));

    private String type;
    private String requestId;
    private long timestamp;
    private String payload;

    /**
     * Default constructor.
     */
    public IPCMessage() {
        this("UNKNOWN", UUID.randomUUID().toString(), System.currentTimeMillis(), "");
    }

    /**
     * Creates an IPCMessage with an auto-generated UUID requestId and current timestamp.
     *
     * @param type Message category / command
     * @param payload Message content
     */
    public IPCMessage(String type, String payload) {
        this(type, UUID.randomUUID().toString(), System.currentTimeMillis(), payload);
    }

    /**
     * Creates an IPCMessage with a specified requestId.
     *
     * @param type Message category / command
     * @param requestId Correlation identifier for matching responses to commands
     * @param payload Message content
     */
    public IPCMessage(String type, String requestId, String payload) {
        this(type, requestId, System.currentTimeMillis(), payload);
    }

    /**
     * Full constructor with explicit timestamp.
     *
     * @param type Message category
     * @param requestId Correlation ID
     * @param timestamp Epoch milliseconds
     * @param payload Message content
     */
    public IPCMessage(String type, String requestId, long timestamp, String payload) {
        setType(type);
        this.requestId = (requestId != null && !requestId.trim().isEmpty()) ? requestId.trim() : UUID.randomUUID().toString();
        this.timestamp = (timestamp > 0) ? timestamp : System.currentTimeMillis();
        this.payload = (payload != null) ? payload : "";
    }

    // =========================================================================
    // Static Factory Helpers
    // =========================================================================

    public static IPCMessage createExecute(String requestId, String commandOrSource) {
        return new IPCMessage(TYPE_EXECUTE, requestId, commandOrSource);
    }

    public static IPCMessage createReset(String requestId) {
        return new IPCMessage(TYPE_RESET, requestId, "");
    }

    public static IPCMessage createResult(String requestId, String resultData) {
        return new IPCMessage(TYPE_RESULT, requestId, resultData);
    }

    public static IPCMessage createError(String requestId, String errorMessage) {
        return new IPCMessage(TYPE_ERROR, requestId, errorMessage);
    }

    public static IPCMessage createLog(String logMessage) {
        return new IPCMessage(TYPE_LOG, UUID.randomUUID().toString(), logMessage);
    }

    public static IPCMessage createShutdown(String reason) {
        return new IPCMessage(TYPE_SHUTDOWN, UUID.randomUUID().toString(), reason);
    }

    public static IPCMessage createLoad(String requestId, String assemblySource) {
        return new IPCMessage(TYPE_LOAD, requestId, assemblySource);
    }

    public static IPCMessage createState(String requestId) {
        return new IPCMessage(TYPE_STATE, requestId, "");
    }

    public static IPCMessage createAck(String requestId, String message) {
        return new IPCMessage(TYPE_ACK, requestId, message);
    }

    // =========================================================================
    // Getters and Setters with Validation
    // =========================================================================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Message type cannot be null or empty.");
        }
        String normalized = type.trim().toUpperCase();
        // Custom types are supported as long as they are valid alphanumeric identifiers
        if (!normalized.matches("^[A-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid message type: '" + type + "'. Must be alphanumeric uppercase.");
        }
        this.type = normalized;
    }

    public boolean isKnownType() {
        return KNOWN_TYPES.contains(this.type);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = (requestId != null) ? requestId.trim() : "";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = (payload != null) ? payload : "";
    }

    // =========================================================================
    // Envelope Serialization & Deserialization
    // =========================================================================

    /**
     * Serializes this message into an envelope string:
     * TYPE|REQUEST_ID|TIMESTAMP|BASE64_PAYLOAD
     *
     * @return Formatted envelope string
     */
    public String serialize() {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        String base64Payload = Base64.getEncoder().encodeToString(payloadBytes);
        return type + WIRE_DELIMITER + requestId + WIRE_DELIMITER + timestamp + WIRE_DELIMITER + base64Payload;
    }

    /**
     * Deserializes a message from an envelope string.
     *
     * @param wireData Serialized envelope string
     * @return Reconstructed IPCMessage
     * @throws IllegalArgumentException if envelope is invalid or malformed
     */
    public static IPCMessage deserialize(String wireData) {
        if (wireData == null || wireData.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot deserialize null or empty wire data.");
        }

        String[] parts = wireData.split(FIELD_DELIMITER_REGEX, 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Malformed message envelope: expected 4 fields, got " + parts.length);
        }

        String type = parts[0].trim().toUpperCase();
        String reqId = parts[1].trim();

        long time;
        try {
            time = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Malformed timestamp in message envelope: " + parts[2], ex);
        }

        String rawPayload = "";
        if (!parts[3].isEmpty()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(parts[3]);
                rawPayload = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Malformed Base64 payload in message envelope: " + ex.getMessage(), ex);
            }
        }

        return new IPCMessage(type, reqId, time, rawPayload);
    }

    // =========================================================================
    // Stream Framing (Length-Prefixed I/O for Named Pipes and FIFOs)
    // =========================================================================

    /**
     * Writes this message with a 4-byte big-endian length prefix to an OutputStream.
     * This ensures the frame is completely and unambiguously delimited on any byte pipe.
     *
     * @param out Target output stream (e.g. pipe OutputStream)
     * @throws IOException on I/O failure
     */
    public void writeDelimitedTo(OutputStream out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("OutputStream cannot be null.");
        }
        byte[] envelopeBytes = serialize().getBytes(StandardCharsets.UTF_8);
        if (envelopeBytes.length > MAX_FRAME_SIZE) {
            throw new IOException("Message frame size (" + envelopeBytes.length + " bytes) exceeds limit of " + MAX_FRAME_SIZE);
        }

        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(envelopeBytes.length);
        dataOut.write(envelopeBytes);
        dataOut.flush();
    }

    /**
     * Reads the next length-prefixed message frame from an InputStream.
     * Guarantees that complete message bytes are accumulated even if the underlying
     * OS pipe delivers data across multiple partial reads.
     *
     * @param in Source input stream (e.g. pipe InputStream)
     * @return Reconstructed IPCMessage, or null if end-of-stream (EOF) is reached cleanly
     * @throws IOException on read errors or malformed frames
     */
    public static IPCMessage readDelimitedFrom(InputStream in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null.");
        }
        DataInputStream dataIn = new DataInputStream(in);

        int frameLength;
        try {
            frameLength = dataIn.readInt();
        } catch (EOFException ex) {
            // Clean EOF from remote process disconnect
            return null;
        }

        if (frameLength <= 0 || frameLength > MAX_FRAME_SIZE) {
            throw new IOException("Invalid message frame length received: " + frameLength);
        }

        byte[] buffer = new byte[frameLength];
        dataIn.readFully(buffer);

        String envelope = new String(buffer, StandardCharsets.UTF_8);
        return deserialize(envelope);
    }

    // =========================================================================
    // Display & Debugging
    // =========================================================================

    @Override
    public String toString() {
        String preview = payload;
        if (preview.length() > 60) {
            preview = preview.substring(0, 57) + "...";
        }
        preview = preview.replace("\r", "").replace("\n", "\\n");
        return String.format("IPCMessage[type=%s, id=%s, time=%d, payload=\"%s\"]", type, requestId, timestamp, preview);
    }
}
