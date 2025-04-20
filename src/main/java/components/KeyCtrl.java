package components;

import TCB_Field.GameObject;
import TCB_Field.ImGuiLayer;
import TCB_Field.KeyListener;
import TCB_Field.Window;
import editor.Properties;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import imgui.type.ImBoolean;
import org.joml.Vector4f;
import utility.Settings;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to processing key's events for the editor.
 * Handle active objects, duplication and deletion of objects.
 * Handle Project, Save and Load keybinding
 */
public class KeyCtrl extends Component {
    @Override
    public void editorUpdate(float dt) {
        Properties properties = Window.loadImGui().loadProperties();
        GameObject activeGameObj = properties.loadActiveObj();
        List<GameObject> activeObjList = properties.loadAllActiveObj();


        if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeGameObj != null) {
            GameObject newObj = activeGameObj.copy();
            Window.getScene().addObjToScene(newObj);

            newObj.transform.position.add(Settings.GRID_WIDTH, 0.0f);
            properties.setActiveGameObj(newObj);
        } else if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeObjList.size() > 1) {
            List<GameObject> gameObjects = new ArrayList<>(activeObjList);

            // Get a copy of selected sprites' true color.
            List<Vector4f> trueColor = properties.getActiveObjTrueColor();

            properties.clearSelection();

            int i = 0;
            for (GameObject go : gameObjects) {
                GameObject copy = go.copy();

                // Update sprite's color to true color
                SpriteRender spriteRender = copy.getComponent(SpriteRender.class);
                if (spriteRender != null) {
                    spriteRender.setColor(trueColor.get(i));
                }

                // Refresh texture
                if (copy.getComponent(StateEngine.class) != null) {
                    copy.getComponent(StateEngine.class).reloadTexture();
                }

                Window.getScene().addObjToScene(copy);
                properties.addActiveObj(copy);
                i++;
            }
        } else if (KeyListener.isKeyPressed(GLFW_KEY_DELETE)) {
            for (GameObject go : activeObjList) {
                go.destroy();
            }

            properties.clearSelection();
        }

        // Make keybinding of Shift + S = scale | Shift + T = translate
        if (KeyListener.isKeyTapped(GLFW_KEY_T, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(0);
            //System.out.println("Using move gizmo");
        } else if (KeyListener.isKeyTapped(GLFW_KEY_S, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(1);
            //System.out.println("using scale gizmo");
        }

        // Make keybinding of Ctrl + S = Save file | Ctrl + O = open file
        if (KeyListener.isKeyTapped(GLFW_KEY_S, GLFW_MOD_CONTROL)) {
            EventSystem.notice(null, new Event(EventType.LEVEL_SAVE));
        }

        if (KeyListener.isKeyTapped(GLFW_KEY_O, GLFW_MOD_CONTROL)) {
            EventSystem.notice(null, new Event(EventType.LEVEL_LOAD));
        }

        // Make keybinding of Ctrl + P = Open project dialog box | ESC while dialog box is active = close.
        if (KeyListener.isKeyTapped(GLFW_KEY_P, GLFW_MOD_CONTROL)) {
            ImGuiLayer.set_openFileDialog(new ImBoolean(true));
        } else if (KeyListener.isKeyTapped(GLFW_KEY_ESCAPE) && ImGuiLayer.get_openFileDialog().get()) {
            ImGuiLayer.set_openFileDialog(new ImBoolean(false));
        }
    }
}
