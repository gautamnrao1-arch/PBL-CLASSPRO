# WEEK 5 – TEST RESULTS

| Sl. No. | Test Performed | Expected Result | Status |
|---|---|---|---|
| 1 | CRC-16 Standard Test (`123456789`) | CRC = `0x29B1` | PASS |
| 2 | Single Character CRC Test (`A`) | CRC = `0xB915` | PASS |
| 3 | Multiple Character CRC Test (`HELLO`) | CRC = `0x49D6` | PASS |
| 4 | XORWF Instruction | Correct XOR operation and Zero flag update | PASS |
| 5 | XORLW Instruction | Correct literal XOR operation | PASS |
| 6 | RLF Instruction | Correct rotate-left operation and Carry flag update | PASS |
| 7 | RRF Instruction | Correct rotate-right operation and Carry flag update | NOT VERIFIED |
| 8 | BSF Instruction | Selected register bit is set | PASS |
| 9 | BCF Instruction | Selected register bit is cleared | PASS |
| 10 | BTFSS Instruction | Correct skip-next-instruction behaviour when the tested bit is set | FAIL |
| 11 | BTFSC Instruction | Correct skip-next-instruction behaviour when the tested bit is clear | FAIL |
| 12 | DECFSZ Instruction | Correct decrement and skip behaviour when the result is zero | FAIL |
| 13 | MOVF Instruction | Correct file-register value transfer and flag update | PASS |
| 14 | CLRF Instruction | File register is cleared to zero | PASS |
| 15 | INDF/FSR Indirect Addressing | Correct indirect memory access | PASS |
| 16 | CoreProcess IPC Integration | Core receives LOAD/RUN messages and returns execution results | PASS |
| 17 | CRC Memory Verification | High byte at `0x20` = `0x29`, low byte at `0x21` = `0xB1` | PASS |
| 18 | Register Dump Verification | Returned register dump contains expected CRC values | PASS |

## Issues Identified During Testing

| Sl. No. | Instruction / Test | Issue Identified | Status |
|---|---|---|---|
| 1 | BTFSS | Program counter skip behaviour requires correction or further debugging | FAIL |
| 2 | BTFSC | Program counter skip behaviour requires correction or further debugging | FAIL |
| 3 | DECFSZ | Program counter skip behaviour requires correction or further debugging | FAIL |
| 4 | RRF | Requires additional verification of rotation and Carry flag behaviour | NOT VERIFIED |

## Test Summary

Week 5 testing focused on CRC-16/CCITT-FALSE implementation, instruction-set functionality, indirect memory addressing, and CoreProcess IPC integration.

The CRC implementation was tested using three input cases, with the expected CRC values:

- `123456789` → `0x29B1`
- `A` → `0xB915`
- `HELLO` → `0x49D6`

The testing also identified issues involving the skip behaviour of BTFSS, BTFSC, and DECFSZ instructions, highlighting areas requiring further debugging and correction.