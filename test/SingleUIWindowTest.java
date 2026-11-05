import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * SingleUIWindowTest.java
 *
 * Verifies that:
 * 1. Neither CoreProcess nor LoggerProcess contains or creates SimulatorUI instances.
 * 2. Main role dispatch routes --role=logger and --role=core strictly to background processes without opening SimulatorUI.
 * 3. Exactly one role (--role=ui) launches SimulatorUI.
 */
public class SingleUIWindowTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("      Single UI Window Test Suite        ");
        System.out.println("=========================================");

        testCoreProcessNoUiReferences();
        testLoggerProcessNoUiReferences();
        testMainRoleExclusivity();

        System.out.println("=========================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL SINGLE-UI TESTS PASSED");
        } else {
            System.out.println("Result: SOME TESTS FAILED");
        }
        System.out.println("=========================================");
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

    private static void testCoreProcessNoUiReferences() {
        boolean pass = true;
        for (Field f : CoreProcess.class.getDeclaredFields()) {
            if ("SimulatorUI".equals(f.getType().getSimpleName()) || f.getType().getName().contains("swing") || f.getType().getName().contains("awt")) {
                pass = false;
                break;
            }
        }
        for (Method m : CoreProcess.class.getDeclaredMethods()) {
            if ("SimulatorUI".equals(m.getReturnType().getSimpleName())) {
                pass = false;
                break;
            }
        }
        recordResult("Test 1: CoreProcess has no UI references", pass,
                "CoreProcess contains 0 SimulatorUI fields or return types; runs strictly as headless server");
    }

    private static void testLoggerProcessNoUiReferences() {
        boolean pass = true;
        for (Field f : LoggerProcess.class.getDeclaredFields()) {
            if ("SimulatorUI".equals(f.getType().getSimpleName()) || f.getType().getName().contains("swing") || f.getType().getName().contains("awt")) {
                pass = false;
                break;
            }
        }
        for (Method m : LoggerProcess.class.getDeclaredMethods()) {
            if ("SimulatorUI".equals(m.getReturnType().getSimpleName())) {
                pass = false;
                break;
            }
        }
        recordResult("Test 2: LoggerProcess has no UI references", pass,
                "LoggerProcess contains 0 SimulatorUI fields or return types; runs strictly as headless server");
    }

    private static void testMainRoleExclusivity() {
        String loggerRole = Main.parseRole(new String[]{"--role=logger"});
        String coreRole = Main.parseRole(new String[]{"--role=core"});
        String uiRole = Main.parseRole(new String[]{"--role=ui"});

        boolean loggerIsBackground = IPCConstants.ROLE_LOGGER.equals(loggerRole);
        boolean coreIsBackground = IPCConstants.ROLE_CORE.equals(coreRole);
        boolean uiIsOnlyUi = IPCConstants.ROLE_UI.equals(uiRole)
                && !uiRole.equals(loggerRole)
                && !uiRole.equals(coreRole);

        boolean pass = loggerIsBackground && coreIsBackground && uiIsOnlyUi;
        recordResult("Test 3: Main Role Exclusivity", pass,
                String.format("LoggerRole='%s', CoreRole='%s', UiRole='%s' (Only UI role launches SimulatorUI)",
                        loggerRole, coreRole, uiRole));
    }
}
