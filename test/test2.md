<<<<<<< HEAD

=======
#Week 2 Instruction Test Results

| Test | Instruction | Expected Result | Actual Result | Status |
|-----|-----|-----|-----|-----|
| TC01 | MOVLW 5 | W = 5 | W = 5 | PASS |
| TC02 | MOVWF 20 (after W = 5) | Register 0x14 = 5 | Register 0x14 = 5 | PASS |
| TC03 | ADDWF 0, false (W = 3, Register = 5) | Register = 8 | Register = 8 | PASS |
| TC04 | SUBWF 0, false (Register = 8, W = 3) | Register = 5 | Register = 5 | PASS |
| TC05 | ANDWF 20,1 | Register 0x14 = 0, Z = 1 | Register 0x14 = 0, Z = 1 | PASS |
| TC06 | INCF 20,1 | Register 0x14 = 1 | Register 0x14 = 1 | PASS |
| TC07 | GOTO 10 | PC = 10 | PC = 10 | PASS |
| TC08 | SLEEP | Processor enters sleep mode | Processor enters sleep mode | PASS |
>>>>>>> 2be60d2 (Add test2.md)
        