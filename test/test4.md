| TC11 | Core STATE command | Current Core state is returned | CORE STATE returned successfully | PASS |
| TC12 | Core RESET command | Core resets CPU, Memory, Stack and Queue | RESET SUCCESS | PASS |
| TC13 | Invalid Core command | Error message is returned | ERROR: Unknown command: ABC | PASS |
| TC14 | Core Stack PUSH/POP | LIFO behavior is maintained | 20 popped first | PASS |
| TC15 | Core Queue ENQUEUE/DEQUEUE | FIFO behavior is maintained | 10 dequeued first | PASS |