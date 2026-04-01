package TheCellBeyond;

import editor.ExitToProjectList;
import utility.CrashReport;
import utility.UnifiedPaths;

public class Main {
    static void main(String[] args) {
        CrashReport.install();
        UnifiedPaths.initialize(null);
        try {
            Window window = Window.get();
            window.run();
        } catch (Throwable t) {
            throw new RuntimeException("Engine crashed during execution", t);
        }
        System.out.println("Ending editor instance...");
        ExitToProjectList.get().spawnNewProcess();
    }
}
