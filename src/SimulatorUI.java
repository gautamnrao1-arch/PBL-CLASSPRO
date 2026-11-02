import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

/**
 * SimulatorUI.java
 *
 * Graphical User Interface for the PIC16F72 microcontroller simulator.
 * Built using pure Java Swing with an academic, clean, and responsive design.
 *
 * Architecture & Process Separation:
 * - Operates strictly as a presentation client communicating with the external
 *   Core simulator process via the shared IPCManager interface.
 * - Does NOT own, instantiate, or mutate CPU, Memory, Register, or Stack state directly.
 * - Dispatches all IPC requests asynchronously on a background worker thread to ensure
 *   the Swing Event Dispatch Thread (EDT) never freezes or hangs.
 * - All UI component updates are guaranteed to execute on the EDT via {@link SwingUtilities#invokeLater}.
 */
public class SimulatorUI extends JFrame {

    private IPCManager ipcManager;
    private final ExecutorService ipcExecutor;

    // Central instruction / input area components
    private JTextArea programInputArea;
    private JButton loadButton;
    private JButton resetButton;
    private JButton stepButton;
    private JButton runButton;
    private JButton reconnectButton;

    // Execution trace and result display
    private JTextArea traceTextArea;

    // Surrounding status & CPU state display components
    private JLabel statusLabel;
    private JLabel pcLabel;
    private JLabel currentInstructionLabel;
    private JLabel wRegisterLabel;
    private JLabel statusRegisterLabel;
    private JLabel zeroFlagLabel;
    private JLabel carryFlagLabel;
    private JLabel digitCarryFlagLabel;

    // Surrounding memory, register, stack & queue display components
    private JTextArea loadedProgramArea;
    private JTextArea registersDisplayArea;
    private JTextArea stackQueueDisplayArea;

    // Footer connection status
    private JLabel connectionStatusLabel;

    // Standardized typography
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 23);
    private static final Font BOLD_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font MONO_FONT  = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /**
     * Default constructor: initializes UI and connects to the Core simulator over IPC.
     */
    public SimulatorUI() {
        this((IPCManager) null);
    }

    /**
     * Primary constructor: creates the UI with an existing IPCManager channel.
     *
     * @param ipcManager Connected IPC communication channel to the Core process
     */
    public SimulatorUI(IPCManager ipcManager) {
        this.ipcManager = ipcManager;
        this.ipcExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SimulatorUI-IPC-Worker");
                t.setDaemon(true);
                return t;
            }
        });

        initWindow();
        initInitialState();
    }

    /**
     * Backward-compatible constructor: delegates to IPC communication without
     * creating or storing CPU state inside the UI process.
     *
     * @param cpu Ignored CPU reference retained for compatibility
     */
    public SimulatorUI(CPU cpu) {
        this((IPCManager) null);
    }

    /**
     * Configures window frame properties, window listeners, and layout.
     */
    private void initWindow() {
        setTitle("PIC16F72 VIRTUAL SIMULATOR");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1220, 800));
        setMinimumSize(new Dimension(980, 640));

        initComponents();
        setupListeners();

        // Release IPC channel and worker thread on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Bootstraps the initial simulator state asynchronously over IPC.
     */
    private void initInitialState() {
        if (ipcManager != null && ipcManager.isConnected()) {
            updateConnectionStatus(true, "Connected to Core Simulator [" + ipcManager.getChannelName() + "]");
            requestStateAsync();
        } else {
            connectAsync();
        }
    }

    /**
     * Establishes IPC connection to the Core process asynchronously.
     */
    public void connectAsync() {
        ipcExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateConnectionStatus(false, "Connecting to Core simulator process...");
                            statusLabel.setText("CONNECTING");
                            setControlsEnabled(false);
                        }
                    });

                    if (ipcManager == null) {
                        ipcManager = IPCFactory.createClient(IPCConstants.CHANNEL_UI_CORE);
                    }
                    if (!ipcManager.isConnected()) {
                        ipcManager.connectClient();
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateConnectionStatus(true, "Connected to Core Simulator [" + IPCConstants.CHANNEL_UI_CORE + "]");
                            statusLabel.setText("READY");
                            setControlsEnabled(true);
                        }
                    });

                    // Request initial CPU and memory state snapshot from Core
                    requestStateAsync();

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showConnectionError("Could not connect to Core process on channel '"
                                    + IPCConstants.CHANNEL_UI_CORE + "': " + ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    /**
     * Initializes all Swing GUI components and establishes layout hierarchy.
     */
    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // 1. TOP HEADER: Exact title "PIC16F72 VIRTUAL SIMULATOR", bold, font size 23, centered
        JPanel headerPanel = new JPanel(new BorderLayout(4, 4));
        JLabel titleLabel = new JLabel("PIC16F72 VIRTUAL SIMULATOR", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. MAIN BODY: Central input area surrounded by status, memory, registers, and trace panels
        JPanel bodyPanel = new JPanel(new BorderLayout(10, 10));
        bodyPanel.add(createLeftPanel(), BorderLayout.WEST);
        bodyPanel.add(createCenterPanel(), BorderLayout.CENTER);
        bodyPanel.add(createRightPanel(), BorderLayout.EAST);
        rootPanel.add(bodyPanel, BorderLayout.CENTER);

        // 3. BOTTOM FOOTER: Connection status bar
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        connectionStatusLabel = new JLabel("Initializing IPC connection...", SwingConstants.LEFT);
        connectionStatusLabel.setFont(BOLD_FONT);
        footerPanel.add(connectionStatusLabel, BorderLayout.WEST);

        JLabel architectureLabel = new JLabel("Process: UI Process | IPC Role: Client", SwingConstants.RIGHT);
        architectureLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        footerPanel.add(architectureLabel, BorderLayout.EAST);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    /**
     * Creates the WEST panel containing:
     * - Top: CPU State & Status panel
     * - Bottom: Loaded Program (Memory) panel
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        leftPanel.setPreferredSize(new Dimension(280, 0));

        // CPU State & Flags Panel
        JPanel statePanel = new JPanel(new GridBagLayout());
        statePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "CPU State & Status", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Execution Status
        gbc.gridx = 0; gbc.gridy = 0;
        statePanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        statusLabel = new JLabel("READY");
        statusLabel.setFont(BOLD_FONT);
        statePanel.add(statusLabel, gbc);

        // Row 1: PC
        gbc.gridx = 0; gbc.gridy = 1;
        statePanel.add(new JLabel("PC:"), gbc);
        gbc.gridx = 1;
        pcLabel = new JLabel("0x0000 (0)");
        pcLabel.setFont(MONO_FONT);
        statePanel.add(pcLabel, gbc);

        // Row 2: Current Instruction
        gbc.gridx = 0; gbc.gridy = 2;
        statePanel.add(new JLabel("Current Inst:"), gbc);
        gbc.gridx = 1;
        currentInstructionLabel = new JLabel("None");
        currentInstructionLabel.setFont(MONO_FONT);
        statePanel.add(currentInstructionLabel, gbc);

        // Row 3: W Register
        gbc.gridx = 0; gbc.gridy = 3;
        statePanel.add(new JLabel("W Register:"), gbc);
        gbc.gridx = 1;
        wRegisterLabel = new JLabel("0x00 (0)");
        wRegisterLabel.setFont(MONO_FONT);
        statePanel.add(wRegisterLabel, gbc);

        // Row 4: STATUS Register
        gbc.gridx = 0; gbc.gridy = 4;
        statePanel.add(new JLabel("STATUS Reg:"), gbc);
        gbc.gridx = 1;
        statusRegisterLabel = new JLabel("0x00");
        statusRegisterLabel.setFont(MONO_FONT);
        statePanel.add(statusRegisterLabel, gbc);

        // Row 5: Flags (Z, C, DC)
        gbc.gridx = 0; gbc.gridy = 5;
        statePanel.add(new JLabel("Zero (Z):"), gbc);
        gbc.gridx = 1;
        zeroFlagLabel = new JLabel("0");
        zeroFlagLabel.setFont(MONO_FONT);
        statePanel.add(zeroFlagLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        statePanel.add(new JLabel("Carry (C):"), gbc);
        gbc.gridx = 1;
        carryFlagLabel = new JLabel("0");
        carryFlagLabel.setFont(MONO_FONT);
        statePanel.add(carryFlagLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        statePanel.add(new JLabel("Digit Carry (DC):"), gbc);
        gbc.gridx = 1;
        digitCarryFlagLabel = new JLabel("0");
        digitCarryFlagLabel.setFont(MONO_FONT);
        statePanel.add(digitCarryFlagLabel, gbc);

        leftPanel.add(statePanel);

        // Loaded Program (Memory) Display
        JPanel loadedPanel = new JPanel(new BorderLayout());
        loadedPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Loaded Program (Memory)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        loadedProgramArea = new JTextArea();
        loadedProgramArea.setFont(MONO_FONT);
        loadedProgramArea.setEditable(false);
        loadedPanel.add(new JScrollPane(loadedProgramArea), BorderLayout.CENTER);
        leftPanel.add(loadedPanel);

        return leftPanel;
    }

    /**
     * Creates the central panel containing:
     * - Top: Instruction / Program Input area (centralized)
     * - Middle: Simulator control buttons (LOAD, RESET, STEP, RUN, RECONNECT)
     * - Bottom: Execution result & pipeline trace display
     */
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        // 1. Program Input Area (Centrally Positioned)
        JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Instruction / Program Input Area", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        programInputArea = new JTextArea();
        programInputArea.setFont(MONO_FONT);
        programInputArea.setText(
            "; Sample PIC16F72 Assembly Program\n" +
            "MOVLW 5\n" +
            "MOVWF 20\n" +
            "INCF 20\n" +
            "ADDWF 20\n" +
            "SLEEP\n"
        );
        JScrollPane inputScroll = new JScrollPane(programInputArea);
        inputScroll.setPreferredSize(new Dimension(420, 220));
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // 2. Simulator Controls
        JPanel controlsPanel = new JPanel(new GridLayout(1, 5, 8, 8));
        controlsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Simulator Controls", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        loadButton = new JButton("LOAD");
        resetButton = new JButton("RESET");
        stepButton = new JButton("STEP");
        runButton = new JButton("RUN");
        reconnectButton = new JButton("RECONNECT");

        controlsPanel.add(loadButton);
        controlsPanel.add(resetButton);
        controlsPanel.add(stepButton);
        controlsPanel.add(runButton);
        controlsPanel.add(reconnectButton);
        inputPanel.add(controlsPanel, BorderLayout.SOUTH);

        centerPanel.add(inputPanel, BorderLayout.NORTH);

        // 3. Execution Results & Trace Panel
        JPanel tracePanel = new JPanel(new BorderLayout());
        tracePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Execution Results & Trace", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        traceTextArea = new JTextArea();
        traceTextArea.setFont(MONO_FONT);
        traceTextArea.setEditable(false);
        JScrollPane traceScroll = new JScrollPane(traceTextArea);
        tracePanel.add(traceScroll, BorderLayout.CENTER);

        centerPanel.add(tracePanel, BorderLayout.CENTER);

        return centerPanel;
    }

    /**
     * Creates the EAST panel containing:
     * - Top: File Registers / Data Memory display
     * - Bottom: Hardware Stack & FIFO Queue display
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        rightPanel.setPreferredSize(new Dimension(320, 0));

        // File Registers & Data Memory Display
        JPanel regPanel = new JPanel(new BorderLayout());
        regPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "File Registers / Data Memory", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        registersDisplayArea = new JTextArea();
        registersDisplayArea.setFont(MONO_FONT);
        registersDisplayArea.setEditable(false);
        regPanel.add(new JScrollPane(registersDisplayArea), BorderLayout.CENTER);
        rightPanel.add(regPanel);

        // Hardware Stack & FIFO Queue Display
        JPanel stackQueuePanel = new JPanel(new BorderLayout());
        stackQueuePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Hardware Stack & FIFO Queue", TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        stackQueueDisplayArea = new JTextArea();
        stackQueueDisplayArea.setFont(MONO_FONT);
        stackQueueDisplayArea.setEditable(false);
        stackQueuePanel.add(new JScrollPane(stackQueueDisplayArea), BorderLayout.CENTER);
        rightPanel.add(stackQueuePanel);

        return rightPanel;
    }

    /**
     * Binds action listeners to user controls.
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

        reconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectAsync();
            }
        });
    }

    // =========================================================================
    // Asynchronous IPC Command Handlers
    // =========================================================================

    /**
     * Sends LOAD command with assembly source to Core through IPC asynchronously.
     */
    private void handleLoad() {
        String input = programInputArea.getText();
        if (input == null || input.trim().isEmpty()) {
            statusLabel.setText("ERROR");
            traceTextArea.setText("Program input is empty. Please enter valid assembly instructions.");
            return;
        }

        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createLoad(reqId, input);
        sendIPCCommandAsync(message, "Loading program into Core...");
    }

    /**
     * Sends RESET request to Core through IPC asynchronously.
     */
    private void handleReset() {
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createReset(reqId);
        sendIPCCommandAsync(message, "Resetting simulator in Core...");
    }

    /**
     * Sends STEP command to Core through IPC asynchronously.
     */
    private void handleStep() {
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createExecute(reqId, IPCConstants.CMD_STEP);
        sendIPCCommandAsync(message, "Stepping instruction in Core...");
    }

    /**
     * Sends RUN command to Core through IPC asynchronously.
     */
    private void handleRun() {
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createExecute(reqId, IPCConstants.CMD_RUN);
        sendIPCCommandAsync(message, "Running program in Core...");
    }

    /**
     * Fetches the current simulator state snapshot from Core asynchronously.
     */
    private void requestStateAsync() {
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createState(reqId);
        sendIPCCommandAsync(message, "Querying Core simulator state...");
    }

    /**
     * Core asynchronous message dispatcher:
     * 1. Updates UI to in-progress state and disables controls on EDT.
     * 2. Transmits framed message and waits for response on background worker thread.
     * 3. Validates request ID correlation.
     * 4. Updates Swing visual components strictly on the Event Dispatch Thread.
     * 5. Handles connection breaks and Core termination cleanly.
     *
     * @param request Formatted IPCMessage request
     * @param inProgressStatus Message displayed while waiting for Core
     */
    private void sendIPCCommandAsync(final IPCMessage request, final String inProgressStatus) {
        if (ipcManager == null || !ipcManager.isConnected()) {
            showConnectionError("IPC channel is disconnected. Click 'RECONNECT' to connect to the Core process.");
            return;
        }

        setControlsEnabled(false);
        if (inProgressStatus != null) {
            statusLabel.setText("BUSY");
        }

        ipcExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final String sentReqId = request.getRequestId();
                try {
                    ipcManager.sendMessage(request);
                    final IPCMessage response = ipcManager.receiveMessage();

                    if (response == null) {
                        // Remote Core process disconnected or terminated (clean EOF)
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                showProcessTerminated("Core simulator process closed the connection or terminated (EOF).");
                            }
                        });
                        return;
                    }

                    // Check request ID correlation
                    if (sentReqId != null && !sentReqId.equals(response.getRequestId())) {
                        System.err.println(String.format(
                                "[UI IPC WARNING] Mismatched request ID: sent='%s', received='%s'",
                                sentReqId, response.getRequestId()));
                    }

                    // Dispatch response handling to the Swing Event Dispatch Thread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleCoreResponse(response);
                            setControlsEnabled(true);
                        }
                    });

                } catch (final IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showConnectionError("IPC communication error: " + ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    /**
     * Applies an incoming IPCMessage response from Core onto the UI components.
     * Must be called on the Swing Event Dispatch Thread.
     */
    private void handleCoreResponse(IPCMessage response) {
        String type = response.getType();
        String payload = response.getPayload();

        if (IPCMessage.TYPE_ERROR.equalsIgnoreCase(type)) {
            statusLabel.setText("ERROR");
            traceTextArea.setText("[CORE EXECUTION ERROR]\n" + payload);
            return;
        }

        if (IPCMessage.TYPE_RESULT.equalsIgnoreCase(type)) {
            Map<String, String> snapshot = CoreProcess.decodeDictionary(payload);
            applyStateSnapshot(snapshot);
            return;
        }

        if (IPCMessage.TYPE_ACK.equalsIgnoreCase(type)) {
            statusLabel.setText("ACK");
            if (payload != null && !payload.isEmpty()) {
                traceTextArea.setText(payload);
            }
            return;
        }

        // Generic fallback for other response types
        statusLabel.setText(type);
        if (payload != null && !payload.isEmpty()) {
            traceTextArea.setText(payload);
        }
    }

    /**
     * Updates all UI panels using the decoded dictionary snapshot received from Core.
     * Must be called on the Swing Event Dispatch Thread.
     *
     * @param snapshot Key-value map encoded with standard IPCConstants keys
     */
    public void applyStateSnapshot(Map<String, String> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        // 1. Status String
        String status = snapshot.get(IPCConstants.KEY_STATUS_STR);
        statusLabel.setText(status != null ? status : "READY");

        // 2. Program Counter
        try {
            int pc = Integer.parseInt(snapshot.getOrDefault(IPCConstants.KEY_PC, "0"));
            pcLabel.setText(String.format("0x%04X (%d)", pc, pc));
        } catch (NumberFormatException ignored) {}

        // 3. Current Instruction
        currentInstructionLabel.setText(snapshot.getOrDefault(IPCConstants.KEY_CURRENT_INST, "None"));

        // 4. W Register
        try {
            int w = Integer.parseInt(snapshot.getOrDefault(IPCConstants.KEY_W, "0"));
            wRegisterLabel.setText(String.format("0x%02X (%d)", w, w));
        } catch (NumberFormatException ignored) {}

        // 5. STATUS Register
        try {
            int s = Integer.parseInt(snapshot.getOrDefault(IPCConstants.KEY_STATUS_REG, "0"));
            statusRegisterLabel.setText(String.format("0x%02X", s));
        } catch (NumberFormatException ignored) {}

        // 6. Flags
        zeroFlagLabel.setText(snapshot.getOrDefault(IPCConstants.KEY_FLAG_Z, "0"));
        carryFlagLabel.setText(snapshot.getOrDefault(IPCConstants.KEY_FLAG_C, "0"));
        digitCarryFlagLabel.setText(snapshot.getOrDefault(IPCConstants.KEY_FLAG_DC, "0"));

        // 7. Loaded Program Memory View
        String progView = snapshot.get(IPCConstants.KEY_PROGRAM_VIEW);
        if (progView != null) {
            loadedProgramArea.setText(progView);
        }

        // 8. File Registers & Data Memory Dump
        String regDump = snapshot.get(IPCConstants.KEY_REG_DUMP);
        if (regDump != null) {
            registersDisplayArea.setText(regDump);
        }

        // 9. Hardware Stack & FIFO Queue State
        String stackState = snapshot.get(IPCConstants.KEY_STACK_STATE);
        String queueState = snapshot.get(IPCConstants.KEY_QUEUE_STATE);
        StringBuilder sqText = new StringBuilder();
        if (stackState != null && !stackState.isEmpty()) {
            sqText.append("=== HARDWARE STACK ===").append(System.lineSeparator()).append(stackState);
        }
        if (queueState != null && !queueState.isEmpty()) {
            if (sqText.length() > 0) sqText.append(System.lineSeparator()).append(System.lineSeparator());
            sqText.append("=== FIFO QUEUE ===").append(System.lineSeparator()).append(queueState);
        }
        if (sqText.length() > 0) {
            stackQueueDisplayArea.setText(sqText.toString());
        }

        // 10. Execution Trace
        String trace = snapshot.get(IPCConstants.KEY_TRACE);
        if (trace != null && !trace.isEmpty()) {
            traceTextArea.setText(trace);
        }
    }

    /**
     * Displays connection errors clearly on the UI and disables controls.
     */
    private void showConnectionError(String errorDetail) {
        statusLabel.setText("DISCONNECTED");
        updateConnectionStatus(false, "Disconnected from Core");
        traceTextArea.setText("[IPC CONNECTION FAILURE]\n" + errorDetail
                + "\n\nPlease ensure the Core simulator process is running, then click 'RECONNECT'.");
        setControlsEnabled(false);
    }

    /**
     * Displays Core process termination clearly on the UI and disables controls.
     */
    private void showProcessTerminated(String detail) {
        statusLabel.setText("TERMINATED");
        updateConnectionStatus(false, "Core Process Terminated");
        traceTextArea.setText("[CORE PROCESS TERMINATED]\n" + detail
                + "\n\nThe remote Core simulator process has exited. Click 'RECONNECT' if restarted.");
        setControlsEnabled(false);
    }

    /**
     * Updates the footer connection status label.
     */
    private void updateConnectionStatus(boolean connected, String statusText) {
        connectionStatusLabel.setText(statusText);
        connectionStatusLabel.setForeground(connected ? new Color(0, 130, 0) : Color.RED);
    }

    /**
     * Toggles simulator execution controls enabled state.
     */
    private void setControlsEnabled(boolean enabled) {
        loadButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        stepButton.setEnabled(enabled);
        runButton.setEnabled(enabled);
        reconnectButton.setEnabled(true); // Reconnect is always enabled
    }

    /**
     * Closes IPC connections and releases UI resources during shutdown.
     */
    public synchronized void shutdown() {
        if (ipcManager != null && ipcManager.isConnected()) {
            try {
                ipcManager.sendMessage(IPCMessage.createShutdown("UI window closed by user"));
                ipcManager.close();
            } catch (IOException ignored) {}
        }
        ipcExecutor.shutdownNow();
    }
}
