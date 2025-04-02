import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class CPU {
    private int[] registers;
    private int cycleCount;
    private int instructionCount;
    private boolean isRunning;

    public CPU() {
        registers = new int[8];
        cycleCount = 0;
        instructionCount = 0;
        isRunning = true;
    }

    public void execute(List<String> instructionList) {
        for (String instruction : instructionList) {
            System.out.println("\nExecuting: " + instruction);
            executeInstruction(instruction);
            if (!isRunning) {
                break;
            }
        }
        System.out.println("\nExecution complete.");
    }

    public String encodeInstruction(String instruction) {
        String[] parts = instruction.split("\\s+");
        String opcode = parts[0].toLowerCase();

        int opcodeValue = 0;
        if (opcode.equals("mov")) {
            opcodeValue = 1;
        } else if (opcode.equals("add")) {
            opcodeValue = 2;
        } else if (opcode.equals("sub")) {
            opcodeValue = 3;
        } else if (opcode.equals("mul")) {
            opcodeValue = 4;
        } else if (opcode.equals("div")) {
            opcodeValue = 5;
        } else if (opcode.equals("end")) {
            opcodeValue = 0;
        }

        int encoding = 0;
        if (opcode.equals("end")) {
            encoding = opcodeValue << 28;
        } else {
            int destinationRegister = getRegisterNumber(parts[1]);
            encoding |= (opcodeValue & 0xF) << 28;
            encoding |= (destinationRegister & 0x7) << 25;
            int mode = 0;

            if (parts.length >= 3) {
                if (!parts[2].toLowerCase().startsWith("r")) {
                    mode = 1;
                    int immediateValue = Integer.parseInt(parts[2]);
                    encoding |= (mode & 0x1) << 24;
                    encoding |= (immediateValue & 0xFFFF) << 8;
                } else {
                    int sourceRegister = getRegisterNumber(parts[2]);
                    encoding |= (mode & 0x1) << 24;
                    encoding |= (sourceRegister & 0x7) << 21;
                }
            }
        }

        String binaryString = String.format("%32s", Integer.toBinaryString(encoding)).replace(' ', '0');
        return "[" + (binaryString) + "]";
    }

    public void executeInstruction(String instruction) {
        System.out.println("    Decoded: " + instruction);
        String encodedInstruction = encodeInstruction(instruction);
        System.out.println("    Encoded: " + encodedInstruction);

        String[] parts = instruction.split("\\s+");
        String opcode = parts[0].toLowerCase();

        if (opcode.equals("end")) {
            cycleCount++;
            instructionCount++;
            isRunning = false;
            return;
        }

        int destinationRegister = getRegisterNumber(parts[1]);

        int operandValue = 0;
        if (parts.length >= 3) {
            if (parts[2].toLowerCase().startsWith("r")) {
                int sourceRegister = getRegisterNumber(parts[2]);
                operandValue = registers[sourceRegister];
            } else {
                operandValue = Integer.parseInt(parts[2]);
            }
        }

        if (opcode.equals("mov")) {
            registers[destinationRegister] = operandValue;
            cycleCount++;
        } else if (opcode.equals("add")) {
            registers[destinationRegister] = registers[destinationRegister] + operandValue;
            cycleCount++;
        } else if (opcode.equals("sub")) {
            registers[destinationRegister] = registers[destinationRegister] - operandValue;
            cycleCount++;
        } else if (opcode.equals("mul")) {
            long product = (long) registers[destinationRegister] * operandValue;
            registers[destinationRegister] = (int) product;
            registers[7] = (int) (product >> 32);
            cycleCount += 3;
        } else if (opcode.equals("div")) {
            int dividend = registers[destinationRegister];
            registers[destinationRegister] = dividend / operandValue;
            registers[7] = dividend % operandValue;
            cycleCount += 4;
        }
        instructionCount++;
    }

    private int getRegisterNumber(String register) {
        register = register.replace(",", "").replace(":", "");
        return Integer.parseInt(register.substring(1));
    }

    public void printState() {
        System.out.println("\nFinal Register Values:");
        for (int i = 0; i < registers.length; i++) {
            System.out.println("r" + i + " = " + registers[i] + " (" + intTo32BitBinary(registers[i]) + ")");
        }
        System.out.println("Total cycles: " + cycleCount);
        System.out.println("Instructions executed: " + instructionCount);
        if (instructionCount > 0) {
            System.out.println("CPI (cycles per instruction): " + ((double) cycleCount / instructionCount));
        }
    }

    private String intTo32BitBinary(int value) {
        String binary = Integer.toBinaryString(value);
        return String.format("%32s", binary).replace(' ', '0');
    }
}

public class CPUSimulator {
    public static void main(String[] args) {
        CPU cpu = new CPU();
        Scanner scanner = new Scanner(System.in);
        List<String> instructionList = new ArrayList<>();

        System.out.println("Enter instructions (type 'end' to finish):");

        while (true) {
            System.out.print("Instruction: ");
            String instruction = scanner.nextLine().trim();

            if (instruction.isEmpty()) {
                continue;
            }

            instructionList.add(instruction);
            if (instruction.toLowerCase().startsWith("end")) {
                break;
            }
        }

        cpu.execute(instructionList);
        System.out.println("\n=== Final CPU State ===");
        cpu.printState();
        scanner.close();
    }
}

/*
 * Test Cases
 * Test Case 1: Basic Register Operations
 * 
 * mov r0, 10
 * mov r1, 20
 * add r0, r1
 * end
 * 
 * Expected results:
 * - r0 = 30
 * - r1 = 20
 * - Total cycles: 4
 * - Instructions executed: 4
 * - CPI: 1.0
 * 
 * Test Case 2: Immediate Values
 * 
 * mov r0, 5
 * add r0, 7
 * sub r0, 2
 * end
 * 
 * Expected results:
 * - r0 = 10
 * - Total cycles: 4
 * - Instructions executed: 4
 * - CPI: 1.0
 * 
 * Test Case 3: Multiplication
 * 
 * mov r0, 10
 * mov r1, 5
 * mul r0, r1
 * end
 * 
 * 
 * results:
 * - r0 = 50
 * - r1 = 5
 * - r7 = 0 (high bits of multiplication)
 * - Total cycles: 6
 * - Instructions executed: 4
 * - CPI: 1.5
 * 
 * Test Case 4: Division with Remainder
 * 
 * mov r0, 17
 * mov r1, 5
 * div r0, r1
 * end
 * 
 * Expected results:
 * - r0 = 3 (quotient)
 * - r1 = 5
 * - r7 = 2 (remainder)
 * - Total cycles: 7
 * - Instructions executed: 4
 * - CPI: 1.75
 * 
 * 
 * Test Case 5: Multiple Operations
 * 
 * mov r0, 100
 * mov r1, 50
 * mov r2, 25
 * add r0, r1
 * sub r0, r2
 * mul r2, 4
 * div r1, 10
 * end
 * 
 * results:
 * - r0 = 125
 * - r1 = 5
 * - r2 = 100
 * - r7 = 0
 * - Total cycles: 13
 * - Instructions executed: 8
 * - CPI: 1.625
 */
