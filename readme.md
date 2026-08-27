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
Phase                                                       Work<br>
Phase 1                                     Study PIC16F72 architecture, registers, memory organization and instruction set<br>
Phase 2                                     Design the simulator architecture and data structures<br>

<b>TEAM MEMBERS AND RESPONSIBILITY<b> <br>


