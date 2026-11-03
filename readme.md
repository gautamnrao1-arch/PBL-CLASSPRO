<b><h1>PIC16F72 Virtual Simulator</h1></b> <br>

<b>PROJECT TITLE</b> <br>

Design and Develop virtual simulator of MICROCONTROLLER [ PIC16F72 ]<br>

<b>PROBLEM STATEMENT<b>

We have to understand the internal working of microcontroller PIC16F72. Because initially it would be difficult for the student to work with actual hardware component.<br> So to design VIRTUAL SIMULATOR [ SOFTWARE ] that can replicate the "basic working of PIC16F72" and can allow the students to see N learn teh internal opeartions.

<b>PROJECT OBJECTIVE<b><br>

1.Simulate virtually the working of PIC16F72 on laptop.<br>
2.Have to execute assembly instructions.<br>
3.Have to visualize CPU, register, memory(stack), GPIO, timer and interrupts.<br>
4.To have a user friendly interface to test and learn [ very imp ] <br>
5.Make it <b>compatible for both OS(operating system), LINUX OR WINDOWS</b><br>

<b>PROBLEM SCOPE<b><br>

<b>1.CPU Simulation</b><br>

<b>ALU Operations:</b> Performs arithmetic and logical operations like addition, subtraction, AND, OR and XOR on 8-bit data.<br> It also updates the required status flags.<br>
<b>Working Register (W):</b> An 8-bit register used to temporarily store data during calculations and instruction execution.<br>
<b>Instruction Decoding and Execution:</b> The simulator identifies the instruction and performs the required operation on the specified data or register.<br>
<b>STATUS Register and Flags:</b> Stores important CPU status information, including Zero, Carry and Digit Carry flags, which change based on operations.<br>
<b>Program Counter (PC):</b> Keeps track of the address of the next instruction to be executed and changes during jumps or calls.<br>

<b>2.MEMORY SIMULATION</b><br>

<b>Program Memory:</b> This is where the instructions or program of the PIC16F72 are stored. The Program Counter helps the CPU know 
which instruction to execute next.<br>
<b>Data Memory or RAM:</b> Used to store data while the program is running. It contains different registers and memory locations used by the CPU.<br>
<b>SFR (Special Function Registers):</b> These are special registers used to control and monitor different parts of the microcontroller, like CPU 
status, ports, timers, etc.<br>
<b>Stack Mechanism:</b> The stack temporarily stores return addresses when the program calls another function or subroutine.<br>
It helps the processor know where to come back after the subroutine finishes.<br>

* <b>Stack Pointer or Stack Control -</b> PIC16F72 does not have a normal user-accessible Stack Pointer register. It uses a hardware stack to manage return addresses.<br>
* <b>CALL –</b> Used to go to a subroutine. The return address is saved in the stack.<br>
* <b>RETURN –</b> Used to come back from the subroutine to the main program.<br>
* <b>RETLW –</b> Returns from a subroutine and also loads a value into the W register.<br>
* <b>RETFIE –</b> Used to return from an interrupt routine.<br>

<b>3.Peripheral Simulation</b><br>

<b>GPIO/PORT Operations:</b> It is used for the PIC16F72 to interact with external devices by receiving inputs and giving outputs.<br> In the simulator, we need to represent how the PIC’s PORTs work during program execution.<br>
<b>Timer0:</b> It is used to keep track of timing and counting in the PIC16F72. In our simulator, we need to represent its counting process and how it behaves when the timer reaches its limit.<br>
<b>Interrupt Mechanism:</b> It allow the PIC to respond to an event that needs immediate attention while the program is running. The simulator should represent how the processor handles such events. 
  
<b>4.GRAPHICAL USER INTERFACE</b><br>

<b>Memory Visualization:</b> In our simulator  we can show the memory addresses and their values so the user can see how memory <br>changes during execution.<br>
<b>Register Visualization:</b> The simulator can display important register values and show how they change when instructions are executed.<br>
<b>GPIO/PORT Status:</b> It should provide a simple way to see the current state of the PIC16F72's GPIO/PORTs while the program is running. This helps the user understand how the input and output values are changing.<br>
<b>Timer and Interrupt Status:</b> It should show the current state of Timer0 and indicate important timer events. It should also make the interrupt status visible when an interrupt occurs.<br>

<b>5.Status/Flag Information</b><br>

<b>STATUS Register:</b><br>The STATUS register provides information about the current condition and results of operations in the PIC16F72.<br>
<b>Carry Flag (C):</b> It indicates whether a carry occurs during an arithmetic operation.<br>
<b>Digit Carry Flag (DC):</b> It indicates a carry from the lower 4 bits during an arithmetic operation.<br>
<b>Zero Flag (Z):</b> It is set when the result of an operation is zero.<br>
<b>Time-out Flag (TO):</b> It indicates a Watchdog Timer time-out condition.<br>
<b>Power-down Flag (PD):</b> It indicates the power-down or Sleep condition of the processor.<br>
<b>Bank Select Bits (RP0/RP1):</b> These bits are used to select the required memory bank.<br>

<b>6.Interrupt Mechanism</b><br>

<b>Interrupt</b><br>An interrupt allows the PIC16F72 to respond to an important event while the main program is running.<br>

<b>Interrupt Flag:</b> It indicates that an interrupt event has occurred.<br>
<b>Interrupt Enable:</b> It allows a particular interrupt source to generate an interrupt.<br>
<b>Global Interrupt Enable:</b> It enables or disables the interrupt system of the processor.<br>
<b>Interrupt Service Routine (ISR):</b> It is the routine executed by the processor when an interrupt occurs.<br>
<b>Interrupt Handling:</b> The processor handles the interrupt and then returns to the main program.<br>
<b>Simulator Requirement:</b> The simulator should show the status flags and interrupt events during program execution.<br>                                          

<b>SELECTED PROGRAMMIGN LANGUAGE<b><br>

<b>Java</b><br>
Java language is been preferred for this project.<br>

<b>SYSTEM DESIGN/ARCHITECTURE<b><br>

               --------------------      -------------------
               | UI Process       |      |                 |
        ------>| SimulatorUI.java | ---> |   IPCManager    | 
        |      |                  |      |                 |
        |      --------------------      ------------------- 
        |                                       |
        |                                       |
        |                                       ▼ 
        |                               --------------------
        |                               |                  |
        |                               |    IPCFactory    |
        |                               |                  |
        |                               --------------------
        |                                        |       
        |                                        |  
        |                                        ▼ 
        |               ----------------------------------------------------------------
        |               |                                                              |
        |               ▼                                                              ▼ 
        |       ----------------------                                         ------------------
        |       |                    |                                         |                |
        |       | Windows Named Pipe |                                         | POSIX FIFO     |
        |       | Win32 API          |                                         | Linux          |
        |       |                    |                                         |                |
        |       ----------------------                                         ------------------
        |                |                                                            |
        |                ▼                                                            ▼ 
        |               ---------------------------------------------------------------
        |                                           |
        |                                           |
        |                              --------------------------------------------
        |                              |         CORE                             |
        |                              |         CoreProcess                      |
        |                              |                                          |
        |                              --------------------------------------------
        |                               |    ▲                                  |
        |                               |    |                                  |
        |                               |    |                                  |
        |                               |    |                                  |
        |                               |    |                                  |
        |                               ▼    |                                  ▼ 
        |                       -------------------                     --------------------
        |                       | CPU / Memory /  |                     | Logger Process   |
        <---------------------- | Stack / Queue   |                     | LoggerProcess    |
                                |                 |                     |                  |
                                -------------------                     --------------------
                                                                                |
                                                                                ▼ 
                                                                        -----------------
                                                                        | Log Files     | 
                                                                        |               |
                                                                        -----------------  
                                        
<br>
<b>WEEK 4 IPC MECHANISM EXPLAINATION[why we have choosen this method]</b><br>
<b>IPC Components and Responsibilities</b>

| File Name | Responsibility | Description |
|---|---|---|
| `IPCMessage.java` | Message Contract | Defines the structure of an IPC message and how its data is represented. |
| `IPCConstants.java` | Shared Constants | Stores common names, message types, commands, and other IPC-related constants, including pipe or FIFO identifiers where applicable. |
| `IPCManager.java` | Communication Interface | Defines the common interface for establishing connections, sending and receiving messages, and closing IPC connections. |
| `IPCFactory.java` | Backend Selection | Selects the appropriate Windows or Linux IPC implementation based on the operating system or configuration. |
| `WindowsNamedPipeIPC.java` | Windows Transport | Implements interprocess communication using Windows Named Pipes to send and receive data between processes. |
| `LinuxFifoIPC.java` | Linux Transport | Implements interprocess communication using POSIX named pipes (FIFOs) on Linux systems. |
| `ProcessManager.java` | Lifecycle Management | Starts and monitors child processes and coordinates their termination and resource cleanup. |
| `LoggerProcess.java` | Logging Worker | Runs as a separate process, receives log events through IPC, and forwards them to the logging system. |
| `CoreProcess.java` | Execution Worker | Runs as a separate process, handles IPC requests, and connects them to the simulator's execution logic. |



<b>TEAM MEMBERS AND RESPONSIBILITY<b> <br>

Gautam(team Leader): Integration of IPC Mechanism<br>
Reeva : Core components[CPU. Memory, Stack....]<br>
Yuktha: GUI component<br>
Sanvi: Loggers Status[Handling all logs status]<br> 
