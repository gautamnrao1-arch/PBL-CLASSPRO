import java.util.ArrayList;

/**
 * CPU.java
 *
 * The central CPU controller of the PIC16F72 simulator.
 *
 * Implements the logical instruction processing pipeline:
 *   FETCH -> DECODE -> EXECUTE
 *
 * Coordinates state between:
 * - Registers: CPU register file, Program Counter (PC), STATUS register and arithmetic flags.
 * - Memory: Program memory (instructions) and simulated data memory.
 * - Working Register (W): Internal 8-bit accumulator.
 *
 * Week 2 Supported Instructions:
 * - DATA TRANSFER:       MOVLW, MOVWF
 * - ARITHMETIC:          ADDWF, SUBWF
 * - LOGICAL:             ANDWF
 * - INCREMENT/DECREMENT: INCF
 * - CONTROL FLOW:        GOTO
 * - TERMINATION:         SLEEP
 */
public class CPU {

    // Subsystem references
    private final Registers registers;
    private final Memory memory;

    // CPU internal state
    private int w; // 8-bit Working Register (Accumulator)
    private Instruction currentInstruction;
    private boolean terminated;
    private boolean running;

    /**
     * Default constructor.
     * Initializes the CPU with new Registers and Memory instances.
     */
    public CPU() {
        this(new Registers(), new Memory());
    }

    /**
     * Parameterized constructor.
     *
     * @param registers Existing Registers subsystem
     * @param memory Existing Memory subsystem
     */
    public CPU(Registers registers, Memory memory) {
        this.registers = (registers != null) ? registers : new Registers();
        this.memory = (memory != null) ? memory : new Memory();
        reset();
    }

    /**
     * Reset the CPU, registers, memory, and internal execution flags.
     */
    public void reset() {
        this.registers.reset();
        this.memory.reset();
        this.w = 0;
        this.currentInstruction = null;
        this.terminated = false;
        this.running = false;
    }

    /**
     * Load an assembly program into program memory and initialize CPU state.
     *
     * @param program List of Instruction objects to load
     */
    public void loadProgram(ArrayList<Instruction> program) {
        this.memory.loadProgram(program);
        this.registers.reset();
        this.w = 0;
        this.currentInstruction = null;
        this.terminated = false;
        this.running = false;
    }

    /**
     * Execute a single instruction cycle following the logical pipeline:
     * 1. Save PC before execution.
     * 2. fetch()
     * 3. Mark FETCH complete.
     * 4. decode()
     * 5. Mark DECODE complete.
     * 6. execute()
     * 7. Update status/flags as required.
     * 8. Save PC after execution.
     * 9. Return ExecutionResult.
     *
     * @return ExecutionResult containing pipeline trace and state changes
     */
    public ExecutionResult step() {
        // 1. Save PC before execution
        int pcBefore = this.registers.getPC();

        // Check if CPU is already terminated
        if (this.terminated) {
            ExecutionResult result = new ExecutionResult();
            result.setPcBefore(pcBefore);
            result.setPcAfter(pcBefore);
            result.setTerminated(true);
            result.setSuccess(false);
            result.setErrorMessage("Simulator execution already terminated.");
            result.setResultMessage("Execution stopped: CPU is terminated.");
            return result;
        }

        // 2. fetch()
        Instruction fetched = fetch();
        if (fetched == null) {
            // PC points outside loaded program memory
            ExecutionResult result = new ExecutionResult();
            result.setPcBefore(pcBefore);
            result.setPcAfter(this.registers.getPC());
            result.setFetchComplete(false);
            result.setDecodeComplete(false);
            result.setExecuteComplete(false);
            result.setSuccess(false);
            result.setErrorMessage("PC points outside loaded program (PC = " + pcBefore + ").");
            result.setResultMessage("Fetch failed: invalid program memory address.");
            this.terminated = true;
            this.running = false;
            result.setTerminated(true);
            return result;
        }

        // 3. Mark FETCH complete
        // 4. decode()
        Instruction decoded = decode(fetched);

        // 5. Mark DECODE complete
        // 6. execute()
        ExecutionResult result = execute(decoded);

        // Guarantee logical pipeline statuses and PC tracking
        result.setPcBefore(pcBefore);
        result.setFetchComplete(true);
        result.setDecodeComplete(true);
        result.setInstruction(fetched.toString());

        // 7. Update status/flags as required (handled inside execute())
        // 8. Save PC after execution
        result.setPcAfter(this.registers.getPC());

        // Check if execute resulted in termination (e.g. SLEEP)
        if (result.isTerminated()) {
            this.terminated = true;
            this.running = false;
        }

        // 9. Return ExecutionResult
        return result;
    }

    /**
     * Run the simulator continuously by stepping instructions until termination or error.
     */
    public void run() {
        this.running = true;
        while (!this.terminated && this.running) {
            ExecutionResult result = step();
            if (!result.isSuccess() || result.isTerminated()) {
                break;
            }
        }
        this.running = false;
    }

    /**
     * 1. Read current PC from Registers.
     * 2. Obtain Instruction from Memory using getInstruction(pc).
     * 3. Save as currentInstruction.
     * 4. Increment PC.
     * 5. Return the fetched Instruction.
     *
     * @return The fetched Instruction, or null if PC is invalid
     */
    public Instruction fetch() {
        int pc = this.registers.getPC();
        Instruction inst = this.memory.getInstruction(pc);
        this.currentInstruction = inst;
        if (inst != null) {
            this.registers.incrementPC();
        }
        return inst;
    }

    /**
     * Decode the instruction by reading its opcode and identifying operands.
     *
     * @param instruction The instruction to decode
     * @return The same Instruction object
     */
    public Instruction decode(Instruction instruction) {
        if (instruction == null) {
            return null;
        }
        // Validates/reads opcode
        instruction.getOpcode();
        return instruction;
    }

    /**
     * Execute the operation represented by the instruction opcode.
     *
     * @param instruction The instruction to execute
     * @return ExecutionResult detailing the outcome of execution
     */
    public ExecutionResult execute(Instruction instruction) {
        ExecutionResult result = new ExecutionResult();
        if (instruction == null) {
            result.setSuccess(false);
            result.setErrorMessage("Null instruction provided for execution.");
            result.setResultMessage("Execution failed: null instruction.");
            return result;
        }

        int pcBefore = instruction.getAddress();
        result.setPcBefore(pcBefore);
        result.setInstruction(instruction.toString());
        result.setFetchComplete(true);
        result.setDecodeComplete(true);

        String opcode = instruction.getOpcode();

        try {
            switch (opcode) {
                case "MOVLW": {
                    // Move Literal to W
                    int k = parseValue(instruction.getOperand(0)) & 0xFF;
                    this.w = k;
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("MOVLW 0x%02X (%d): loaded literal into W", k, k));
                    result.addRegisterChange(String.format("W = 0x%02X (%d)", this.w, this.w));
                    break;
                }

                case "MOVWF": {
                    // Move W to File Register
                    int f = parseValue(instruction.getOperand(0)) & 0xFF;
                    this.registers.setRegister(f, this.w);
                    this.memory.write(f, this.w);
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("MOVWF 0x%02X: wrote W (0x%02X) to register 0x%02X", f, this.w, f));
                    result.addRegisterChange(String.format("Reg[0x%02X] = 0x%02X", f, this.w));
                    result.addMemoryChange(String.format("DataMem[0x%02X] = 0x%02X", f, this.w));
                    break;
                }

                case "ADDWF": {
                    // Add W and File Register: dest = W + f
                    int f = parseValue(instruction.getOperand(0)) & 0xFF;
                    int d = parseDestination(instruction.getOperand(1));
                    int valF = this.registers.getRegister(f);
                    int sum = valF + this.w;
                    int res8 = sum & 0xFF;

                    // Update flags
                    this.registers.setCarryFlag(sum > 0xFF ? 1 : 0);
                    this.registers.setDigitCarryFlag(((valF & 0x0F) + (this.w & 0x0F)) > 0x0F ? 1 : 0);
                    this.registers.setZeroFlag(res8 == 0 ? 1 : 0);

                    // Write to destination (0 = W, 1 = f)
                    if (d == 0) {
                        this.w = res8;
                        result.addRegisterChange(String.format("W = 0x%02X", this.w));
                    } else {
                        this.registers.setRegister(f, res8);
                        this.memory.write(f, res8);
                        result.addRegisterChange(String.format("Reg[0x%02X] = 0x%02X", f, res8));
                        result.addMemoryChange(String.format("DataMem[0x%02X] = 0x%02X", f, res8));
                    }

                    result.addRegisterChange(String.format("STATUS = 0x%02X (%s)", this.registers.getStatus(), this.registers.getFlagState()));
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("ADDWF: Reg[0x%02X] (0x%02X) + W (0x%02X) = 0x%02X (dest=%s)", f, valF, this.w, res8, (d == 0 ? "W" : "F")));
                    break;
                }

                case "SUBWF": {
                    // Subtract W from File Register: dest = f - W
                    int f = parseValue(instruction.getOperand(0)) & 0xFF;
                    int d = parseDestination(instruction.getOperand(1));
                    int valF = this.registers.getRegister(f);
                    int diff = valF - this.w;
                    int res8 = diff & 0xFF;

                    // PIC16 flags: C=1 if no borrow (f >= W), C=0 if borrow (f < W)
                    this.registers.setCarryFlag(valF >= this.w ? 1 : 0);
                    this.registers.setDigitCarryFlag((valF & 0x0F) >= (this.w & 0x0F) ? 1 : 0);
                    this.registers.setZeroFlag(res8 == 0 ? 1 : 0);

                    // Write to destination (0 = W, 1 = f)
                    if (d == 0) {
                        this.w = res8;
                        result.addRegisterChange(String.format("W = 0x%02X", this.w));
                    } else {
                        this.registers.setRegister(f, res8);
                        this.memory.write(f, res8);
                        result.addRegisterChange(String.format("Reg[0x%02X] = 0x%02X", f, res8));
                        result.addMemoryChange(String.format("DataMem[0x%02X] = 0x%02X", f, res8));
                    }

                    result.addRegisterChange(String.format("STATUS = 0x%02X (%s)", this.registers.getStatus(), this.registers.getFlagState()));
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("SUBWF: Reg[0x%02X] (0x%02X) - W (0x%02X) = 0x%02X (dest=%s)", f, valF, this.w, res8, (d == 0 ? "W" : "F")));
                    break;
                }

                case "ANDWF": {
                    // AND W with File Register: dest = f & W
                    int f = parseValue(instruction.getOperand(0)) & 0xFF;
                    int d = parseDestination(instruction.getOperand(1));
                    int valF = this.registers.getRegister(f);
                    int res8 = (valF & this.w) & 0xFF;

                    // Zero flag affected
                    this.registers.setZeroFlag(res8 == 0 ? 1 : 0);

                    if (d == 0) {
                        this.w = res8;
                        result.addRegisterChange(String.format("W = 0x%02X", this.w));
                    } else {
                        this.registers.setRegister(f, res8);
                        this.memory.write(f, res8);
                        result.addRegisterChange(String.format("Reg[0x%02X] = 0x%02X", f, res8));
                        result.addMemoryChange(String.format("DataMem[0x%02X] = 0x%02X", f, res8));
                    }

                    result.addRegisterChange(String.format("STATUS = 0x%02X (%s)", this.registers.getStatus(), this.registers.getFlagState()));
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("ANDWF: Reg[0x%02X] (0x%02X) AND W (0x%02X) = 0x%02X (dest=%s)", f, valF, this.w, res8, (d == 0 ? "W" : "F")));
                    break;
                }

                case "INCF": {
                    // Increment File Register: dest = f + 1
                    int f = parseValue(instruction.getOperand(0)) & 0xFF;
                    int d = parseDestination(instruction.getOperand(1));
                    int valF = this.registers.getRegister(f);
                    int res8 = (valF + 1) & 0xFF;

                    // Zero flag affected
                    this.registers.setZeroFlag(res8 == 0 ? 1 : 0);

                    if (d == 0) {
                        this.w = res8;
                        result.addRegisterChange(String.format("W = 0x%02X", this.w));
                    } else {
                        this.registers.setRegister(f, res8);
                        this.memory.write(f, res8);
                        result.addRegisterChange(String.format("Reg[0x%02X] = 0x%02X", f, res8));
                        result.addMemoryChange(String.format("DataMem[0x%02X] = 0x%02X", f, res8));
                    }

                    result.addRegisterChange(String.format("STATUS = 0x%02X (%s)", this.registers.getStatus(), this.registers.getFlagState()));
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("INCF: Reg[0x%02X] (0x%02X) + 1 = 0x%02X (dest=%s)", f, valF, res8, (d == 0 ? "W" : "F")));
                    break;
                }

                case "GOTO": {
                    // Branch to target address
                    int target = parseValue(instruction.getOperand(0)) & 0x1FFF;
                    this.registers.setPC(target);
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setResultMessage(String.format("GOTO 0x%04X: branch to address", target));
                    result.addRegisterChange(String.format("PC = 0x%04X (%d)", target, target));
                    break;
                }

                case "SLEEP": {
                    // Processor sleep / program termination
                    this.terminated = true;
                    this.running = false;
                    result.setExecuteComplete(true);
                    result.setSuccess(true);
                    result.setTerminated(true);
                    result.setResultMessage("SLEEP: Processor entered sleep mode (program terminated).");
                    break;
                }

                default: {
                    // Unknown instruction
                    result.setExecuteComplete(false);
                    result.setSuccess(false);
                    result.setErrorMessage("Unknown opcode encountered: " + opcode);
                    result.setResultMessage("Execution error: unrecognized instruction " + opcode);
                    break;
                }
            }
        } catch (Exception ex) {
            result.setExecuteComplete(false);
            result.setSuccess(false);
            result.setErrorMessage("Error executing instruction " + opcode + ": " + ex.getMessage());
            result.setResultMessage("Execution failure: " + ex.getMessage());
        }

        result.setPcAfter(this.registers.getPC());
        return result;
    }

    /**
     * Return the Registers subsystem.
     *
     * @return Registers reference
     */
    public Registers getRegisters() {
        return this.registers;
    }

    /**
     * Return the Memory subsystem.
     *
     * @return Memory reference
     */
    public Memory getMemory() {
        return this.memory;
    }

    /**
     * Return whether the CPU is terminated.
     *
     * @return True if terminated
     */
    public boolean isTerminated() {
        return this.terminated;
    }

    /**
     * Return whether the CPU is actively running.
     *
     * @return True if running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Return the current instruction being executed.
     *
     * @return Current Instruction or null
     */
    public Instruction getCurrentInstruction() {
        return this.currentInstruction;
    }

    /**
     * Return the current value of the 8-bit Working Register (W).
     *
     * @return 8-bit W register value (0x00 - 0xFF)
     */
    public int getW() {
        return this.w & 0xFF;
    }

    /**
     * Set the value of the 8-bit Working Register (W).
     *
     * @param value 8-bit value to set into W
     */
    public void setW(int value) {
        this.w = value & 0xFF;
    }

    /**
     * Helper to parse integer operand values formatted as hex, decimal, or PIC assembly literals.
     *
     * @param s Operand string (e.g., "0x20", "20h", "H'20'", "32", "D'32'")
     * @return Parsed integer value
     */
    private int parseValue(String s) {
        if (s == null) {
            return 0;
        }
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16);
        }
        if (s.endsWith("h") || s.endsWith("H")) {
            return Integer.parseInt(s.substring(0, s.length() - 1), 16);
        }
        if ((s.startsWith("H'") || s.startsWith("h'")) && s.endsWith("'")) {
            return Integer.parseInt(s.substring(2, s.length() - 1), 16);
        }
        if ((s.startsWith("D'") || s.startsWith("d'")) && s.endsWith("'")) {
            return Integer.parseInt(s.substring(2, s.length() - 1), 10);
        }
        if ((s.startsWith("B'") || s.startsWith("b'")) && s.endsWith("'")) {
            return Integer.parseInt(s.substring(2, s.length() - 1), 2);
        }
        if (s.startsWith(".")) {
            return Integer.parseInt(s.substring(1), 10);
        }
        return Integer.parseInt(s);
    }

    /**
     * Helper to parse the destination operand 'd' in byte-oriented instructions.
     * In PIC microcontrollers:
     *   0 or W -> destination is W register.
     *   1 or F -> destination is File register f (default).
     *
     * @param s Destination operand string
     * @return 0 for W, 1 for F
     */
    private int parseDestination(String s) {
        if (s == null) {
            return 1;
        }
        s = s.trim().toUpperCase();
        if (s.equals("0") || s.equals("W")) {
            return 0;
        }
        if (s.equals("1") || s.equals("F")) {
            return 1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
