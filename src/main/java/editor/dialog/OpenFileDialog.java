package editor.dialog;

import TheCellBeyond.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.nfd.NativeFileDialog.*;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_GetError;

public class OpenFileDialog {
    private static final HashMap<List<String>, OpenFileDialog> instances = new HashMap<>();
    private static long windowHandle = -1;
    private static int handleType = -1;
    private final Set<String> fileExtensions;

    private OpenFileDialog(List<String> filteringExtensions) {
        fileExtensions = new HashSet<>(filteringExtensions);
    }

    public static OpenFileDialog get(List<String> filteringExtensions) {
        OpenFileDialog instance = instances.get(filteringExtensions);

        if (instance == null) {
            instance = new OpenFileDialog(filteringExtensions);
            instances.putIfAbsent(filteringExtensions, instance);
        }

        return instance;
    }

    private static void setPlatform() {
        long ptr = Window.get().getWindowPtr();

        switch (Platform.get()) {
            case FREEBSD, LINUX -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_X11;
                windowHandle = glfwGetX11Window(ptr);
            }

            case MACOSX -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_COCOA;
                windowHandle = glfwGetCocoaWindow(ptr);
            }

            case WINDOWS -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_WINDOWS;
                windowHandle = glfwGetWin32Window(ptr);
            }

            default -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_UNSET;
                windowHandle = NULL;
            }
        }
    }

    public String openDialog() {
        if (windowHandle == -1 || handleType == -1) {
            setPlatform();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NFDFilterItem.Buffer filter = NFDFilterItem.malloc(1);
            for (String ext : fileExtensions) {
                filter.get(0).spec(stack.UTF8(ext));
            }

            PointerBuffer pointerBuffer = stack.mallocPointer(1);

            int result = NFD_OpenDialog_With(pointerBuffer, NFDOpenDialogArgs.calloc(stack)
                    .filterList(filter)
                    .parentWindow(it -> it
                            .type(handleType)
                            .handle(windowHandle)
                    )
            );

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
