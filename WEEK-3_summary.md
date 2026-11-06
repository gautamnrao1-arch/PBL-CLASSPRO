WEEK 3 – PBL PROJECT STATUS
PIC16F72 Virtual Simulator
1. Week 3 Objective

Extend the Week 2 CPU simulator with Memory read/write, a Stack with Stack Pointer (PUSH/POP) and a FIFO Queue (Enqueue/Dequeue), and validate them with a flowchart, an assembly program and test cases.

2. Implementation Completed

Memory (Memory.java)

256 bytes of 8-bit data memory (values masked with & 0xFF).
Program memory stored as an ArrayList<Instruction>.
read(), write(), getInstruction() and reset() handle invalid addresses safely.

Stack (PICStack.java)

8-level stack, matching the PIC16F72 hardware stack depth.
Stack Pointer (SP) counts stored elements (0 = empty, 8 = full).
push() returns false on overflow. pop() returns null on underflow.

FIFO Queue (FIFOQueue.java)

Circular queue of capacity 8 using front, rear and size.
enqueue() and dequeue() keep first-in, first-out order, including after wrap-around.
Overflow returns false and underflow returns null.

CPU Integration (CPU.java) 

CPU holds the Registers, Memory, PICStack and FIFOQueue objects.
PUSH, POP, ENQUEUE and DEQUEUE run through FETCH → DECODE → EXECUTE.
3. Instruction Support

Added to the Week 2 set (MOVLW, MOVWF, ADDWF, SUBWF, ANDWF, INCF, GOTO, SLEEP):

PUSH, POP, ENQUEUE, DEQUEUE

These four are simulator operations, not standard PIC16F72 instructions.

4. User Interface Updates
Added MEMORY, STACK and FIFO QUEUE panels, and SP in the CPU state panel.
parseProgram() now recognizes the Week 3 operations.
5. Queue Flowchart and Assembly Validation [confirm: add file names]
Flowchart covers Enqueue, Dequeue, empty condition, full condition and queue status update.
Assembly program enqueues several values (for example 30, 40, 50) and dequeues them.
Expected: values come out in FIFO order. Actual: (fill in)

6. Test Suite
Test	Area	Operation	Expected Result	Status
TC09	Memory	write 0x55 to 0x20, then read	0x55	(PASS/FAIL)
TC10	Memory	write 300, then read	0x2C (8-bit wrap)	(PASS/FAIL)
TC12	Stack	push 10, push 20	SP = 2	(PASS/FAIL)
TC13	Stack	pop	20 (LIFO), SP = 1	(PASS/FAIL)
TC14	Stack	pop on empty stack	null	(PASS/FAIL)
TC16	Queue	enqueue 30, 40, 50, dequeue ×3	30, 40, 50	(PASS/FAIL)
TC17	Queue	dequeue on empty queue	null	(PASS/FAIL)
TC18	Queue	9 enqueues into 8 slots	9th returns false	(PASS/FAIL)

Week 2 tests TC01–TC08 passed and are carried forward.

7. Files Added or Updated
PICStack.java, FIFOQueue.java, Memory.java
CPU.java (stack and queue integration)
SimulatorUI.java (Memory, Stack and Queue panels)
Queue flowchart, assembly program and test results

8. Current Limitations / Pending Verification
The actual test output must be checked to confirm all tests pass.
PUSH, POP, ENQUEUE and DEQUEUE are not real PIC16F72 instructions, so confirm with faculty that this is acceptable.
RESET also clears the loaded program.
readme.md still has merge-conflict markers.

9. Week 3 Outcome

The simulator now runs CPU + Memory + Stack + FIFO Queue, with all three structures visible in the UI and covered by test cases. It is ready for the Week 4 multi-process and IPC work.

