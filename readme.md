# PIC16F72 Virtual Simulator

## PROJECT TITLE

Design and Development of a Virtual Simulator for the PIC16F72 Microcontroller.

## PROBLEM STATEMENT

Understanding the internal working of a microcontroller such as the PIC16F72 can be challenging for students without access to physical hardware. This project provides a software-based virtual simulator that allows students to write and execute assembly instructions and observe the internal operations through a graphical interface.

## PROJECT OBJECTIVES

1. Simulate the basic working of the PIC16F72 microcontroller on a computer.
2. Execute assembly instructions.
3. Visualize CPU, registers, memory, stack, and execution status.
4. Provide a user-friendly graphical interface for learning and testing.
5. Support compatibility with both Windows and Linux operating systems.

## PROBLEM SCOPE

### 1. CPU Simulation

- Simulates CPU instruction execution and arithmetic and logical operations.
- Supports register operations and status flag updates.
- Maintains the Working Register (W) and Program Counter (PC).
- Supports instructions such as `MOVLW`, `MOVWF`, `ADDWF`, `SUBWF`, `XORWF`, `XORLW`, `RLF`, `RRF`, `BSF`, `BCF`, `BTFSC`, `BTFSS`, `DECFSZ`, `MOVF`, and `CLRF`.

### 2. Memory Simulation

- Simulates program execution using memory and register components.
- Supports direct and indirect memory access through INDF and FSR.
- Includes stack and FIFO queue functionality.
- Displays relevant register and memory values during execution.

### 3. Assembly Program Execution

- Allows users to enter assembly instructions through the simulator interface.
- Supports loading, running, stepping through, and resetting programs.
- Processes assembly instructions through the Core Simulator.
- Supports label and symbol resolution in assembly programs.

### 4. Peripheral and Data Handling

- Provides simulator support for memory, stack, and FIFO queue operations.
- Supports CRC-16/CCITT-FALSE implementation using assembly instructions.
- Uses polynomial `0x1021` and initial CRC value `0xFFFF`.
- Produces the expected CRC value `0x29B1` for the standard input `123456789`.

### 5. Graphical User Interface

- Provides an assembly code editor.
- Includes LOAD, RUN, STEP, RESET, and RECONNECT controls.
- Displays CPU and register information.
- Provides memory visualization.
- Displays execution status, program counter, and simulator logs.

### 6. Inter-Process Communication (IPC) Mechanism

The simulator uses three independent processes:

- UI Process: Handles user interaction and displays simulator information.
- Core Process: Handles assembly processing, instruction execution, and simulator state.
- Logger Process: Receives and manages execution-related log messages.

## SELECTED PROGRAMMING LANGUAGE

### Java

Java is the primary programming language used for this project.

### Technologies Used

| Technology | Purpose |
|---|---|
| Java | Core simulator implementation |
| Java Swing | Graphical User Interface |
| JNA (Java Native Access) | Windows Named Pipe integration |
| Windows Named Pipes | Inter-process communication on Windows |
| POSIX FIFO | Inter-process communication on Linux |
| VS Code | Development environment |

## SYSTEM DESIGN / ARCHITECTURE

```text
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
```

## HOW TO RUN THE SIMULATOR

### Prerequisites

- Java Development Kit (JDK).
- JNA library files available in the `lib` folder.
- Windows or Linux operating system.

### Windows – Compile and Execute

Open the terminal in the project root directory.

Step 1: Compile the Java source files

```powershell
javac -cp "lib/*" -d bin src/*.java
```

Step 2: Run the simulator

```powershell
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" Main
```

### Linux – Compile and Execute

Step 1: Compile the Java source files

```bash
javac -cp "lib/*" -d bin src/*.java
```

Step 2: Run the simulator

```bash
java --enable-native-access=ALL-UNNAMED -cp "bin:lib/*" Main
```

Note: Run these commands from the project root directory. They assume Java source files are directly inside `src`, dependencies are inside `lib`, and `Main` is the entry point.

## TEAM MEMBERS AND RESPONSIBILITIES

| Team Member | Responsibility |
|---|---|
| Gautam (Team Leader) | Integration of IPC Mechanism, CRC build |
| Reeva | Core components (CPU, Memory, Stack, etc.) |
| Yuktha | GUI component |
| Sanvi | Logger implementation and log management |

## FUTURE SCOPE

- Improve live synchronization between the assembly editor and Core process.
- Enhance IPC reconnection and recovery mechanisms.
- Extend support for additional PIC16F72 instructions and peripherals.
- Improve Timer0, GPIO, and interrupt simulation.
- Perform further cross-platform testing and validation.

---

### PIC16F72 Virtual Simulator – A Java-based, cross-platform, multi-process microcontroller simulation project.
