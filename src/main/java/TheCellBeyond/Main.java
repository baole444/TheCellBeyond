package TheCellBeyond;

import editor.ExitToProjectList;
import utility.UnifiedPaths;

public class Main {
    static void main(String[] args){
        UnifiedPaths.initialize(null);

        Window window = Window.get();
        window.run();

        System.out.println("Ending editor instance...");
        ExitToProjectList.get().spawnNewProcess();
    }
}
