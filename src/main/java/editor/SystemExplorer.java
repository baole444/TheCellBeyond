package editor;

import org.lwjgl.system.Platform;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SystemExplorer contain static method use to determine the host platform and open the file manager.
 */
final class SystemExplorer {
    private static final EngineLog Logger = new EngineLog(SystemExplorer.class);

    private SystemExplorer() {}

    /**
     * Open the file manager UI to the given path.
     * @param directory the path to the opening directory
     */
    static void open(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            Logger.warning(String.format("Cannot open %s: no such directory or not a directory", directory));
            return;
        }
        String path = directory.toString();
        String command = switch (Platform.get()) {
            case WINDOWS -> "explorer.exe";
            case MACOSX -> "open";
            default -> "xdg-open";
        };
        try {
            new ProcessBuilder(command, path).inheritIO().start();
        } catch (IOException e) {
            Logger.warning(String.format("Failed to open '%s' in the file manager: %s", path, e.getMessage()));
        }
    }
}
