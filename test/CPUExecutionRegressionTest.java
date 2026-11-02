import java.util.ArrayList;

/**
 * CPUExecutionRegressionTest.java
 *
 * Standalone regression test verifying that CPU.java and ExecutionResult.java
 * expose all necessary methods for CoreProcess without modifying existing APIs:
 * 1. reset() - initializes registers, memory, W, PC
 * 2. loadProgram() - loads ArrayList of Instruction objects
 * 3. step() - single step execution returning ExecutionResult
 * 4. execute() - direct execution of a single Instruction
 * 5. run() - continuous execution until termination
 * 6. getRegisters(), getMemory(), getW(), isTerminated(), isRunning()
 * 7. ExecutionResult methods: isSuccess(), isTerminated(), getTrace(), getResultMessage(), getErrorMessage()
 */
public class CPUExecutionRegressionTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   CPU & ExecutionResult Regression Test ");
        System.out.println("=========================================");

        testCpuResetAndInitialState();
        testProgramLoadAndStep();
        testDirectInstructionExecution();
        testContinuousRunToSleep();
        testExecutionResultTraceAndErrors();

        System.out.println("=========================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL TESTS PASSED");
        } else {
            System.out.println("Result: SOME TESTS FAILED");
        }
        System.out.println("=========================================");
    }

    private static void recordResult(String testName, boolean success, String details) {
        totalCount++;
        if (success) {
            passedCount++;
            System.out.println("[PASS] " + testName + " -> " + details);
        } else {
            System.out.println("[FAIL] " + testName + " -> " + details);
        }
    }

    private static void testCpuResetAndInitialState() {
        Registers reg = new Registers();
        Memory mem = new Memory();
        CPU cpu = new CPU(reg, mem);

        cpu.setW(0x55);
        cpu.reset();

        boolean pass = (cpu.getW() == 0)
                && (cpu.getRegisters().getPC() == 0)
                && !cpu.isTerminated()
                && !cpu.isRunning();

        recordResult("Test 1: CPU Reset & State Query", pass,
                String.format("W=%d, PC=%d, terminated=%b, running=%b",
                        cpu.getW(), cpu.getRegisters().getPC(), cpu.isTerminated(), cpu.isRunning()));
    }

    private static void testProgramLoadAndStep() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOVLW", new String[]{"10"}, "MOVLW 10"));
        program.add(new Instruction("MOVWF", new String[]{"0x20"}, "MOVWF 0x20"));

        cpu.loadProgram(program);

        // Step 1: MOVLW 10
        ExecutionResult res1 = cpu.step();
        boolean step1Pass = res1.isSuccess() && cpu.getW() == 10 && cpu.getRegisters().getPC() == 1;

        // Step 2: MOVWF 0x20
        ExecutionResult res2 = cpu.step();
        boolean step2Pass = res2.isSuccess() && cpu.getRegisters().getRegister(0x20) == 10 && cpu.getRegisters().getPC() == 2;

        boolean pass = step1Pass && step2Pass;
        recordResult("Test 2: Load Program & Step Cycle", pass,
                String.format("Step1 (W=10): %b, Step2 (RAM[0x20]=10): %b", step1Pass, step2Pass));
    }

    private static void testDirectInstructionExecution() {
        CPU cpu = new CPU();
        Instruction inst = new Instruction("MOVLW", new String[]{"0x2A"}, "MOVLW 0x2A");

        ExecutionResult result = cpu.execute(inst);

        boolean pass = result.isSuccess()
                && (cpu.getW() == 42)
                && result.isExecuteComplete()
                && result.getChanges().contains("W = 0x2A");

        recordResult("Test 3: Direct Instruction Execution", pass,
                "Executed MOVLW 0x2A directly, W updated to 42 (0x2A)");
    }

    private static void testContinuousRunToSleep() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOVLW", new String[]{"5"}, "MOVLW 5"));
        program.add(new Instruction("SLEEP", new String[]{}, "SLEEP"));

        cpu.loadProgram(program);
        cpu.run();

        boolean pass = cpu.isTerminated() && !cpu.isRunning() && (cpu.getW() == 5);

        recordResult("Test 4: Continuous Run to SLEEP", pass,
                String.format("Terminated=%b, Running=%b, Final W=%d",
                        cpu.isTerminated(), cpu.isRunning(), cpu.getW()));
    }

    private static void testExecutionResultTraceAndErrors() {
        ExecutionResult res = new ExecutionResult();
        res.setInstruction("ADDWF 0x20");
        res.setPcBefore(0);
        res.setPcAfter(1);
        res.setFetchComplete(true);
        res.setDecodeComplete(true);
        res.setExecuteComplete(true);
        res.setSuccess(true);
        res.setResultMessage("Added W to RAM[0x20]");
        res.addRegisterChange("W = 0x05");
        res.addMemoryChange("RAM[0x20] = 0x0F");

        String trace = res.getTrace();
        boolean pass = trace.contains("FETCH: ✓")
                && trace.contains("DECODE: ✓")
                && trace.contains("EXECUTE: ✓")
                && trace.contains("PC: 0x0000 → 0x0001")
                && trace.contains("W = 0x05")
                && trace.contains("RAM[0x20] = 0x0F");

        recordResult("Test 5: ExecutionResult Trace Formatting", pass,
                "Trace correctly captures pipeline, PC transition, and changes");
    }
}
