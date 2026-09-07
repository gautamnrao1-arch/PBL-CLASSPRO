import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Entry point for the PIC16F72 Microcontroller Simulator.
 *
 * This class is solely responsible for bootstrapping the application:
 * 1. Instantiates the Registers subsystem.
 * 2. Instantiates the Memory subsystem.
 * 3. Instantiates the central CPU controller with the subsystems.
 * 4. Instantiates the SimulatorUI with the CPU controller.
 * 5. Launches and displays the graphical interface on the Swing Event Dispatch Thread.
 *
 * NOTE: This class contains no business, execution, or UI component logic.
 */
public class Main {

    public static void main(String[] args) {
        // Launch the graphical user interface on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Registers registers = new Registers();
                Memory memory = new Memory();
                CPU cpu = new CPU(registers, memory);
                SimulatorUI ui = new SimulatorUI(cpu);
                ui.setVisible(true);
            }
        });
    }
}
