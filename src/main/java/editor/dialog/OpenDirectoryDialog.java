package editor.dialog;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDPickFolderArgs;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

public final class OpenDirectoryDialog extends NativeDialog {
    public static Path openDialog() {
        setPlatform();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer outPath = stack.mallocPointer(1);
            int result = NFD_PickFolder_With(outPath, NFDPickFolderArgs.calloc(stack)
                    .parentWindow(it -> it
                            .type(handleType)
                            .handle(windowHandle)
                    )
            );
            return checkResult(result, outPath);
        } catch (Exception e) {
            System.err.println("Error while opening directory dialog: " + e.getMessage());
            return null;
        }
    }

    private static Path checkResult(int result, PointerBuffer pp) {
        Path validPath = null;
        switch (result) {
            case NFD_OKAY -> {
                long pathPtr = pp.get(0);
                String selectedPath = memUTF8(pathPtr);
                if (validDirectory(selectedPath)){
                    validPath = Paths.get(selectedPath);
                    NFD_FreePath(pathPtr);
                } else {
                    System.out.println("Selected directory does not exist");
                    NFD_FreePath(pathPtr);
                }
            }
            case NFD_CANCEL -> {}
            default -> System.err.format("Error: %s\n", NFD_GetError());
        }
        return validPath;
    }

    private static boolean validDirectory(String path) {
        if (path == null || path.isEmpty()) return false;
        Path check = Path.of(path);
        return check.toFile().isDirectory();
    }
}
