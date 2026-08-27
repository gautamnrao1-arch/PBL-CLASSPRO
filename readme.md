PIC16F72 Virtual Simulator

PROJECT TITLE
Design and Develop virtual simulator of MICROCONTROLLER [ PIC16F72 ]

PROBLEM STATEMENT
We have to understand the internal working of microcontroller PIC16F72. Because initially it would be difficult for the student to work with actual hardware component. So to design VIRTUAL SIMULATOR [ SOFTWARE ] that can replicate the "basic working of PIC16F72" and can allow the students to see N learn teh internal opeartions.

PROJECT OBJECTIVE
1. Simulate virtually the working of PIC16F72 on laptop.
2. Have to execute assembly instructions.
3. Have to visualize CPU, register, memory(stack), GPIO, timer and interrupts.
4. To have a user friendly interface to test and learn [ very imp ] 

PROBLEM SCOPE:
2.MEMORY SIMULATION:
 Program Memory =This is where the instructions or program of the PIC16F72 are stored. The Program Counter helps the CPU know 
                    which instruction to execute next.
                    
Data Memory or RAM = Used to store data while the program is running. It contains different registers and memory locations used by the CPU.

SFR (Special Function Registers) = These are special registers used to control and monitor different parts of the microcontroller, like CPU 
              status, ports, timers, etc.
              
Stack Mechanism =The stack temporarily stores return addresses when the program calls another function or subroutine.
                  It helps the processor know where to come back after the subroutine finishes.
* Stack Pointer or  Stack Control = PIC16F72 does not have a normal user-accessible Stack Pointer register.  It uses a hardware stack to manage return addresses.
* CALL – Used to go to a subroutine. The return address is saved in the stack.
* RETURN – Used to come back from the subroutine to the main program.
* RETLW – Returns from a subroutine and also loads a value into the W register.
* RETFIE – Used to return from an interrupt routine.

5.GRAPHICAL USER INTERFACE:

 Memory Visualization = In our simulator  we can show the memory addresses and their values so the user can see how memory 
               changes during execution.
Register Visualization = The simulator can display important register values and show how they change when instructions are executed.
                  


SELECTED PROGRAMMING LANGUAGE
Java
Java language is been preferred for this project.

INITIAL SYSTEM DESIGN/ARCHITECTURE
INITIAL DEVELOPMENT PLAN
TEAM MEMBERS AND RESPONSIBILITY 
