package TheCellBeyond;

import TheCellBeyond.internal.Engine;
import editor.EditorHost;
import editor.ExitToProjectList;
import editor.StartupWindow;
import scripting.ScriptLoader;
import utility.CrashReport;
import utility.log.EngineLog;

public class Main {
    static void main(String[] args) {
        CrashReport.install();
        ScriptLoader.bindMainThread();
        StartupWindow.init();
        String openProjectPath = StartupWindow.show();
        StartupWindow.dispose();
        if (openProjectPath == null || openProjectPath.isBlank()) return;
        Engine.host(new EditorHost(openProjectPath));
        Window window = Window.get();
        window.run();
        ExitToProjectList.spawnNewProcess();
    }
}
