package TheCellBeyond;

import editor.ExitToProjectList;
import utility.PathResolver;

public class Main {
    static void main(String[] args){
        PathResolver.initialize(null);

        Window window = Window.get();
        window.run();

        System.out.println("Ending editor instance...");
        ExitToProjectList.get().spawnNewProcess();
    }
}
