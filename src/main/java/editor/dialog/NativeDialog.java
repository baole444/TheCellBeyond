package editor.dialog;

import TheCellBeyond.Window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

abstract class NativeDialog {
    protected static long windowHandle = -1;
    protected static int handleType = -1;

    protected static void setPlatform() {
        if (windowHandle != -1 && handleType != -1) return;
        long ptr = Window.get().getWindowPtr();
        switch (glfwGetPlatform()) {
            case GLFW_PLATFORM_X11 -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_X11;
                windowHandle = glfwGetX11Window(ptr);
            }
            case GLFW_PLATFORM_COCOA -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_COCOA;
                windowHandle = glfwGetCocoaWindow(ptr);
            }
            case GLFW_PLATFORM_WIN32 -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_WINDOWS;
                windowHandle = glfwGetWin32Window(ptr);
            }
            default -> {
                handleType = NFD_WINDOW_HANDLE_TYPE_UNSET;
                windowHandle = NULL;
            }
        }
    }
}
