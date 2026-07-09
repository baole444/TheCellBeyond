package TheCellBeyond;

import editor.ExitToProjectList;
import editor.StartupWindow;
import scripting.ScriptLoader;
import utility.CrashReport;

public class Main {
    private static String openProjectPath;

    static void main(String[] args) {
        CrashReport.install();
        ScriptLoader.bindMainThread();
        StartupWindow.init();
        openProjectPath = StartupWindow.show();
        StartupWindow.dispose();
        if (openProjectPath == null || openProjectPath.isBlank()) return;
        Window window = Window.get();
        window.run();
        System.out.println("Ending editor instance...");
        ExitToProjectList.spawnNewProcess();
    }

    static String openProjectPath() {
        return openProjectPath;
    }
}
