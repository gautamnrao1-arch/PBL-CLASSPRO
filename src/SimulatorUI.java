import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

/**
 * SimulatorUI.java
 *
 * Graphical User Interface for the PIC16F72 microcontroller simulator.
 * Built using pure Java Swing with an academic, clean, and functional design.
 *
 * Week 2 & Week 3 Integration:
 * - Controls: LOAD, RESET, STEP, RUN
 * - CPU State: PC, Current Instruction, W, STATUS, Z, C, DC, SP
 * - MEMORY: Address -> Value
 * - STACK: SP, Stack Status, Stack Contents
 * - FIFO QUEUE: Queue Status, Queue Size, Queue Contents, Front, Rear
 * - EXECUTION TRACE: Pipeline execution trace and state changes
 *
 * Communicates strictly with the central CPU controller.
 */
public class SimulatorUI extends JFrame {

    private final CPU cpu;

    // Input and control components
    private JTextArea programInputArea;
    private JTextArea loadedProgramArea;
    private JButton loadButton;
    private JButton resetButton;
    private JButton stepButton;
    private JButton runButton;

    // CPU state components
    private JLabel statusLabel;
    private JLabel pcLabel;
    private JLabel currentInstructionLabel;
    private JLabel wRegisterLabel;
    private JLabel statusRegisterLabel;
    private JLabel zeroFlagLabel;
    private JLabel carryFlagLabel;
    private JLabel digitCarryFlagLabel;
    private JLabel spLabel;

    // Memory, Stack, Queue, and Trace display components
    private JTextArea memoryDisplayArea;
    private JTextArea stackDisplayArea;
    private JTextArea queueDisplayArea;
    private JTextArea traceTextArea;

    // Fonts for code and labels
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font BOLD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /**
     * Constructs the SimulatorUI.
     *
     * @param cpu The CPU controller instance
     */
    public SimulatorUI(CPU cpu) {
        this.cpu = (cpu != null) ? cpu : new CPU();

        setTitle("PIC16F72 Microcontroller Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1120, 800));
        setMinimumSize(new Dimension(900, 650));

        initComponents();
        setupListeners();

        // Refresh initial state
        updateUIState("READY", "Simulator ready. Load a program to begin.");

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Initializes all Swing GUI components and establishes layout hierarchy.
     */
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("PIC16F72 SIMULATOR");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Split left and right panels
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setDividerLocation(430);
        splitPane.setResizeWeight(0.38);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    /**
     * Creates the left panel containing program input, controls, and loaded program preview.
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));

        // 1. Program Input Area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Program Input", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        programInputArea = new JTextArea();
        programInputArea.setFont(MONO_FONT);
        programInputArea.setText(
            "; Sample PIC16F72 Program with Week 3 Operations\n" +
            "MOVLW 10\n" +
            "MOVWF 20\n" +
            "PUSH 10\n" +
            "PUSH 20\n" +
            "POP W\n" +
            "ENQUEUE 30\n" +
            "ENQUEUE 40\n" +
            "DEQUEUE 21\n" +
            "SLEEP\n"
        );
        JScrollPane inputScroll = new JScrollPane(programInputArea);
        inputScroll.setPreferredSize(new Dimension(390, 220));
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // 2. Control Buttons Panel
        JPanel controlsPanel = new JPanel(new GridLayout(1, 4, 6, 6));
        controlsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Controls", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        loadButton = new JButton("LOAD");
        resetButton = new JButton("RESET");
        stepButton = new JButton("STEP");
        runButton = new JButton("RUN");

        controlsPanel.add(loadButton);
        controlsPanel.add(resetButton);
        controlsPanel.add(stepButton);
        controlsPanel.add(runButton);

        // Top section of left panel
        JPanel topContainer = new JPanel(new BorderLayout(4, 4));
        topContainer.add(inputPanel, BorderLayout.CENTER);
        topContainer.add(controlsPanel, BorderLayout.SOUTH);
        leftPanel.add(topContainer, BorderLayout.NORTH);

        // 3. Loaded Program Display
        JPanel loadedPanel = new JPanel(new BorderLayout());
        loadedPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Loaded Program (Memory)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        loadedProgramArea = new JTextArea();
        loadedProgramArea.setFont(MONO_FONT);
        loadedProgramArea.setEditable(false);
        JScrollPane loadedScroll = new JScrollPane(loadedProgramArea);
        loadedPanel.add(loadedScroll, BorderLayout.CENTER);

        leftPanel.add(loadedPanel, BorderLayout.CENTER);
        return leftPanel;
    }

    /**
     * Creates the right panel displaying CPU state, Memory, Stack, Queue, and Execution Trace.
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));

        // CPU state and status top section
        JPanel topContainer = new JPanel(new BorderLayout(6, 6));

        // Execution Status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Execution Status", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));
        statusLabel = new JLabel("READY");
        statusLabel.setFont(BOLD_FONT);
        statusPanel.add(new JLabel("Status: "));
        statusPanel.add(statusLabel);
        topContainer.add(statusPanel, BorderLayout.NORTH);

        // CPU State & Flags
        JPanel statePanel = new JPanel(new GridBagLayout());
        statePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "CPU State & Flags", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 8, 2, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: PC and Current Instruction
        gbc.gridx = 0; gbc.gridy = 0;
        statePanel.add(new JLabel("PC:"), gbc);
        gbc.gridx = 1;
        pcLabel = new JLabel("0x0000 (0)");
        pcLabel.setFont(MONO_FONT);
        statePanel.add(pcLabel, gbc);

        gbc.gridx = 2;
        statePanel.add(new JLabel("Current Inst:"), gbc);
        gbc.gridx = 3;
        currentInstructionLabel = new JLabel("None");
        currentInstructionLabel.setFont(MONO_FONT);
        statePanel.add(currentInstructionLabel, gbc);

        // Row 1: W Register and STATUS Register
        gbc.gridx = 0; gbc.gridy = 1;
        statePanel.add(new JLabel("W Register:"), gbc);
        gbc.gridx = 1;
        wRegisterLabel = new JLabel("0x00 (0)");
        wRegisterLabel.setFont(MONO_FONT);
        statePanel.add(wRegisterLabel, gbc);

        gbc.gridx = 2;
        statePanel.add(new JLabel("STATUS:"), gbc);
        gbc.gridx = 3;
        statusRegisterLabel = new JLabel("0x00");
        statusRegisterLabel.setFont(MONO_FONT);
        statePanel.add(statusRegisterLabel, gbc);

        // Row 2: Flags (Z, C)
        gbc.gridx = 0; gbc.gridy = 2;
        statePanel.add(new JLabel("Zero (Z):"), gbc);
        gbc.gridx = 1;
        zeroFlagLabel = new JLabel("0");
        zeroFlagLabel.setFont(MONO_FONT);
        statePanel.add(zeroFlagLabel, gbc);

        gbc.gridx = 2;
        statePanel.add(new JLabel("Carry (C):"), gbc);
        gbc.gridx = 3;
        carryFlagLabel = new JLabel("0");
        carryFlagLabel.setFont(MONO_FONT);
        statePanel.add(carryFlagLabel, gbc);

        // Row 3: Flags (DC) and Stack Pointer (SP)
        gbc.gridx = 0; gbc.gridy = 3;
        statePanel.add(new JLabel("Digit Carry (DC):"), gbc);
        gbc.gridx = 1;
        digitCarryFlagLabel = new JLabel("0");
        digitCarryFlagLabel.setFont(MONO_FONT);
        statePanel.add(digitCarryFlagLabel, gbc);

        gbc.gridx = 2;
        statePanel.add(new JLabel("Stack Pointer (SP):"), gbc);
        gbc.gridx = 3;
        spLabel = new JLabel("0");
        spLabel.setFont(MONO_FONT);
        statePanel.add(spLabel, gbc);

        topContainer.add(statePanel, BorderLayout.CENTER);
        rightPanel.add(topContainer, BorderLayout.NORTH);

        // Center Container: Memory, Stack, Queue, and Execution Trace
        JPanel centerContainer = new JPanel(new GridLayout(4, 1, 4, 4));

        // 1. Memory Panel (Address -> Value)
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "MEMORY (Data RAM)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));
        memoryDisplayArea = new JTextArea();
        memoryDisplayArea.setFont(MONO_FONT);
        memoryDisplayArea.setEditable(false);
        JScrollPane memoryScroll = new JScrollPane(memoryDisplayArea);
        memoryPanel.add(memoryScroll, BorderLayout.CENTER);
        centerContainer.add(memoryPanel);

        // 2. Stack Panel (SP, Status, Contents)
        JPanel stackPanel = new JPanel(new BorderLayout());
        stackPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "STACK (PICStack - 8 Levels)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));
        stackDisplayArea = new JTextArea();
        stackDisplayArea.setFont(MONO_FONT);
        stackDisplayArea.setEditable(false);
        JScrollPane stackScroll = new JScrollPane(stackDisplayArea);
        stackPanel.add(stackScroll, BorderLayout.CENTER);
        centerContainer.add(stackPanel);

        // 3. FIFO Queue Panel (Status, Size, Front, Rear, Contents)
        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "FIFO QUEUE", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));
        queueDisplayArea = new JTextArea();
        queueDisplayArea.setFont(MONO_FONT);
        queueDisplayArea.setEditable(false);
        JScrollPane queueScroll = new JScrollPane(queueDisplayArea);
        queuePanel.add(queueScroll, BorderLayout.CENTER);
        centerContainer.add(queuePanel);

        // 4. Execution Trace Panel
        JPanel tracePanel = new JPanel(new BorderLayout());
        tracePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "EXECUTION TRACE", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));
        traceTextArea = new JTextArea();
        traceTextArea.setFont(MONO_FONT);
        traceTextArea.setEditable(false);
        JScrollPane traceScroll = new JScrollPane(traceTextArea);
        tracePanel.add(traceScroll, BorderLayout.CENTER);
        centerContainer.add(tracePanel);

        rightPanel.add(centerContainer, BorderLayout.CENTER);
        return rightPanel;
    }

    /**
     * Connects event handlers to controls.
     */
    private void setupListeners() {
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLoad();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReset();
            }
        });

        stepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStep();
            }
        });

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRun();
            }
        });
    }

    /**
     * Action performed when LOAD button is clicked.
     * Reads text, parses instructions, loads program into CPU Memory, and refreshes UI.
     */
    private void handleLoad() {
        String input = programInputArea.getText();
        ArrayList<Instruction> program = parseProgram(input);

        if (program.isEmpty()) {
            updateUIState("ERROR", "No valid instructions found in input. Please enter valid assembly lines.");
            return;
        }

        cpu.loadProgram(program);
        updateUIState("PROGRAM LOADED", String.format("Successfully loaded %d instruction(s) into memory.", program.size()));
    }

    /**
     * Action performed when RESET button is clicked.
     */
    private void handleReset() {
        cpu.reset();
        updateUIState("READY", "Simulator reset: registers, memory, stack, and queue cleared.");
    }

    /**
     * Action performed when STEP button is clicked.
     * Executes one instruction and reports pipeline status and changes.
     */
    private void handleStep() {
        ExecutionResult result = cpu.step();

        String status;
        if (!result.isSuccess()) {
            status = "ERROR: " + result.getErrorMessage();
        } else if (result.isTerminated() || cpu.isTerminated()) {
            status = "TERMINATED";
        } else {
            status = "EXECUTING";
        }

        updateUIState(status, result.getTrace());
    }

    /**
     * Action performed when RUN button is clicked.
     * Continuously steps until program termination or error.
     */
    private void handleRun() {
        statusLabel.setText("EXECUTING");
        cpu.run();

        String status = cpu.isTerminated() ? "TERMINATED" : "READY";
        StringBuilder summary = new StringBuilder();
        summary.append("Program run completed.").append(System.lineSeparator());
        summary.append("Terminated: ").append(cpu.isTerminated()).append(System.lineSeparator());
        summary.append(String.format("Final PC: 0x%04X (%d)%n", cpu.getRegisters().getPC(), cpu.getRegisters().getPC()));
        summary.append(String.format("Final W:  0x%02X (%d)%n", cpu.getW(), cpu.getW()));
        summary.append(String.format("Final STATUS: 0x%02X [%s]%n", cpu.getRegisters().getStatus(), cpu.getRegisters().getFlagState()));
        summary.append(String.format("Final SP: %d%n", cpu.getRegisters().getSP()));
        if (cpu.getStack() != null) {
            summary.append(String.format("Stack: %s (SP=%d)%n", cpu.getStack().getStatus(), cpu.getStack().getStackPointer()));
        }
        if (cpu.getQueue() != null) {
            summary.append(String.format("Queue: %s (Size=%d)%n", cpu.getQueue().getStatus(), cpu.getQueue().getSize()));
        }

        updateUIState(status, summary.toString());
    }

    /**
     * Refreshes all visual components with the latest CPU, Registers, Memory, Stack, and Queue state.
     *
     * @param status The current execution status string
     * @param traceText Text to display in the execution trace box
     */
    private void updateUIState(String status, String traceText) {
        statusLabel.setText(status != null ? status : "READY");

        Registers reg = cpu.getRegisters();
        int pc = reg.getPC();
        pcLabel.setText(String.format("0x%04X (%d)", pc, pc));

        Instruction currentInst = cpu.getCurrentInstruction();
        currentInstructionLabel.setText(currentInst != null ? currentInst.toString() : "None");

        wRegisterLabel.setText(String.format("0x%02X (%d)", cpu.getW(), cpu.getW()));
        statusRegisterLabel.setText(String.format("0x%02X", reg.getStatus()));

        zeroFlagLabel.setText(String.valueOf(reg.getZeroFlag()));
        carryFlagLabel.setText(String.valueOf(reg.getCarryFlag()));
        digitCarryFlagLabel.setText(String.valueOf(reg.getDigitCarryFlag()));
        spLabel.setText(String.valueOf(reg.getSP()));

        // Show loaded program and current position
        ArrayList<Instruction> program = cpu.getMemory().getProgram();
        StringBuilder progText = new StringBuilder();
        for (int i = 0; i < program.size(); i++) {
            Instruction inst = program.get(i);
            String pointer = (i == pc && !cpu.isTerminated()) ? "--> " : "    ";
            progText.append(String.format("%s%2d: %s%n", pointer, i, inst.getRawInstruction()));
        }
        if (program.isEmpty()) {
            progText.append("(No program loaded)");
        }
        loadedProgramArea.setText(progText.toString());

        // 1. MEMORY: Show relevant memory contents (Address -> Value)
        StringBuilder memText = new StringBuilder();
        memText.append("Special Function Registers / Base RAM:").append(System.lineSeparator());
        for (int addr = 0; addr < 16; addr++) {
            int val = cpu.getMemory().read(addr);
            memText.append(String.format("0x%02X -> 0x%02X  ", addr, val));
            if ((addr + 1) % 4 == 0 && addr < 15) {
                memText.append(System.lineSeparator());
            }
        }
        // Show any non-zero RAM locations from 0x10 to 0xFF
        boolean extra = false;
        for (int addr = 16; addr < Memory.DATA_MEMORY_SIZE; addr++) {
            int val = cpu.getMemory().read(addr);
            if (val != 0) {
                if (!extra) {
                    memText.append(System.lineSeparator()).append("User RAM Locations:").append(System.lineSeparator());
                    extra = true;
                }
                memText.append(String.format("0x%02X -> 0x%02X (%d)  ", addr, val, val));
            }
        }
        memoryDisplayArea.setText(memText.toString().trim());

        // 2. STACK: Show Stack Pointer (SP), Stack Status, Stack Contents
        PICStack stack = cpu.getStack();
        StringBuilder stackText = new StringBuilder();
        if (stack != null) {
            stackText.append(String.format("Stack Pointer (SP): %d / %d | Status: %s%n",
                    stack.getStackPointer(), stack.getMaxSize(), stack.getStatus()));
            stackText.append("Contents: ");
            int[] contents = stack.getStackContents();
            if (contents.length == 0) {
                stackText.append("[Empty]");
            } else {
                stackText.append("[");
                for (int i = 0; i < contents.length; i++) {
                    stackText.append(String.format("0x%04X (%d)", contents[i], contents[i]));
                    if (i < contents.length - 1) {
                        stackText.append(", ");
                    }
                }
                stackText.append("]");
            }
        } else {
            stackText.append("(Stack not initialized)");
        }
        stackDisplayArea.setText(stackText.toString().trim());

        // 3. QUEUE: Show Queue Status, Queue Size, Queue Contents, Front, Rear
        FIFOQueue queue = cpu.getQueue();
        StringBuilder qText = new StringBuilder();
        if (queue != null) {
            qText.append(String.format("Status: %s | Size: %d/%d | Front: %d | Rear: %d%n",
                    queue.getStatus(), queue.getSize(), queue.getMaxSize(),
                    queue.getFront(), queue.getRear()));
            qText.append("Contents: ");
            int[] qContents = queue.getQueueContents();
            if (qContents.length == 0) {
                qText.append("[Empty]");
            } else {
                qText.append("[");
                for (int i = 0; i < qContents.length; i++) {
                    qText.append(String.format("0x%02X (%d)", qContents[i], qContents[i]));
                    if (i < qContents.length - 1) {
                        qText.append(", ");
                    }
                }
                qText.append("]");
            }
        } else {
            qText.append("(Queue not initialized)");
        }
        queueDisplayArea.setText(qText.toString().trim());

        // 4. EXECUTION TRACE
        if (traceText != null && !traceText.isEmpty()) {
            traceTextArea.setText(traceText);
        }
    }

    /**
     * Parses multiline program text into an ArrayList of Instruction objects.
     * Supports commas or whitespace as operand delimiters and strips comments.
     * Recognizes Week 2 and Week 3 instructions:
     * MOVLW, MOVWF, ADDWF, SUBWF, ANDWF, INCF, GOTO, SLEEP, PUSH, POP, ENQUEUE, DEQUEUE.
     *
     * @param text Raw multiline assembly source
     * @return List of constructed Instruction objects
     */
    private ArrayList<Instruction> parseProgram(String text) {
        ArrayList<Instruction> instructions = new ArrayList<Instruction>();
        if (text == null || text.trim().isEmpty()) {
            return instructions;
        }

        String[] lines = text.split("\\r?\\n");
        int address = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Skip empty lines or comment-only lines
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("//")) {
                continue;
            }

            // Strip inline comments
            int commentIndex = line.indexOf(';');
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex).trim();
            }
            commentIndex = line.indexOf("//");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex).trim();
            }

            if (line.isEmpty()) {
                continue;
            }

            // Tokenize opcode and operands (separated by whitespace and/or commas)
            String[] tokens = line.split("[,\\s]+");
            if (tokens.length == 0 || tokens[0].trim().isEmpty()) {
                continue;
            }

            String opcode = tokens[0].trim().toUpperCase();
            String[] operands;
            if (tokens.length > 1) {
                operands = new String[tokens.length - 1];
                System.arraycopy(tokens, 1, operands, 0, tokens.length - 1);
            } else {
                operands = new String[0];
            }

            Instruction inst = new Instruction(opcode, operands, rawLine.trim());
            inst.setAddress(address++);
            instructions.add(inst);
        }

        return instructions;
    }
}
