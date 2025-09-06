package TheCellBeyond;

import editor.ExitToProjectList;
import utility.PathResolver;

import java.io.PrintStream;

public class Main {
    public static void main(String[] args){
        ConsoleStream consoleStream = ConsoleStream.get();
        PrintStream printStream = new PrintStream(consoleStream, true);

        System.setOut(printStream);
        System.setErr(printStream);

        PathResolver.initialize(null);

        Window window = Window.get();
        window.run();

        System.out.println("Ending editor instance...");
        ExitToProjectList.get().spawnNewProcess();
    }
}
