package editor;

import TCB_Field.KeyListener;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.ImGui;

import static org.lwjgl.glfw.GLFW.*;

public class MenuBar {
    public void imgui() {
        ImGui.beginMenuBar();

        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S") || ((KeyListener.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) && KeyListener.isKeyTapped(GLFW_KEY_S))) {
                EventSystem.notice(null, new Event(EventType.LevelSave));
                System.out.println("Saving scene");
            }

            if (ImGui.menuItem("Open", "Ctrl+O") || ((KeyListener.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) && KeyListener.isKeyTapped(GLFW_KEY_O))) {
                EventSystem.notice(null, new Event(EventType.LevelLoad));
                System.out.println("Loading scene");
            }

            ImGui.endMenu();
        }

        ImGui.endMenuBar();
    }
}
