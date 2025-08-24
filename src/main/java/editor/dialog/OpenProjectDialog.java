package editor.dialog;

import TheCellBeyond.Window;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

public class OpenProjectDialog {
    private static long windowHandle = -1;
    private static int handleType = -1;

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

    public static void openProjectDialog() {
        if (windowHandle == -1 || handleType == -1) {
            setPlatform();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NFDFilterItem.Buffer filter = NFDFilterItem.malloc(1);
            filter.get(0)
                    .name(stack.UTF8("_project"))
                    .spec(stack.UTF8("yml,yaml"));

            PointerBuffer pointerBuffer = stack.mallocPointer(1);

            int result = NFD_OpenDialog_With(pointerBuffer, NFDOpenDialogArgs.calloc(stack)
                    .filterList(filter)
                    .parentWindow(it -> it
                            .type(handleType)
                            .handle(windowHandle)
                    )
            );

            // Check the result, set project file path and free the pointer.
            checkResult(result, pointerBuffer);
        } catch (Exception e) {
            System.err.println("Error while opening file dialog: " + e.getMessage());
        }
    }

    private static void checkResult(int result, PointerBuffer pp) {
        switch (result) {
            case NFD_OKAY -> {
                long pathPtr = pp.get(0);
                String selectedPath = memUTF8(pathPtr);

                if (isFileValid(selectedPath)){
                    EventSystem.emit(selectedPath, new Event(EventType.PROJECT_LOAD));

                    NFD_FreePath(pathPtr);
                } else {
                    System.out.println("Selected file format not supported.");
                    NFD_FreePath(pathPtr);
                }
            }
            case NFD_CANCEL -> {}
            default -> System.err.format("Error: %s\n", NFD_GetError());
        }
    }

    private static boolean isFileValid(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (!Files.exists(Paths.get(path))) return false;

        return path.toLowerCase().endsWith(".yml") || path.toLowerCase().endsWith(".yaml");
    }
}
