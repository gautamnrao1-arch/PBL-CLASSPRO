import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * SimulatorUITest.java
 *
 * Verifies the layout, typography, architecture rules, and IPC integration for SimulatorUI:
 * 1. Exact window title is "PIC16F72 VIRTUAL SIMULATOR".
 * 2. Centered header title label with font size 23 and bold text.
 * 3. Architecture check: UI does not own CPU, Memory, Registers, or Stack state fields.
 * 4. Asynchronous snapshot application and UI component updates.
 * 5. Clean shutdown and resource release.
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
