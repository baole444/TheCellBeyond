package editor;

import TheCellBeyond.Main;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * ExitToProjectList handle restarting the editor back to the project when the user request to go back to the project list.
 */
public final class ExitToProjectList {
    private static boolean toProjectList = false;

    private ExitToProjectList() {}

    /**
     * Toggle the request to return to the project list after closing the current editor instance.
     * @param enable true to restart back to project list
     */
    public static void toProjectList(boolean enable) {
        toProjectList = enable;
    }

    /**
     * Create a new editor instance as a new process and exit the current process.
     */
    public static void spawnNewProcess() {
        if (!toProjectList) {
            System.out.println("Finished shutdown");
            return;
        }
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        for (String arg : jvmArgs) {
            if (arg.contains("-agentlib") || arg.contains("-Xdebug")) continue;
            command.add(arg);
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            System.err.println("Failed to start new process: " + e.getMessage());
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Finished shutdown");
        System.exit(0);
    }
}
