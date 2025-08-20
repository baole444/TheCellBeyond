package components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.KeyListener;
import TheCellBeyond.Window;
import editor.OpenProjectDialog;
import editor.Properties;
import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.Settings;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to processing key's events for the editor.
 * Handle active objects, duplication, and deletion of objects.
 * Handle Project, Save and Load keybinding
 */
public class KeyCtrl extends Component {
    @Override
    public void editorUpdate(float dt) {
        Properties properties = Window.getImGuiLayer().loadProperties();
        GameObject activeGameObj = properties.getActiveGameObject();
        List<GameObject> activeObjList = properties.getActiveGameObjects();


        if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeGameObj != null) {
            GameObject newObj = activeGameObj.copy(true);
            Window.getScene().queueForObjectAddition(newObj);

            if (newObj instanceof GameObject2D go2D) {
                Vector2f currentPos = go2D.getOffsetPosition();
                currentPos.add(Settings.GRID_WIDTH / 2.0f, Settings.GRID_HEIGHT / 2.0f);
            }

            properties.setActiveGameObject(newObj);
        } else if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeObjList.size() > 1) {
            List<GameObject> gameObjects = new ArrayList<>(activeObjList);

            // Get a copy of selected sprites' true color.
            List<Vector4f> trueColor = properties.getActiveObjTrueColor();

            properties.clearSelection();

            int i = 0;
            for (GameObject go : gameObjects) {
                GameObject copy = go.copy(true);

                // Update sprite's color to true color
                SpriteRenderer spriteRenderer = copy.getFirstComponent(SpriteRenderer.class);
                if (spriteRenderer != null) {
                    spriteRenderer.setColor(trueColor.get(i));
                }

                // Refresh texture
                if (copy.getFirstComponent(StateEngine.class) != null) {
                    copy.getFirstComponent(StateEngine.class).reloadTexture();
                }

                Window.getScene().queueForObjectAddition(copy);
                properties.addActiveGameObject(copy);
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
            EventSystem.emit(null, new Event(EventType.LEVEL_SAVE));
        }

        if (KeyListener.isKeyTapped(GLFW_KEY_O, GLFW_MOD_CONTROL)) {
            EventSystem.emit(null, new Event(EventType.LEVEL_LOAD));
        }

        // Make keybinding of Ctrl + P = Open project dialog box | ESC while dialog box is active = close.
        if (KeyListener.isKeyTapped(GLFW_KEY_P, GLFW_MOD_CONTROL)) {
            OpenProjectDialog.openProjectDialog();
        }
    }
}
