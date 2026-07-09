package editor.dialogs;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

/**
 * Create and handle instances of file dialog with different filter set.
 * @see <a href="https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/util/nfd/HelloNFD.java">Setup file open dialog</a>
 */
public final class OpenFileDialog extends NativeDialog {
    private static final HashMap<List<String>, OpenFileDialog> instances = new HashMap<>();
    private final String hint;
    private final Set<String> fileExtensions;

    private OpenFileDialog(String hint, List<String> filteringExtensions) {
        this.hint = hint;
        fileExtensions = new HashSet<>(filteringExtensions);
    }

    public static OpenFileDialog get(String hint, List<String> filteringExtensions) {
        OpenFileDialog instance = instances.get(filteringExtensions);
        if (instance == null) {
            instance = new OpenFileDialog(hint, filteringExtensions);
            instances.putIfAbsent(filteringExtensions, instance);
        }
        return instance;
    }

    public String openDialog() {
        setPlatform();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointerBuffer = stack.mallocPointer(1);
            NFDOpenDialogArgs args = NFDOpenDialogArgs.calloc(stack).
                    parentWindow(it -> it.type(handleType).handle(windowHandle));
            if (!fileExtensions.isEmpty()) {
                NFDFilterItem.Buffer filter = NFDFilterItem.malloc(1);
                String joined = String.join(",", fileExtensions);
                filter.get(0)
                        .name(stack.UTF8(hint))
                        .spec(stack.UTF8(joined));
                args.filterList(filter);
            }
            int result = NFD_OpenDialog_With(pointerBuffer, args);
            return checkResult(result, pointerBuffer);
        } catch (Exception e) {
            System.err.println("Error while opening file dialog: " + e.getMessage());
            return null;
        }
    }

    private String checkResult(int result, PointerBuffer pp) {
        switch (result) {
            case NFD_OKAY -> {
                long pathPtr = pp.get(0);
                String selectedPath = memUTF8(pathPtr);
                if (isFileValid(selectedPath)){
                    NFD_FreePath(pathPtr);
                    return selectedPath;
                } else {
                    System.out.println("Selected file format not supported.");
                    NFD_FreePath(pathPtr);
                }
            }
            case NFD_CANCEL -> {}
            default -> System.err.format("Error: %s\n", NFD_GetError());
        }
        return null;
    }

    private boolean isFileValid(String path) {
        if (path == null || path.isEmpty()) return false;
        return Files.exists(Paths.get(path));
    }
}
