/**
 * CoreCommandHandler.java
 *
 * Handles commands received by the Core Process.
 *
 * This class keeps command processing separate from
 * the actual CPU, Memory, Stack and Queue implementation.
 */
public class CoreCommandHandler {

    private final CoreProcess core;

    public CoreCommandHandler(CoreProcess core) {
        this.core = core;
    }

    /**
     * Process a command received by the Core.
     *
     * Supported commands:
     * LOAD
     * RESET
     * STEP
     * RUN
     * STATE
     */
    public String handleCommand(String command) {

        if (command == null || command.trim().isEmpty()) {
            return "ERROR: Empty command";
        }

        String cmd = command.trim().toUpperCase();

        switch (cmd) {

            case "RESET":
                core.reset();
                return "RESET SUCCESS";

            case "STEP":
                ExecutionResult result = core.step();

                if (result == null) {
                    return "ERROR: No execution result";
                }

                return result.getTrace();

            case "RUN":
                core.run();
                return "RUN COMPLETED";

            case "STATE":
                return core.getCoreState();

            default:
                return "ERROR: Unknown command: " + command;
        }
    }
}