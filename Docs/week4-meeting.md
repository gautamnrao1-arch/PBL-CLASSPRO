Meeting Minutes

Date: 18 October 2026  
Meeting Number: 4  
Participants: Gautam, Reeva, Yuktha, Sanvi

 Agenda

1. Discuss the Week 4 task of developing a Multi-Process PIC16F72 Simulator.
2. Divide the implementation work among the team members.
3. Assign responsibilities for UI, Core, Logging and Integration.
4. Discuss inter-process communication (IPC) and system testing.

 Discussion

- The team discussed separating the existing simulator into three independent processes: UI, Core and Logging.
- The team discussed implementing communication between the processes using a suitable POSIX IPC mechanism.
- The team discussed using threads wherever appropriate within the processes.
- The team discussed integration, IPC testing and comparing standalone and multi-process simulator performance.
- The implementation work and Java files were assigned according to each member's role.

Decisions

1. Gautam – Integration
   - `Main.java`
   - Integration of UI, Core and Logging processes.
   - Resolve IPC and interface issues.
   - Perform system testing and benchmarking.

2. Reeva – Core Process
   - `CPU.java`
   - `Registers.java`
   - `Memory.java`
   - `PICStack.java`
   - `FIFOQueue.java`
   - `Instruction.java`
   - `InstructionSet.java`
   - `ExecutionResult.java`

3. Yuktha – UI Process
   - `SimulatorUI.java`
   - Handle user interaction and simulator display.
   - Connect the UI with the Core process.

4. Sanvi – Logging Process
   - Develop the Logging process.
   - Handle execution logs and error logs.
   - Maintain records of simulator activities.

 Action Items

- Gautam: `Main.java`, integration of all three processes, IPC coordination, system testing and benchmarking.
- Reeva: Core process implementation using CPU, Registers, Memory, Stack, Queue, Instruction and Execution Result classes.
- Yuktha: `SimulatorUI.java` and UI process implementation.
- Sanvi: Logging process implementation and execution/error logging.
