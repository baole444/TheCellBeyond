package editor;

import org.lwjgl.system.Platform;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SystemExplorer contain static method use to determine the host platform and open the file manager.
 */
public final class SystemExplorer {
    private static final EngineLog Logger = new EngineLog(SystemExplorer.class);

    private SystemExplorer() {}

    /**
     * Open the file manager UI to the given path.
     * @param directory the path to the opening directory
     */
    public static void openDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            Logger.warning(String.format("Cannot open %s: no such directory or not a directory", directory));
            return;
        }
        String path = directory.toString();
        String command = openCommand();
        try {
            new ProcessBuilder(command, path).inheritIO().start();
        } catch (IOException e) {
            Logger.warning(String.format("Failed to open '%s' in the file manager: %s", path, e.getMessage()));
        }
    }

    /**
     * Open a file from the given path with the host platform's default application for the file's type.
     * @param file the path to the opening file
     */
    public static void openFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            Logger.warning(String.format("Cannot open %s: no such file or not a regular file", file));
            return;
        }
        String path = file.toString();
        String command = openCommand();
        try {
            new ProcessBuilder(command, path).inheritIO().start();
        } catch (IOException e) {
            Logger.warning(String.format("Failed to open '%s' in the file manager: %s", path, e.getMessage()));
        }
    }

    private static String openCommand() {
        return switch (Platform.get()) {
            case WINDOWS -> "explorer.exe";
            case MACOSX -> "open";
            default -> "xdg-open";
        };
    }
}
