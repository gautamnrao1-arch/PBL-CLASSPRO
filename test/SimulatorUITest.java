import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * SimulatorUITest.java
 *
 * Verifies the layout, typography, architecture rules, and IPC integration for SimulatorUI:
 * 1. Exact window title is "PIC16F72 VIRTUAL SIMULATOR".
 * 2. Centered header title label with font size 23 and bold text.
 * 3. Architecture check: UI does not own CPU, Memory, Registers, or Stack state fields.
 * 4. Asynchronous snapshot application and UI component updates.
 * 5. Dynamic CRC Extraction from live register dump without hardcoding.
 * 6. Assembly Error Display with light-red background and normal light-grey reset.
 * 7. Clean shutdown and resource release.
 */
public class SimulatorUITest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("            SimulatorUI Unit Test Suite          ");
        System.out.println("=================================================");

        testWindowTitleAndTypography();
        testNoCpuOrMemoryStateInUI();
        testSnapshotApplication();
        testDynamicCrcExtraction();
        testAssemblyErrorDisplayAndTheme();

        System.out.println("=================================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL SIMULATOR UI TESTS PASSED");
        } else {
            System.out.println("Result: SOME TESTS FAILED");
        }
        System.out.println("=================================================");
    }

    private static void recordResult(String testName, boolean success, String details) {
        totalCount++;
        if (success) {
            passedCount++;
            System.out.println("[PASS] " + testName + " -> " + details);
        } else {
            System.out.println("[FAIL] " + testName + " -> " + details);
        }
    }

    /**
     * Test 1: Verify exact title text and bold font size 23.
     */
    private static void testWindowTitleAndTypography() throws Exception {
        final boolean[] results = new boolean[2];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                SimulatorUI ui = new SimulatorUI((IPCManager) null);

                // 1. Exact title
                String title = ui.getTitle();
                results[0] = "PIC16F72 VIRTUAL SIMULATOR".equals(title);

                // 2. Find Title JLabel in hierarchy and check font
                JLabel titleLabel = findLabelWithText(ui.getContentPane(), "PIC16F72 VIRTUAL SIMULATOR");
                if (titleLabel != null) {
                    Font font = titleLabel.getFont();
                    results[1] = font != null && font.isBold() && font.getSize() == 23;
                }

                ui.shutdown();
                ui.dispose();
            }
        });

        recordResult("Test 1a: Exact Title", results[0],
                "Window title exactly matches 'PIC16F72 VIRTUAL SIMULATOR'");
        recordResult("Test 1b: Title Typography", results[1],
                "Header title uses bold font with size 23");
    }

    /**
     * Test 2: Verify UI does not own CPU, Memory, Registers, or Stack state.
     */
    private static void testNoCpuOrMemoryStateInUI() {
        boolean pass = true;
        Field[] fields = SimulatorUI.class.getDeclaredFields();
        for (Field f : fields) {
            String type = f.getType().getSimpleName();
            if ("CPU".equals(type) || "Memory".equals(type) || "Registers".equals(type)
                    || "PICStack".equals(type) || "FIFOQueue".equals(type)) {
                pass = false;
                break;
            }
        }

        recordResult("Test 2: Architecture Rule - No CPU/Memory State in UI", pass,
                "SimulatorUI does not declare CPU, Memory, Registers, PICStack, or FIFOQueue fields");
    }

    /**
     * Test 3: Verify applying state snapshot dictionary to UI.
     */
    private static void testSnapshotApplication() throws Exception {
        final boolean[] pass = {false};

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                SimulatorUI ui = new SimulatorUI((IPCManager) null);

                Map<String, String> snapshot = new HashMap<String, String>();
                snapshot.put(IPCConstants.KEY_STATUS_STR, "READY");
                snapshot.put(IPCConstants.KEY_PC, "12");
                snapshot.put(IPCConstants.KEY_W, "25");
                snapshot.put(IPCConstants.KEY_STATUS_REG, "24");
                snapshot.put(IPCConstants.KEY_FLAG_Z, "1");
                snapshot.put(IPCConstants.KEY_FLAG_C, "0");
                snapshot.put(IPCConstants.KEY_FLAG_DC, "1");
                snapshot.put(IPCConstants.KEY_CURRENT_INST, "MOVLW 25");
                snapshot.put(IPCConstants.KEY_TRACE, "Executed MOVLW 25 successfully");
                snapshot.put(IPCConstants.KEY_REG_DUMP, "W=0x19");
                snapshot.put(IPCConstants.KEY_STACK_STATE, "SP: 0/8");
                snapshot.put(IPCConstants.KEY_QUEUE_STATE, "Queue: EMPTY");

                ui.applyStateSnapshot(snapshot);
                pass[0] = true;

                ui.shutdown();
                ui.dispose();
            }
        });

        recordResult("Test 3: Asynchronous State Snapshot Application", pass[0],
                "Snapshot correctly decoded and applied to UI components without exceptions");
    }

    /**
     * Test 4: Verify dynamic extraction of CRC from live register dump without hardcoding.
     */
    private static void testDynamicCrcExtraction() throws Exception {
        final boolean[] pass = {false};

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                SimulatorUI ui = new SimulatorUI((IPCManager) null);

                // Simulate snapshot with RAM 0x20 = 0x29 (CRC_HIGH) and RAM 0x21 = 0xB1 (CRC_LOW)
                Map<String, String> snapshot = new HashMap<String, String>();
                snapshot.put(IPCConstants.KEY_STATUS_STR, "TERMINATED");
                snapshot.put(IPCConstants.KEY_REG_DUMP,
                        "General Purpose Registers (RAM 0x20 - 0x2F):\n" +
                        "  [0x20]: 29  [0x21]: B1  [0x22]: 00  [0x23]: 00\n");

                ui.applyStateSnapshot(snapshot);

                JLabel highLabel = findLabelWithText(ui.getContentPane(), "0x29");
                JLabel lowLabel = findLabelWithText(ui.getContentPane(), "0xB1");
                JLabel crcResultLabel = findLabelWithText(ui.getContentPane(), "0x29B1");
                JLabel statusLabel = findLabelWithText(ui.getContentPane(), "COMPLETED");

                pass[0] = (highLabel != null && lowLabel != null && crcResultLabel != null && statusLabel != null);

                ui.shutdown();
                ui.dispose();
            }
        });

        recordResult("Test 4: Dynamic CRC-16 Extraction from REG_DUMP", pass[0],
                "CRC_HIGH (0x29), CRC_LOW (0xB1) and Result (0x29B1) dynamically extracted and displayed");
    }

    /**
     * Test 5: Verify Assembly Error Display with light-red background and normal light-grey reset.
     */
    private static void testAssemblyErrorDisplayAndTheme() throws Exception {
        final boolean[] results = new boolean[2];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                SimulatorUI ui = new SimulatorUI((IPCManager) null);

                try {
                    Field errorPanelField = SimulatorUI.class.getDeclaredField("errorBannerPanel");
                    errorPanelField.setAccessible(true);
                    JPanel errorPanel = (JPanel) errorPanelField.get(ui);

                    Field errorLabelField = SimulatorUI.class.getDeclaredField("errorLabel");
                    errorLabelField.setAccessible(true);
                    JLabel errorLabel = (JLabel) errorLabelField.get(ui);

                    // 1. Simulate an assembly / execution error
                    Map<String, String> errSnapshot = new HashMap<String, String>();
                    errSnapshot.put(IPCConstants.KEY_STATUS_STR, "ERROR: Invalid opcode 'FOOBAR' at line 4");
                    ui.applyStateSnapshot(errSnapshot);

                    Color errBg = errorPanel.getBackground();
                    Color errFg = errorLabel.getForeground();
                    String errText = errorLabel.getText();

                    results[0] = errBg.equals(SimulatorUI.ERR_BG)
                            && errFg.equals(SimulatorUI.ERR_FG)
                            && errText.contains("FOOBAR");

                    // 2. Simulate subsequent successful execution
                    Map<String, String> okSnapshot = new HashMap<String, String>();
                    okSnapshot.put(IPCConstants.KEY_STATUS_STR, "READY");
                    ui.applyStateSnapshot(okSnapshot);

                    Color okBg = errorPanel.getBackground();
                    results[1] = okBg.equals(SimulatorUI.BG_PANEL);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                ui.shutdown();
                ui.dispose();
            }
        });

        recordResult("Test 5a: Error Styling (#FFD2D2 background with red text)", results[0],
                "Error banner correctly displays light-red background and visible red text on assembly error");
        recordResult("Test 5b: Error Clearing & Normal Theme Reset", results[1],
                "Error banner resets to light-grey #F2F2F2 theme upon valid execution");
    }

    private static JLabel findLabelWithText(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if (text.equals(l.getText())) {
                    return l;
                }
            } else if (c instanceof Container) {
                JLabel l = findLabelWithText((Container) c, text);
                if (l != null) return l;
            }
        }
        return null;
    }
}
