import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * CoreProcess.java
 *
 * Runs as the independent operating-system process containing the PIC16F72 microcontroller
 * CPU simulator, registers, memory, stack, and FIFO queue.
 *
 * Architectural Responsibilities:
 * - Owns and executes CPU instructions.
 * - Receives simulation commands (LOAD, STEP, RUN, RESET, STATE, SHUTDOWN) from UI over {@link IPCConstants#CHANNEL_UI_CORE}.
 * - Returns execution outcomes and state snapshots to UI.
 * - Forwards operational audit logs to Logger over {@link IPCConstants#CHANNEL_CORE_LOGGER}.
 */
public class CoreProcess {

    private final Registers registers;
    private final Memory memory;
    private final CPU cpu;
    private final PICStack stack;
    private final FIFOQueue queue;

    private IPCManager loggerIPC;
    private IPCManager uiIPC;
    private volatile boolean running = true;

    public CoreProcess() {
        this.registers = new Registers();
        this.memory = new Memory();
        this.cpu = new CPU(registers, memory);
        this.stack = new PICStack();
        this.queue = new FIFOQueue();
    }

    public static void main(String[] args) {
        System.out.println("[CORE] Starting Core Simulator Process...");
        CoreProcess core = new CoreProcess();
        core.run();
    }

    /**
     * Executes the Core process lifecycle: connects to Logger, binds UI server,
     * processes commands in a loop, and cleanly terminates on SHUTDOWN.
     */
    public void run() {
        // 1. Connect to Logger process
        System.out.println("[CORE] Connecting to Logger process on channel: " + IPCConstants.CHANNEL_CORE_LOGGER);
        try {
            loggerIPC = IPCFactory.createClient(IPCConstants.CHANNEL_CORE_LOGGER);
            loggerIPC.connectClient();
            sendLog("Core simulator process initialized. PIC16F72 CPU ready.");
        } catch (IOException e) {
            System.err.println("[CORE WARNING] Failed to connect to Logger: " + e.getMessage() + ". Continuing without remote logger.");
        }

        // 2. Start UI server pipe
        System.out.println("[CORE] Binding UI server on channel: " + IPCConstants.CHANNEL_UI_CORE);
        try (IPCManager server = IPCFactory.createServer(IPCConstants.CHANNEL_UI_CORE)) {
            this.uiIPC = server;
            server.startServer();
            sendLog("Core process connected to UI process on channel '" + IPCConstants.CHANNEL_UI_CORE + "'");
            System.out.println("[CORE] UI client connected. Awaiting commands...");

            // 3. Command loop
            while (running && server.isConnected()) {
                IPCMessage request = server.receiveMessage();
                if (request == null) {
                    System.out.println("[CORE] UI process disconnected (EOF).");
                    break;
                }

                IPCMessage response = handleMessage(request);
                if (response != null && server.isConnected()) {
                    server.sendMessage(response);
                }

                if (IPCMessage.TYPE_SHUTDOWN.equalsIgnoreCase(request.getType())) {
                    System.out.println("[CORE] Shutdown requested. Exiting Core process.");
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("[CORE ERROR] IPC Error: " + e.getMessage());
        } finally {
            closeAll();
        }
    }

    /**
     * Dispatches an incoming UI request to the corresponding simulator action.
     */
    public IPCMessage handleMessage(IPCMessage request) {
        String type = request.getType();
        String reqId = request.getRequestId();
        String payload = request.getPayload();

        try {
            switch (type) {
                case IPCMessage.TYPE_LOAD: {
                    ArrayList<Instruction> instructions = parseProgram(payload);
                    if (instructions.isEmpty()) {
                        sendLog("LOAD ERROR: No valid assembly instructions in payload.");
                        return IPCMessage.createError(reqId, "No valid assembly instructions found in input.");
                    }
                    cpu.loadProgram(instructions);
                    sendLog(String.format("LOAD SUCCESS: Loaded %d instruction(s) into memory.", instructions.size()));
                    String snapshot = serializeStateSnapshot("PROGRAM LOADED",
                            String.format("Successfully loaded %d instruction(s) into memory.", instructions.size()));
                    return IPCMessage.createResult(reqId, snapshot);
                }

                case IPCMessage.TYPE_RESET: {
                    cpu.reset();
                    stack.reset();
                    queue.reset();
                    sendLog("RESET: Microcontroller CPU, registers, memory and stack reset to initial state.");
                    String snapshot = serializeStateSnapshot("READY", "Simulator reset: registers and memory cleared.");
                    return IPCMessage.createResult(reqId, snapshot);
                }

                case IPCMessage.TYPE_EXECUTE: {
                    String cmd = (payload != null) ? payload.trim().toUpperCase() : IPCConstants.CMD_STEP;
                    if (cmd.startsWith(IPCConstants.CMD_STEP)) {
                        ExecutionResult result = cpu.step();
                        String status;
                        if (!result.isSuccess()) {
                            status = "ERROR: " + result.getErrorMessage();
                        } else if (result.isTerminated() || cpu.isTerminated()) {
                            status = "TERMINATED";
                        } else {
                            status = "EXECUTING";
                        }
                        sendLog(String.format("STEP: PC=0x%04X, W=0x%02X, Status=%s",
                                registers.getPC(), cpu.getW(), status));
                        String snapshot = serializeStateSnapshot(status, result.getTrace());
                        return IPCMessage.createResult(reqId, snapshot);
                    } else if (cmd.startsWith(IPCConstants.CMD_RUN)) {
                        cpu.run();
                        String status = cpu.isTerminated() ? "TERMINATED" : "READY";
                        StringBuilder summary = new StringBuilder();
                        summary.append("Program run completed.").append(System.lineSeparator());
                        summary.append("Terminated: ").append(cpu.isTerminated()).append(System.lineSeparator());
                        summary.append(String.format("Final PC: 0x%04X (%d)%n", registers.getPC(), registers.getPC()));
                        summary.append(String.format("Final W:  0x%02X (%d)%n", cpu.getW(), cpu.getW()));
                        summary.append(String.format("Final STATUS: 0x%02X [%s]%n", registers.getStatus(), registers.getFlagState()));

                        sendLog(String.format("RUN: Completed. Terminated=%b, PC=0x%04X, W=0x%02X",
                                cpu.isTerminated(), registers.getPC(), cpu.getW()));
                        String snapshot = serializeStateSnapshot(status, summary.toString());
                        return IPCMessage.createResult(reqId, snapshot);
                    } else {
                        return IPCMessage.createError(reqId, "Unknown execution command: " + cmd);
                    }
                }

                case IPCMessage.TYPE_STATE: {
                    String snapshot = serializeStateSnapshot("READY", "Current simulator state snapshot.");
                    return IPCMessage.createResult(reqId, snapshot);
                }

                case IPCMessage.TYPE_SHUTDOWN: {
                    sendLog("SHUTDOWN: Core simulator stopping by request.");
                    if (loggerIPC != null && loggerIPC.isConnected()) {
                        try {
                            loggerIPC.sendMessage(IPCMessage.createShutdown("Core shutting down"));
                        } catch (IOException ignored) {}
                    }
                    running = false;
                    return IPCMessage.createAck(reqId, "Core shutting down");
                }

                default:
                    return IPCMessage.createError(reqId, "Unsupported message type: " + type);
            }
        } catch (Exception ex) {
            sendLog("EXCEPTION in Core: " + ex.getMessage());
            return IPCMessage.createError(reqId, "Core execution error: " + ex.getMessage());
        }
    }

    /**
     * Builds and serializes the complete CPU, memory, registers, stack, and queue state snapshot
     * dictionary using the standard keys from IPCConstants.
     */
    public String serializeStateSnapshot(String statusStr, String traceText) {
        Map<String, String> snapshot = new HashMap<String, String>();

        snapshot.put(IPCConstants.KEY_STATUS_STR, statusStr != null ? statusStr : "READY");
        snapshot.put(IPCConstants.KEY_PC, String.valueOf(registers.getPC()));
        snapshot.put(IPCConstants.KEY_W, String.valueOf(cpu.getW()));
        snapshot.put(IPCConstants.KEY_STATUS_REG, String.valueOf(registers.getStatus()));
        snapshot.put(IPCConstants.KEY_FLAG_Z, String.valueOf(registers.getZeroFlag()));
        snapshot.put(IPCConstants.KEY_FLAG_C, String.valueOf(registers.getCarryFlag()));
        snapshot.put(IPCConstants.KEY_FLAG_DC, String.valueOf(registers.getDigitCarryFlag()));

        Instruction currentInst = cpu.getCurrentInstruction();
        snapshot.put(IPCConstants.KEY_CURRENT_INST, currentInst != null ? currentInst.toString() : "None");
        snapshot.put(IPCConstants.KEY_TRACE, traceText != null ? traceText : "");

        // Build Program View with PC pointer
        ArrayList<Instruction> program = memory.getProgram();
        StringBuilder progText = new StringBuilder();
        int pc = registers.getPC();
        for (int i = 0; i < program.size(); i++) {
            Instruction inst = program.get(i);
            String pointer = (i == pc && !cpu.isTerminated()) ? "--> " : "    ";
            progText.append(String.format("%s%2d: %s%n", pointer, i, inst.getRawInstruction()));
        }
        if (program.isEmpty()) {
            progText.append("(No program loaded)");
        }
        snapshot.put(IPCConstants.KEY_PROGRAM_VIEW, progText.toString());

        // Build Register Dump
        StringBuilder regText = new StringBuilder();
        regText.append("Special Function Registers:").append(System.lineSeparator());
        regText.append(String.format("  INDF  (0x00): 0x%02X | TMR0 (0x01): 0x%02X%n", registers.getRegister(0x00), registers.getRegister(0x01)));
        regText.append(String.format("  PCL   (0x02): 0x%02X | FSR  (0x04): 0x%02X%n", registers.getRegister(0x02), registers.getRegister(0x04)));
        regText.append(String.format("  PORTA (0x05): 0x%02X | PORTB(0x06): 0x%02X%n", registers.getRegister(0x05), registers.getRegister(0x06)));
        regText.append(System.lineSeparator());
        regText.append("General Purpose Registers (RAM 0x20 - 0x2F):").append(System.lineSeparator());
        for (int addr = 0x20; addr <= 0x2F; addr += 4) {
            regText.append(String.format("  [0x%02X]: %02X  [0x%02X]: %02X  [0x%02X]: %02X  [0x%02X]: %02X%n",
                    addr, registers.getRegister(addr),
                    addr + 1, registers.getRegister(addr + 1),
                    addr + 2, registers.getRegister(addr + 2),
                    addr + 3, registers.getRegister(addr + 3)));
        }

        boolean hasOther = false;
        StringBuilder otherRegs = new StringBuilder();
        for (int addr = 0; addr < Registers.TOTAL_REGISTERS; addr++) {
            if (addr >= 0x20 && addr <= 0x2F) continue;
            if (addr <= 0x07) continue;
            int val = registers.getRegister(addr);
            if (val != 0) {
                if (!hasOther) {
                    otherRegs.append(System.lineSeparator()).append("Other Active Registers:").append(System.lineSeparator());
                    hasOther = true;
                }
                otherRegs.append(String.format("  [0x%02X] = 0x%02X (%d)%n", addr, val, val));
            }
        }
        if (hasOther) {
            regText.append(otherRegs);
        }
        snapshot.put(IPCConstants.KEY_REG_DUMP, regText.toString());

        snapshot.put(IPCConstants.KEY_MEM_DUMP, memory.getMemoryState());
        snapshot.put(IPCConstants.KEY_STACK_STATE, stack.getStackState());
        snapshot.put(IPCConstants.KEY_QUEUE_STATE, queue.getState());

        return encodeDictionary(snapshot);
    }

    /**
     * Encodes a key-value map safely into a line-delimited Base64 string payload.
     */
    public static String encodeDictionary(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String b64Val = Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8));
            sb.append(entry.getKey()).append("=").append(b64Val).append("\n");
        }
        return sb.toString();
    }

    /**
     * Decodes a line-delimited Base64 dictionary string into a map.
     */
    public static Map<String, String> decodeDictionary(String payload) {
        Map<String, String> map = new HashMap<String, String>();
        if (payload == null || payload.trim().isEmpty()) {
            return map;
        }
        String[] lines = payload.split("\\r?\\n");
        for (String line : lines) {
            int eqIdx = line.indexOf('=');
            if (eqIdx > 0) {
                String key = line.substring(0, eqIdx).trim();
                String valStr = line.substring(eqIdx + 1).trim();
                try {
                    String decoded = new String(Base64.getDecoder().decode(valStr), StandardCharsets.UTF_8);
                    map.put(key, decoded);
                } catch (IllegalArgumentException ex) {
                    map.put(key, valStr);
                }
            }
        }
        return map;
    }

    /**
     * Sends a log message to the Logger process if connected.
     */
    private void sendLog(String logMessage) {
        System.out.println("[CORE LOG] " + logMessage);
        if (loggerIPC != null && loggerIPC.isConnected()) {
            try {
                loggerIPC.sendMessage(IPCMessage.createLog(logMessage));
            } catch (IOException e) {
                System.err.println("[CORE WARNING] Failed to send log to Logger: " + e.getMessage());
            }
        }
    }

    private void closeAll() {
        if (uiIPC != null) {
            try { uiIPC.close(); } catch (IOException ignored) {}
        }
        if (loggerIPC != null) {
            try { loggerIPC.close(); } catch (IOException ignored) {}
        }
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

    public PICStack getStack() {
        return stack;
    }

    public FIFOQueue getQueue() {
        return queue;
    }

    /**
     * Parses multiline assembly text into an ArrayList of Instruction objects.
     */
    public static ArrayList<Instruction> parseProgram(String text) {
        ArrayList<Instruction> instructions = new ArrayList<Instruction>();
        if (text == null || text.trim().isEmpty()) {
            return instructions;
        }

        String[] lines = text.split("\\r?\\n");
        int address = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith(";") || line.startsWith("//")) {
                continue;
            }

            int commentIndex = line.indexOf(';');
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex).trim();
            }
            commentIndex = line.indexOf("//");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex).trim();
            }

            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("[,\\s]+");
            if (tokens.length == 0 || tokens[0].trim().isEmpty()) {
                continue;
            }

            String opcode = tokens[0].trim();
            String[] operands;
            if (tokens.length > 1) {
                operands = new String[tokens.length - 1];
                System.arraycopy(tokens, 1, operands, 0, tokens.length - 1);
            } else {
                operands = new String[0];
            }

            Instruction inst = new Instruction(opcode, operands, rawLine.trim());
            inst.setAddress(address++);
            instructions.add(inst);
        }

        return instructions;
    }
}
