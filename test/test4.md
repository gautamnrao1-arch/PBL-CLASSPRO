# WEEK 4 – TEST RESULTS

| Sl. No. | Test Performed | Expected Result | Status |
|---|---|---|---|
| 1 | Application launch through Main.java | Application starts successfully | To Verify |
| 2 | Three-process initialization | UI, Core and Logger start as separate processes | To Verify |
| 3 | Process role identification | Each process executes its assigned role | To Verify |
| 4 | IPC channel initialization | Communication channels initialize successfully | To Verify |
| 5 | UI-to-Core communication | UI commands reach the Core process | To Verify |
| 6 | Assembly program loading | Core receives and loads assembly instructions | To Verify |
| 7 | Assembly instruction execution | CPU executes instructions correctly | To Verify |
| 8 | Core-to-UI communication | Execution results are displayed in the UI | To Verify |
| 9 | Logger communication | Logger receives execution messages | To Verify |
| 10 | RESET functionality | Simulator state resets successfully | To Verify |
| 11 | STEP functionality | One instruction executes per step | To Verify |
| 12 | RECONNECT functionality | UI attempts to restore the IPC connection | Partially Verified |
| 13 | Windows Named Pipe implementation | IPC communication works on Windows | To Verify |
| 14 | Linux FIFO implementation | IPC communication works on Linux | To Verify |
| 15 | Live UI-to-Core synchronization | Newly appended instructions reach Core during execution | Pending |
 
 