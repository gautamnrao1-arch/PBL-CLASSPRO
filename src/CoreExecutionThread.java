/**
 * CoreExecutionThread.java
 *
 * Week 4 execution thread for the Core Process.
 *
 * This thread runs the simulator program without blocking
 * the process that controls the Core.
 */
public class CoreExecutionThread extends Thread {

    private final CoreProcess coreProcess;

    public CoreExecutionThread(CoreProcess coreProcess) {
        this.coreProcess = coreProcess;
    }

    @Override
    public void run() {
        if (coreProcess != null) {
            coreProcess.run();
        }
    }
}