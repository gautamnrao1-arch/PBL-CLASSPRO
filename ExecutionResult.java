import java.util.ArrayList;

/**
 * ExecutionResult.java
 *
 * Stores the detailed result of executing a single instruction in the PIC16F72 simulator.
 * Facilitates communication between CPU.java (which generates the execution data)
 * and SimulatorUI.java (which renders the execution trace, state changes, and pipeline stages).
 *
 * This class encapsulates:
 * - The instruction executed.
 * - PC values before and after execution.
 * - Completion statuses for FETCH, DECODE, and EXECUTE stages.
 * - Human-readable result message or error message.
 * - Detailed register and memory changes.
 * - Execution success and program termination flags.
 *
 * DESIGN RESTRICTIONS:
 * - This class does NOT execute any instructions.
 * - It serves purely as an execution report and data carrier.
 */
public class ExecutionResult {

    // The instruction text that was executed
    private String instruction;

    // Program Counter values
    private int pcBefore;
    private int pcAfter;

    // Pipeline stage completion statuses
    private boolean fetchComplete;
    private boolean decodeComplete;
    private boolean executeComplete;

    // Outcome message
    private String resultMessage;

    // State changes recorded during execution
    private final ArrayList<String> registerChanges;
    private final ArrayList<String> memoryChanges;

    // Execution status flags
    private boolean terminated;
    private boolean success;
    private String errorMessage;

    /**
     * Constructs a new ExecutionResult initialized with default empty states.
     */
    public ExecutionResult() {
        this.instruction = "";
        this.pcBefore = 0;
        this.pcAfter = 0;
        this.fetchComplete = false;
        this.decodeComplete = false;
        this.executeComplete = false;
        this.resultMessage = "";
        this.registerChanges = new ArrayList<String>();
        this.memoryChanges = new ArrayList<String>();
        this.terminated = false;
        this.success = true;
        this.errorMessage = "";
    }

    /**
     * Set the instruction text/representation executed.
     *
     * @param instruction Instruction string
     */
    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    /**
     * Return the instruction executed.
     *
     * @return Instruction string
     */
    public String getInstruction() {
        return this.instruction;
    }

    /**
     * Set the Program Counter value prior to execution.
     *
     * @param pc PC value before execution
     */
    public void setPcBefore(int pc) {
        this.pcBefore = pc;
    }

    /**
     * Return the Program Counter value prior to execution.
     *
     * @return PC value before execution
     */
    public int getPcBefore() {
        return this.pcBefore;
    }

    /**
     * Set the Program Counter value after execution.
     *
     * @param pc PC value after execution
     */
    public void setPcAfter(int pc) {
        this.pcAfter = pc;
    }

    /**
     * Return the Program Counter value after execution.
     *
     * @return PC value after execution
     */
    public int getPcAfter() {
        return this.pcAfter;
    }

    /**
     * Set the status of the instruction FETCH stage.
     *
     * @param status True if fetch succeeded
     */
    public void setFetchComplete(boolean status) {
        this.fetchComplete = status;
    }

    /**
     * Return whether the FETCH stage completed successfully.
     *
     * @return True if fetch completed
     */
    public boolean isFetchComplete() {
        return this.fetchComplete;
    }

    /**
     * Set the status of the instruction DECODE stage.
     *
     * @param status True if decode succeeded
     */
    public void setDecodeComplete(boolean status) {
        this.decodeComplete = status;
    }

    /**
     * Return whether the DECODE stage completed successfully.
     *
     * @return True if decode completed
     */
    public boolean isDecodeComplete() {
        return this.decodeComplete;
    }

    /**
     * Set the status of the instruction EXECUTE stage.
     *
     * @param status True if execute succeeded
     */
    public void setExecuteComplete(boolean status) {
        this.executeComplete = status;
    }

    /**
     * Return whether the EXECUTE stage completed successfully.
     *
     * @return True if execute completed
     */
    public boolean isExecuteComplete() {
        return this.executeComplete;
    }

    /**
     * Set the descriptive result message of the execution.
     *
     * @param message Result description
     */
    public void setResultMessage(String message) {
        this.resultMessage = message;
    }

    /**
     * Return the descriptive result message.
     *
     * @return Result message string
     */
    public String getResultMessage() {
        return this.resultMessage;
    }

    /**
     * Record a change to a CPU register.
     *
     * @param change Description of the register update (e.g. "W = 0x15", "STATUS.Z = 1")
     */
    public void addRegisterChange(String change) {
        if (change != null && !change.trim().isEmpty()) {
            this.registerChanges.add(change.trim());
        }
    }

    /**
     * Return a readable string representing all recorded register and memory changes.
     *
     * @return Consolidated changes description
     */
    public String getChanges() {
        StringBuilder sb = new StringBuilder();
        if (!this.registerChanges.isEmpty()) {
            sb.append("Registers: ").append(String.join(", ", this.registerChanges));
        }
        if (!this.memoryChanges.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Memory: ").append(String.join(", ", this.memoryChanges));
        }
        if (sb.length() == 0) {
            return "None";
        }
        return sb.toString();
    }

    /**
     * Record a change to simulated data memory.
     *
     * @param change Description of the memory update (e.g. "RAM[0x20] = 0x55")
     */
    public void addMemoryChange(String change) {
        if (change != null && !change.trim().isEmpty()) {
            this.memoryChanges.add(change.trim());
        }
    }

    /**
     * Set whether the simulator should terminate execution after this instruction
     * (e.g. on SLEEP, HALT, or end of program).
     *
     * @param terminated True if program reached termination
     */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * Return whether execution has terminated.
     *
     * @return True if terminated
     */
    public boolean isTerminated() {
        return this.terminated;
    }

    /**
     * Set the overall execution success status.
     *
     * @param success True if instruction executed without errors
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Return whether the execution was successful.
     *
     * @return True if successful
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Set the error message if execution failed.
     *
     * @param message Error description
     */
    public void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    /**
     * Return the error message if execution failed.
     *
     * @return Error message string
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Return a complete readable trace of this instruction cycle.
     * Shows pipeline status (FETCH ✓, DECODE ✓, EXECUTE ✓), result,
     * register changes, memory changes, and PC progression (PC before → after).
     *
     * @return Comprehensive multiline execution trace for SimulatorUI
     */
    public String getTrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("Instruction: ").append(this.instruction != null && !this.instruction.isEmpty() ? this.instruction : "N/A").append(System.lineSeparator());
        sb.append("FETCH: ").append(this.fetchComplete ? "✓" : "✗").append(System.lineSeparator());
        sb.append("DECODE: ").append(this.decodeComplete ? "✓" : "✗").append(System.lineSeparator());
        sb.append("EXECUTE: ").append(this.executeComplete ? "✓" : "✗").append(System.lineSeparator());
        sb.append(String.format("PC: 0x%04X → 0x%04X%n", this.pcBefore, this.pcAfter));
        sb.append("Result: ").append(this.resultMessage != null && !this.resultMessage.isEmpty() ? this.resultMessage : (this.success ? "Success" : "Failed")).append(System.lineSeparator());
        if (!this.success && this.errorMessage != null && !this.errorMessage.isEmpty()) {
            sb.append("Error: ").append(this.errorMessage).append(System.lineSeparator());
        }
        sb.append("Register changes: ").append(!this.registerChanges.isEmpty() ? String.join(", ", this.registerChanges) : "None").append(System.lineSeparator());
        sb.append("Memory changes: ").append(!this.memoryChanges.isEmpty() ? String.join(", ", this.memoryChanges) : "None");
        return sb.toString();
    }
}
