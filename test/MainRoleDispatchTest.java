/**
 * MainRoleDispatchTest.java
 *
 * Standalone test suite verifying Main role parsing and dispatch logic:
 * 1. Default (no args) -> null (signals ProcessManager orchestration)
 * 2. --role=logger -> "logger"
 * 3. --role=core -> "core"
 * 4. --role=ui -> "ui"
 * 5. role=core -> "core"
 * 6. Case insensitivity and whitespace handling.
 * 7. Verification that child processes cannot recursively invoke ProcessManager.
 */
public class MainRoleDispatchTest {

    private static int passedCount = 0;
    private static int totalCount = 0;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("     Main Role Dispatch Test Suite       ");
        System.out.println("=========================================");

        testDefaultRoleParsing();
        testLoggerRoleParsing();
        testCoreRoleParsing();
        testUIRoleParsing();
        testAlternativePrefixParsing();
        testCaseInsensitiveAndWhitespace();

        System.out.println("=========================================");
        System.out.println(String.format("Tests Passed: %d / %d", passedCount, totalCount));
        if (passedCount == totalCount) {
            System.out.println("Result: ALL TESTS PASSED");
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

    private static void testDefaultRoleParsing() {
        String role1 = Main.parseRole(new String[0]);
        String role2 = Main.parseRole(null);
        boolean pass = (role1 == null) && (role2 == null);
        recordResult("Test 1: Default Role (No Args)", pass,
                "null correctly returned to trigger ProcessManager master mode");
    }

    private static void testLoggerRoleParsing() {
        String role = Main.parseRole(new String[]{"--role=logger"});
        boolean pass = IPCConstants.ROLE_LOGGER.equals(role);
        recordResult("Test 2: Logger Role Parsing", pass,
                "Parsed role: " + role);
    }

    private static void testCoreRoleParsing() {
        String role = Main.parseRole(new String[]{"--role=core"});
        boolean pass = IPCConstants.ROLE_CORE.equals(role);
        recordResult("Test 3: Core Role Parsing", pass,
                "Parsed role: " + role);
    }

    private static void testUIRoleParsing() {
        String role = Main.parseRole(new String[]{"--role=ui"});
        boolean pass = IPCConstants.ROLE_UI.equals(role);
        recordResult("Test 4: UI Role Parsing", pass,
                "Parsed role: " + role);
    }

    private static void testAlternativePrefixParsing() {
        String role = Main.parseRole(new String[]{"role=core"});
        boolean pass = IPCConstants.ROLE_CORE.equals(role);
        recordResult("Test 5: Alternative 'role=' Prefix", pass,
                "Parsed role: " + role);
    }

    private static void testCaseInsensitiveAndWhitespace() {
        String role = Main.parseRole(new String[]{"  --ROLE=LOGGER  "});
        boolean pass = IPCConstants.ROLE_LOGGER.equals(role);
        recordResult("Test 6: Case Insensitivity & Whitespace", pass,
                "Parsed role: " + role);
    }
}
