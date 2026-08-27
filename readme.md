<b>PIC16F72 Virtual Simulator</b> <br>

<b>PROJECT TITLE</b> <br>

Design and Develop virtual simulator of MICROCONTROLLER [ PIC16F72 ]<br>

<b>PROBLEM STATEMENT<b>

We have to understand the internal working of microcontroller PIC16F72. Because initially it would be difficult for the student to work with actual hardware component.<br> So to design VIRTUAL SIMULATOR [ SOFTWARE ] that can replicate the "basic working of PIC16F72" and can allow the students to see N learn teh internal opeartions.

<b>PROJECT OBJECTIVE<b><br>

1. Simulate virtually the working of PIC16F72 on laptop.<br>
2. Have to execute assembly instructions.<br>
3. Have to visualize CPU, register, memory(stack), GPIO, timer and interrupts.<br>
4. To have a user friendly interface to test and learn [ very imp ] <br>

<b>PROBLEM SCOPE<b><br>

1.CPU Simulation<br>

ALU Operations: Performs arithmetic and logical operations like addition, subtraction, AND, OR and XOR on 8-bit data.<br> It also updates the required status flags.<br>
Working Register (W): An 8-bit register used to temporarily store data during calculations and instruction execution.<br>
Instruction Decoding and Execution: The simulator identifies the instruction and performs the required operation on the specified data or register.<br>
STATUS Register and Flags: Stores important CPU status information, including Zero, Carry and Digit Carry flags, which change based on operations.<br>
Program Counter (PC): Keeps track of the address of the next instruction to be executed and changes during jumps or calls.<br>

2.MEMORY SIMULATION<br>

Program Memory = This is where the instructions or program of the PIC16F72 are stored. The Program Counter helps the CPU know 
which instruction to execute next.<br>
Data Memory or RAM = Used to store data while the program is running. It contains different registers and memory locations used by the CPU.<br>
SFR (Special Function Registers) = These are special registers used to control and monitor different parts of the microcontroller, like CPU 
status, ports, timers, etc.<br>
Stack Mechanism =The stack temporarily stores return addresses when the program calls another function or subroutine.<br>
It helps the processor know where to come back after the subroutine finishes.<br>

* Stack Pointer or  Stack Control = PIC16F72 does not have a normal user-accessible Stack Pointer register.  It uses a hardware stack to manage return addresses.<br>
* CALL – Used to go to a subroutine. The return address is saved in the stack.<br>
* RETURN – Used to come back from the subroutine to the main program.<br>
* RETLW – Returns from a subroutine and also loads a value into the W register.<br>
* RETFIE – Used to return from an interrupt routine.<br>

4.Peripheral Simulation<br>

GPIO/PORT Operations = It is used for the PIC16F72 to interact with external devices by receiving inputs and giving outputs.<br> In the simulator, we need to represent how the PIC’s PORTs work during program execution.<br>
Timer0 = It is used to keep track of timing and counting in the PIC16F72. In our simulator, we need to represent its counting process and how it behaves when the timer reaches its limit.<br>
Interrupt Mechanism = It allow the PIC to respond to an event that needs immediate attention while the program is running. The simulator should represent how the processor handles such events. 
  
5.GRAPHICAL USER INTERFACE<br>

Memory Visualization = In our simulator  we can show the memory addresses and their values so the user can see how memory <br>changes during execution.<br>
Register Visualization = The simulator can display important register values and show how they change when instructions are executed.<br>
GPIO/PORT Status = It should provide a simple way to see the current state of the PIC16F72's GPIO/PORTs while the program is running. This helps the user understand how the input and output values are changing.<br>
Timer and Interrupt Status = It should show the current state of Timer0 and indicate important timer events. It should also make the interrupt status visible when an interrupt occurs.
<b>SELECTED PROGRAMMING LANGUAGE<b> <br>

Java<br>
Java language is been preferred for this project.<br>

<b>INITIAL SYSTEM DESIGN/ARCHITECTURE<b><br>

                -----------------      -------------------
                | Graphical     |      |                 |  
                | User          | ---> | Processing Unit | 
                | Interface     |      |                 |      
                -----------------      ------------------- 
                                                |
                                                |
                        ----------------------------------------------------------
                        |                           |                            |
                ----------------            -----------------              --------------------
                |              |            |               |              |                  |
                |   CPU/ALU    |            | Memory model  |              |  Registers and   |               
                |              |            |               |              |           Flags  |      
                ----------------            -----------------              --------------------
                        |                           |                            |
                        ----------------------------------------------------------
                                                    |
                                                    |
                                          --------------------------------
                                          |  Peripherals                 |
                                          |  GPIO | Timer0 | Interrupts  |
                                          |                              |
                                          --------------------------------
                                                    |
                                                    |
                                          --------------------------------
                                          | Simulator Status/            |
                                          |             Visualization    |
                                          |                              |
                                          -------------------------------- 

<b>INITIAL DEVELOPMENT PLAN<b><br>

Phase                |                                     Work<br>
                     |
Phase 1              |                      Study PIC16F72 architecture, registers, memory organization and instruction set<br>
Phase 2              |                      Design the simulator architecture and data structures<br>


<b>TEAM MEMBERS AND RESPONSIBILITY<b> <br>

Gautam(team Leader): CPU/Register organization + instruction category<br>
Reeva : Memory organization + stack mechanism<br>
Yuktha: GPIO + timer<br>
Sanvi: status/flag info + interrupt mechanism<br>
