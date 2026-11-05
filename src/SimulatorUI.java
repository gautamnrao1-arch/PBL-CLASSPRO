import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

/**
 * SimulatorUI.java
 *
 * Graphical User Interface for the PIC16F72 microcontroller virtual simulator.
 * Built using pure Java Swing with a professional, responsive, light-grey-and-black academic theme.
 *
 * Architecture & Process Separation:
 * - Operates strictly as a presentation client communicating with the external
 *   Core simulator process via the shared IPCManager interface.
 * - Does NOT own, instantiate, or mutate CPU, Memory, Register, or Stack state directly.
 * - Dispatches all IPC requests asynchronously on a background worker thread to ensure
 *   the Swing Event Dispatch Thread (EDT) never freezes or hangs.
 * - All UI component updates are guaranteed to execute on the EDT via SwingUtilities.invokeLater.
 */
public class SimulatorUI extends JFrame {

    private IPCManager ipcManager;
    private final ExecutorService ipcExecutor;

    // Theme Color Palette
    public static final Color BG_MAIN     = new Color(0xE6, 0xE6, 0xE6);
    public static final Color BG_PANEL    = new Color(0xF2, 0xF2, 0xF2);
    public static final Color TEXT_MAIN   = new Color(0x11, 0x11, 0x11);
    public static final Color BORDER_LINE = new Color(0xB0, 0xB0, 0xB0);
    public static final Color BTN_BG      = new Color(0x2C, 0x2C, 0x2C);
    public static final Color BTN_FG      = Color.WHITE;
    public static final Color ERR_BG      = new Color(0xFF, 0xD2, 0xD2);
    public static final Color ERR_FG      = new Color(0xC0, 0x00, 0x00);

    // Standard Typography
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 23);
    private static final Font BOLD_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font MONO_FONT  = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font MONO_BOLD  = new Font(Font.MONOSPACED, Font.BOLD, 13);

    // Assembly Code Input & Controls (Left Workspace)
    private JTextArea programInputArea;
    private JButton loadButton;
    private JButton runButton;
    private JButton stepButton;
    private JButton resetButton;
    private JButton reconnectButton;

    // Assembly CRC Status & Error Banner (Header Area)
    private JLabel crcHighLabel;
    private JLabel crcLowLabel;
    private JLabel crcResultLabel;
    private JLabel statusLabel;
    private JPanel errorBannerPanel;
    private JLabel errorLabel;

    // CPU State, Registers & Flags (Right Workspace)
    private JLabel pcLabel;
    private JLabel currentInstructionLabel;
    private JLabel wRegisterLabel;
    private JLabel statusRegisterLabel;
    private JLabel zeroFlagLabel;
    private JLabel carryFlagLabel;
    private JLabel digitCarryFlagLabel;

    // Memory, Trace & Diagnostics (Right Workspace)
    private JTextArea loadedProgramArea;
    private JTextArea registersDisplayArea;
    private JTextArea stackQueueDisplayArea;
    private JTextArea traceTextArea;

    // Footer Status Bar
    private JLabel connectionStatusLabel;

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
        setPreferredSize(new Dimension(1280, 840));
        setMinimumSize(new Dimension(1020, 680));

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
                            clearAssemblyError();
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
     * Initializes all Swing GUI components with the light-grey-and-black theme.
     */
    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout(8, 8));
        rootPanel.setBackground(BG_MAIN);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // 1. TOP CONTAINER: Window Title + Assembly CRC Status Section
        JPanel topContainer = new JPanel(new BorderLayout(6, 6));
        topContainer.setBackground(BG_MAIN);

        JLabel titleLabel = new JLabel("PIC16F72 VIRTUAL SIMULATOR", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_MAIN);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        topContainer.add(titleLabel, BorderLayout.NORTH);

        topContainer.add(createAssemblyCrcStatusPanel(), BorderLayout.CENTER);
        rootPanel.add(topContainer, BorderLayout.NORTH);

        // 2. MAIN WORKSPACE BODY: Left Assembly Area + Right Diagnostic Workspace
        JPanel bodyPanel = new JPanel(new BorderLayout(10, 10));
        bodyPanel.setBackground(BG_MAIN);
        bodyPanel.add(createLeftAssemblyPanel(), BorderLayout.WEST);
        bodyPanel.add(createRightDiagnosticPanel(), BorderLayout.CENTER);
        rootPanel.add(bodyPanel, BorderLayout.CENTER);

        // 3. BOTTOM FOOTER: Connection status bar
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(BG_MAIN);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        connectionStatusLabel = new JLabel("Initializing IPC connection...", SwingConstants.LEFT);
        connectionStatusLabel.setFont(BOLD_FONT);
        footerPanel.add(connectionStatusLabel, BorderLayout.WEST);

        JLabel architectureLabel = new JLabel("Multiprocess Architecture | UI Client: IPC Named Pipes / FIFOs", SwingConstants.RIGHT);
        architectureLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        architectureLabel.setForeground(TEXT_MAIN);
        footerPanel.add(architectureLabel, BorderLayout.EAST);

        rootPanel.add(footerPanel, BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    /**
     * Creates the Assembly CRC Status & Error section displayed above the main workspace.
     */
    private JPanel createAssemblyCrcStatusPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout(4, 4));
        outerPanel.setBackground(BG_PANEL);
        outerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "Assembly CRC Status",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));

        // Metadata & Live Metrics Grid
        JPanel metricsPanel = new JPanel(new GridLayout(1, 6, 8, 4));
        metricsPanel.setBackground(BG_PANEL);
        metricsPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        metricsPanel.add(createMetricCard("CRC Algorithm", new JLabel("CRC-16 / CCITT-FALSE", SwingConstants.CENTER), false));
        metricsPanel.add(createMetricCard("Polynomial", new JLabel("0x1021", SwingConstants.CENTER), false));

        crcHighLabel = new JLabel("0x--", SwingConstants.CENTER);
        metricsPanel.add(createMetricCard("CRC High (0x20)", crcHighLabel, true));

        crcLowLabel = new JLabel("0x--", SwingConstants.CENTER);
        metricsPanel.add(createMetricCard("CRC Low (0x21)", crcLowLabel, true));

        crcResultLabel = new JLabel("0x----", SwingConstants.CENTER);
        metricsPanel.add(createMetricCard("Final CRC-16", crcResultLabel, true));

        statusLabel = new JLabel("READY", SwingConstants.CENTER);
        metricsPanel.add(createMetricCard("Execution Status", statusLabel, true));

        outerPanel.add(metricsPanel, BorderLayout.CENTER);

        // Assembly Error / Health Banner
        errorBannerPanel = new JPanel(new BorderLayout());
        errorBannerPanel.setBackground(BG_PANEL);
        errorBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        errorLabel = new JLabel("System Ready. No errors detected.");
        errorLabel.setFont(BOLD_FONT);
        errorLabel.setForeground(new Color(0x55, 0x55, 0x55));
        errorBannerPanel.add(errorLabel, BorderLayout.CENTER);

        outerPanel.add(errorBannerPanel, BorderLayout.SOUTH);
        return outerPanel;
    }

    /**
     * Creates a styled metric badge inside the status panel.
     */
    private JPanel createMetricCard(String title, JLabel valLabel, boolean highlight) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        titleLbl.setForeground(new Color(0x55, 0x55, 0x55));

        valLabel.setFont(highlight ? MONO_BOLD : MONO_FONT);
        valLabel.setForeground(TEXT_MAIN);

        card.add(titleLbl);
        card.add(valLabel);
        return card;
    }

    /**
     * Creates the LEFT workspace panel:
     * - Assembly Code Editor (generous area with ample space for multiple instructions)
     * - Simulator Controls (LOAD, RUN, STEP, RESET, RECONNECT) placed at the bottom
     */
    private JPanel createLeftAssemblyPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setPreferredSize(new Dimension(480, 0));
        leftPanel.setBackground(BG_PANEL);
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "Assembly Code Input",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));

        // Program Input Area
        programInputArea = new JTextArea();
        styleTextArea(programInputArea, true);
        programInputArea.setText(loadDefaultAssembly());
        JScrollPane inputScroll = new JScrollPane(programInputArea);
        leftPanel.add(inputScroll, BorderLayout.CENTER);

        // Simulator Controls
        JPanel controlsPanel = new JPanel(new GridLayout(1, 5, 6, 6));
        controlsPanel.setBackground(BG_PANEL);
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 2, 2, 2),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER_LINE, 1),
                        "Simulator Controls",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        BOLD_FONT,
                        TEXT_MAIN)));

        loadButton = new JButton("LOAD");
        runButton = new JButton("RUN");
        stepButton = new JButton("STEP");
        resetButton = new JButton("RESET");
        reconnectButton = new JButton("RECONNECT");

        styleButton(loadButton);
        styleButton(runButton);
        styleButton(stepButton);
        styleButton(resetButton);
        styleButton(reconnectButton);

        controlsPanel.add(loadButton);
        controlsPanel.add(runButton);
        controlsPanel.add(stepButton);
        controlsPanel.add(resetButton);
        controlsPanel.add(reconnectButton);

        leftPanel.add(controlsPanel, BorderLayout.SOUTH);
        return leftPanel;
    }

    /**
     * Creates the RIGHT diagnostic panel arranged in a clean 2x2 responsive grid:
     * - Top-Left: CPU Registers, STATUS flags & PC
     * - Top-Right: Loaded Program Memory
     * - Bottom-Left: File Registers / Data Memory (RAM) & Hardware Stack/Queue
     * - Bottom-Right: Execution Results & Trace Logs
     */
    private JPanel createRightDiagnosticPanel() {
        JPanel rightGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        rightGrid.setBackground(BG_MAIN);

        // 1. Top-Left: CPU State & Status Flags
        JPanel statePanel = new JPanel(new GridBagLayout());
        statePanel.setBackground(BG_PANEL);
        statePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "CPU Registers & STATUS Flags",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Program Counter
        gbc.gridx = 0; gbc.gridy = 0;
        statePanel.add(createLabel("Program Counter (PC):"), gbc);
        gbc.gridx = 1;
        pcLabel = new JLabel("0x0000 (0)");
        pcLabel.setFont(MONO_FONT);
        statePanel.add(pcLabel, gbc);

        // Row 1: Current Instruction
        gbc.gridx = 0; gbc.gridy = 1;
        statePanel.add(createLabel("Current Instruction:"), gbc);
        gbc.gridx = 1;
        currentInstructionLabel = new JLabel("None");
        currentInstructionLabel.setFont(MONO_FONT);
        statePanel.add(currentInstructionLabel, gbc);

        // Row 2: W Register
        gbc.gridx = 0; gbc.gridy = 2;
        statePanel.add(createLabel("Working Reg (W):"), gbc);
        gbc.gridx = 1;
        wRegisterLabel = new JLabel("0x00 (0)");
        wRegisterLabel.setFont(MONO_FONT);
        statePanel.add(wRegisterLabel, gbc);

        // Row 3: STATUS Register
        gbc.gridx = 0; gbc.gridy = 3;
        statePanel.add(createLabel("STATUS Register:"), gbc);
        gbc.gridx = 1;
        statusRegisterLabel = new JLabel("0x00");
        statusRegisterLabel.setFont(MONO_FONT);
        statePanel.add(statusRegisterLabel, gbc);

        // Row 4: Zero Flag (Z)
        gbc.gridx = 0; gbc.gridy = 4;
        statePanel.add(createLabel("Zero Flag (Z):"), gbc);
        gbc.gridx = 1;
        zeroFlagLabel = new JLabel("0");
        zeroFlagLabel.setFont(MONO_BOLD);
        statePanel.add(zeroFlagLabel, gbc);

        // Row 5: Carry Flag (C)
        gbc.gridx = 0; gbc.gridy = 5;
        statePanel.add(createLabel("Carry Flag (C):"), gbc);
        gbc.gridx = 1;
        carryFlagLabel = new JLabel("0");
        carryFlagLabel.setFont(MONO_BOLD);
        statePanel.add(carryFlagLabel, gbc);

        // Row 6: Digit Carry Flag (DC)
        gbc.gridx = 0; gbc.gridy = 6;
        statePanel.add(createLabel("Digit Carry (DC):"), gbc);
        gbc.gridx = 1;
        digitCarryFlagLabel = new JLabel("0");
        digitCarryFlagLabel.setFont(MONO_BOLD);
        statePanel.add(digitCarryFlagLabel, gbc);

        rightGrid.add(statePanel);

        // 2. Top-Right: Loaded Program (Memory)
        JPanel loadedProgPanel = new JPanel(new BorderLayout());
        loadedProgPanel.setBackground(BG_PANEL);
        loadedProgPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "Loaded Program Memory",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));

        loadedProgramArea = new JTextArea();
        styleTextArea(loadedProgramArea, false);
        loadedProgPanel.add(new JScrollPane(loadedProgramArea), BorderLayout.CENTER);
        rightGrid.add(loadedProgPanel);

        // 3. Bottom-Left: File Registers & Stack Tabbed View
        JTabbedPane dataTabPane = new JTabbedPane();
        dataTabPane.setFont(BOLD_FONT);
        dataTabPane.setBackground(BG_PANEL);

        registersDisplayArea = new JTextArea();
        styleTextArea(registersDisplayArea, false);
        dataTabPane.addTab("File Registers / RAM", new JScrollPane(registersDisplayArea));

        stackQueueDisplayArea = new JTextArea();
        styleTextArea(stackQueueDisplayArea, false);
        dataTabPane.addTab("Hardware Stack & FIFO Queue", new JScrollPane(stackQueueDisplayArea));

        JPanel dataMemoryContainer = new JPanel(new BorderLayout());
        dataMemoryContainer.setBackground(BG_PANEL);
        dataMemoryContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "Data Memory & Storage",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));
        dataMemoryContainer.add(dataTabPane, BorderLayout.CENTER);
        rightGrid.add(dataMemoryContainer);

        // 4. Bottom-Right: Execution Results & Trace Logs
        JPanel tracePanel = new JPanel(new BorderLayout());
        tracePanel.setBackground(BG_PANEL);
        tracePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                "Execution Results & Trace Logs",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BOLD_FONT,
                TEXT_MAIN));

        traceTextArea = new JTextArea();
        styleTextArea(traceTextArea, false);
        tracePanel.add(new JScrollPane(traceTextArea), BorderLayout.CENTER);
        rightGrid.add(tracePanel);

        return rightGrid;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(BOLD_FONT);
        lbl.setForeground(TEXT_MAIN);
        return lbl;
    }

    private void styleButton(JButton btn) {
        btn.setBackground(BTN_BG);
        btn.setForeground(BTN_FG);
        btn.setFocusPainted(false);
        btn.setFont(BOLD_FONT);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1A, 0x1A, 0x1A), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        btn.setOpaque(true);
    }

    private void styleTextArea(JTextArea area, boolean editable) {
        area.setFont(MONO_FONT);
        area.setBackground(Color.WHITE);
        area.setForeground(TEXT_MAIN);
        area.setCaretColor(TEXT_MAIN);
        area.setEditable(editable);
    }

    /**
     * Loads the default assembly source: reads crc16.asm if present, or falls back to sample code.
     */
    private static String loadDefaultAssembly() {
        File asmFile = new File("crc16.asm");
        if (asmFile.exists() && asmFile.isFile()) {
            try {
                return Files.readString(asmFile.toPath());
            } catch (Exception ignored) {}
        }
        return "; Sample PIC16F72 Assembly Program\n" +
               "MOVLW 5\n" +
               "MOVWF 20\n" +
               "INCF 20\n" +
               "ADDWF 20\n" +
               "SLEEP\n";
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

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRun();
            }
        });

        stepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStep();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReset();
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
        clearAssemblyError();
        String input = programInputArea.getText();
        if (input == null || input.trim().isEmpty()) {
            showAssemblyError("Program input is empty. Please enter valid assembly instructions.");
            traceTextArea.setText("Program input is empty. Please enter valid assembly instructions.");
            return;
        }

        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createLoad(reqId, input);
        sendIPCCommandAsync(message, "Loading program into Core...");
    }

    /**
     * Sends RUN command to Core through IPC asynchronously.
     */
    private void handleRun() {
        clearAssemblyError();
        statusLabel.setText("RUNNING");
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createExecute(reqId, IPCConstants.CMD_RUN);
        sendIPCCommandAsync(message, "Running program in Core...");
    }

    /**
     * Sends STEP command to Core through IPC asynchronously.
     */
    private void handleStep() {
        clearAssemblyError();
        statusLabel.setText("RUNNING");
        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createExecute(reqId, IPCConstants.CMD_STEP);
        sendIPCCommandAsync(message, "Stepping instruction in Core...");
    }

    /**
     * Sends RESET request to Core through IPC asynchronously.
     */
    private void handleReset() {
        clearAssemblyError();
        crcHighLabel.setText("0x--");
        crcLowLabel.setText("0x--");
        crcResultLabel.setText("0x----");
        statusLabel.setText("READY");

        String reqId = UUID.randomUUID().toString();
        IPCMessage message = IPCMessage.createReset(reqId);
        sendIPCCommandAsync(message, "Resetting simulator in Core...");
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
            statusLabel.setText("RUNNING");
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
            showAssemblyError(payload != null ? payload : "Core execution error");
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
            clearAssemblyError();
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

        // 1. Status String & Assembly Error Handling
        String status = snapshot.get(IPCConstants.KEY_STATUS_STR);
        if (status != null && status.toUpperCase().startsWith("ERROR")) {
            showAssemblyError(status);
        } else {
            if ("TERMINATED".equalsIgnoreCase(status)) {
                statusLabel.setText("COMPLETED");
            } else if ("EXECUTING".equalsIgnoreCase(status)) {
                statusLabel.setText("RUNNING");
            } else if (status != null && !status.isEmpty()) {
                statusLabel.setText(status.toUpperCase());
            } else {
                statusLabel.setText("READY");
            }
            clearAssemblyError();
        }

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

        // 8. File Registers & Data Memory Dump -> Dynamic CRC Extraction
        String regDump = snapshot.get(IPCConstants.KEY_REG_DUMP);
        if (regDump != null) {
            registersDisplayArea.setText(regDump);
            updateCrcFromDump(regDump);
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
     * Extracts CRC_HIGH (RAM 0x20) and CRC_LOW (RAM 0x21) dynamically from the live register dump.
     * Computes the 16-bit CRC result and updates UI labels without hardcoding.
     */
    private void updateCrcFromDump(String regDump) {
        if (regDump == null || regDump.isEmpty()) return;
        try {
            Pattern p = Pattern.compile("\\[0x20\\]:\\s*([0-9A-Fa-f]{2}).*?\\[0x21\\]:\\s*([0-9A-Fa-f]{2})", Pattern.DOTALL);
            Matcher m = p.matcher(regDump);
            if (m.find()) {
                int high = Integer.parseInt(m.group(1), 16);
                int low = Integer.parseInt(m.group(2), 16);
                int fullCrc = ((high & 0xFF) << 8) | (low & 0xFF);
                crcHighLabel.setText(String.format("0x%02X", high));
                crcLowLabel.setText(String.format("0x%02X", low));
                crcResultLabel.setText(String.format("0x%04X", fullCrc));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Displays assembly syntax, unknown label, or execution error in the status section
     * with light-red background and visible red text.
     */
    private void showAssemblyError(String errorDetail) {
        statusLabel.setText("ERROR");
        statusLabel.setForeground(ERR_FG);

        errorBannerPanel.setBackground(ERR_BG);
        errorBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD3, 0x2F, 0x2F), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        errorLabel.setForeground(ERR_FG);
        errorLabel.setText("Assembly / Execution Error: " + errorDetail);
    }

    /**
     * Clears error display and restores normal light-grey theme when a valid execution begins.
     */
    private void clearAssemblyError() {
        statusLabel.setForeground(TEXT_MAIN);

        errorBannerPanel.setBackground(BG_PANEL);
        errorBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LINE, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        errorLabel.setForeground(new Color(0x55, 0x55, 0x55));
        errorLabel.setText("Status: Normal. No assembly or execution errors.");
    }

    /**
     * Displays connection errors clearly on the UI and disables controls.
     */
    private void showConnectionError(String errorDetail) {
        statusLabel.setText("ERROR");
        updateConnectionStatus(false, "Disconnected from Core");
        showAssemblyError("IPC Connection Failure: " + errorDetail);
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
        showAssemblyError("Core Process Terminated: " + detail);
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
        runButton.setEnabled(enabled);
        stepButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
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
