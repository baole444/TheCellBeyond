package editor;

import TheCellBeyond.Main;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class ExitToProjectList {
    private static ExitToProjectList instance;
    private boolean toProjectList;

    private ExitToProjectList() {
        toProjectList = false;
    }

    public static ExitToProjectList get() {
        if (instance == null) instance = new ExitToProjectList();

        return instance;
    }

    public void toProjectList(boolean enable) {
        toProjectList = enable;
    }

    public void spawnNewProcess() {
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

        System.out.println("Restarting with arguments:\n" + String.join(" ", command));

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
