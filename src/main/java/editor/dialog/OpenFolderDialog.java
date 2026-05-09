package editor.dialog;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDPickFolderArgs;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

public final class OpenFolderDialog extends NativeDialog {
    public static Path openFolderDialog() {
        setPlatform();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer outPath = stack.mallocPointer(1);
            int result = NFD_PickFolder_With(outPath, NFDPickFolderArgs.calloc(stack)
                    .parentWindow(it -> it
                            .type(handleType)
                            .handle(windowHandle)
                    )
            );
            // Check the result, set project file path and free the pointer.
            return checkResult(result, outPath);
        } catch (Exception e) {
            System.err.println("Error while opening file dialog: " + e.getMessage());
            return null;
        }
    }

    private static Path checkResult(int result, PointerBuffer pp) {
        Path validPath = null;
        switch (result) {
            case NFD_OKAY -> {
                long pathPtr = pp.get(0);
                String selectedPath = memUTF8(pathPtr);

                if (isFolderValid(selectedPath)){
                    validPath = Paths.get(selectedPath);
                    NFD_FreePath(pathPtr);
                } else {
                    System.out.println("Selected folder does not exist");
                    NFD_FreePath(pathPtr);
                }
            }
            case NFD_CANCEL -> {}
            default -> System.err.format("Error: %s\n", NFD_GetError());
        }
        return validPath;
    }

    private static boolean isFolderValid(String path) {
        if (path == null || path.isEmpty()) return false;
        Path newProjectRoot = Path.of(path);
        return newProjectRoot.toFile().isDirectory();
    }
}
