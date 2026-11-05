; ==============================================================================
; PIC16F72 ASSEMBLY IMPLEMENTATION OF CRC-16 (CCITT-FALSE)
; ==============================================================================
; Algorithm Details:
;   - Algorithm:          CRC-16 / CCITT-FALSE (AUTOSAR / IBM-3740)
;   - Generator Poly:     G(D) = D^16 + D^12 + D^5 + 1 (Hex: 0x1021)
;   - Initial Value:      0xFFFF (CRC_HIGH = 0xFF, CRC_LOW = 0xFF)
;   - Input Reflection:   None (MSB-first)
;   - Output Reflection:  None (MSB-first)
;   - Final XOR:          0x0000 (No inversion)
;   - Test Input:         "123456789" (ASCII: 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39)
;   - Expected Result:    0x29B1 -> CRC_HIGH (0x20) = 0x29, CRC_LOW (0x21) = 0xB1
; ==============================================================================

; --- Special Function Registers (SFRs) ---
INDF        EQU 0x00        ; Virtual indirect register (addressed via FSR)
STATUS      EQU 0x03        ; Status Register (Bit 0: Carry 'C', Bit 2: Zero 'Z')
FSR         EQU 0x04        ; File Select Register (data memory pointer for INDF)

; --- General Purpose Registers / Variables (RAM 0x20 - 0x7F) ---
CRC_HIGH    EQU 0x20        ; Output: Most Significant Byte of 16-bit CRC remainder
CRC_LOW     EQU 0x21        ; Output: Least Significant Byte of 16-bit CRC remainder
DATA_BYTE   EQU 0x22        ; Current input byte being processed
BIT_COUNT   EQU 0x23        ; Loop counter for 8 bits per byte
BYTE_COUNT  EQU 0x24        ; Loop counter for input data bytes

; ==============================================================================
; PROGRAM INITIALIZATION
; ==============================================================================

START:
    ; 1. Load Input Data ("123456789") into RAM buffer starting at address 0x30
    MOVLW 0x31              ; ASCII '1'
    MOVWF 0x30
    MOVLW 0x32              ; ASCII '2'
    MOVWF 0x31
    MOVLW 0x33              ; ASCII '3'
    MOVWF 0x32
    MOVLW 0x34              ; ASCII '4'
    MOVWF 0x33
    MOVLW 0x35              ; ASCII '5'
    MOVWF 0x34
    MOVLW 0x36              ; ASCII '6'
    MOVWF 0x35
    MOVLW 0x37              ; ASCII '7'
    MOVWF 0x36
    MOVLW 0x38              ; ASCII '8'
    MOVWF 0x37
    MOVLW 0x39              ; ASCII '9'
    MOVWF 0x38

    ; 2. Initialize 16-bit CRC Remainder to 0xFFFF
    MOVLW 0xFF
    MOVWF CRC_HIGH          ; High byte = 0xFF
    MOVLW 0xFF
    MOVWF CRC_LOW           ; Low byte = 0xFF

    ; 3. Setup Byte Processing Loop (9 bytes starting at RAM 0x30)
    MOVLW 9
    MOVWF BYTE_COUNT        ; Total 9 bytes to process
    MOVLW 0x30
    MOVWF FSR               ; Point FSR to start of input buffer

; ==============================================================================
; OUTER LOOP: Process each byte from input buffer
; ==============================================================================
BYTE_LOOP:
    MOVF INDF, 0            ; Read byte pointed to by FSR into W
    MOVWF DATA_BYTE         ; Store into DATA_BYTE register

    ; In MSB-first polynomial division, XOR incoming byte with CRC high byte
    XORWF CRC_HIGH, 1       ; CRC_HIGH = CRC_HIGH ^ DATA_BYTE

    ; Initialize bit counter for 8 iterations
    MOVLW 8
    MOVWF BIT_COUNT         ; BIT_COUNT = 8

; ==============================================================================
; INNER LOOP: Process 8 bits of the current byte
; ==============================================================================
BIT_LOOP:
    ; Clear Carry flag before shifting 16-bit remainder
    BCF STATUS, 0           ; Carry = 0

    ; Shift CRC_LOW left by 1; old bit 7 shifts into Carry
    RLF CRC_LOW, 1          ; CRC_LOW = (CRC_LOW << 1) | Carry_in; Carry_out = old bit 7

    ; Shift CRC_HIGH left by 1; Carry_in is bit 7 from CRC_LOW, Carry_out is old bit 7 of CRC_HIGH
    RLF CRC_HIGH, 1         ; CRC_HIGH = (CRC_HIGH << 1) | Carry_in; Carry_out = old MSB

    ; Now Carry flag contains the MSB that was shifted out of the 16-bit CRC remainder.
    ; If MSB == 1, we must XOR with the generator polynomial 0x1021.
    ; If MSB == 0, skip the polynomial XOR.
    BTFSS STATUS, 0         ; Test bit 0 of STATUS (Carry). Skip next instruction if Set (Carry == 1)
    GOTO SKIP_POLY          ; Carry was 0: skip polynomial subtraction

    ; Carry was 1: XOR generator polynomial 0x1021 with remainder
    MOVLW 0x10
    XORWF CRC_HIGH, 1       ; CRC_HIGH = CRC_HIGH ^ 0x10
    MOVLW 0x21
    XORWF CRC_LOW, 1        ; CRC_LOW = CRC_LOW ^ 0x21

SKIP_POLY:
    ; Decrement bit counter; skip next GOTO when all 8 bits are processed
    DECFSZ BIT_COUNT, 1
    GOTO BIT_LOOP           ; Process remaining bits

    ; Advance FSR pointer to next byte in data buffer
    INCF FSR, 1

    ; Decrement byte counter; skip next GOTO when all bytes are processed
    DECFSZ BYTE_COUNT, 1
    GOTO BYTE_LOOP          ; Process remaining bytes

; ==============================================================================
; TERMINATION
; ==============================================================================
    ; Final CRC remainder resides in CRC_HIGH (0x20) and CRC_LOW (0x21).
    ; For "123456789": CRC_HIGH = 0x29, CRC_LOW = 0xB1 (0x29B1).
    SLEEP                   ; Terminate simulation
