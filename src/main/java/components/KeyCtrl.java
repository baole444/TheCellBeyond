package components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.KeyListener;
import TheCellBeyond.Window;
import editor.Properties;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.Settings;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * KeyCtrl (Key Control) is an accumulation of editor keybind and shortcuts.
 * Itself is a non-serialized component that is added to the level editor object.<br>
 * Note: This will change soon, where this will be refactored into processing key event from the game.
 * Engine keybind and shortcut processing will be moved to different class.
 */
public class KeyCtrl extends Component implements NotSerializeComponent {
    @Override
    public void editorUpdate(float dt) {
        GameObject activeGameObj = Properties.getActiveGameObject();
        List<GameObject> activeObjList = Properties.getActiveGameObjects();

        if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeGameObj != null) {
            GameObject newObj = activeGameObj.copy(true);
            Window.getScene().queueForObjectAddition(newObj);

            if (newObj instanceof GameObject2D go2D) {
                Vector2f currentPos = go2D.getOffsetPosition();
                currentPos.add(Settings.GRID_WIDTH / 2.0f, Settings.GRID_HEIGHT / 2.0f);
            }

            Properties.setActiveGameObject(newObj);
        } else if (KeyListener.isKeyTapped(GLFW_KEY_D, GLFW_MOD_CONTROL) && activeObjList.size() > 1) {
            List<GameObject> gameObjects = new ArrayList<>(activeObjList);
            List<List<Vector4f>> trueColors = Properties.getActiveObjTrueColor();
            Properties.clearSelection();

            for (int i = 0; i < gameObjects.size(); i++) {
                GameObject go = gameObjects.get(i);
                GameObject copy = go.copy(true);
                List<Vector4f> colors = trueColors.get(i);
                List<SpriteRenderer> sprites = go.getComponents(SpriteRenderer.class);
                for (int j = 0; j < sprites.size(); j++) {
                    SpriteRenderer sprite = sprites.get(j);
                    if (sprite != null) sprite.setColor(colors.get(j));
                }

                Window.getScene().queueForObjectAddition(copy);
                Properties.addActiveGameObject(copy);
            }
        } else if (KeyListener.isKeyPressed(GLFW_KEY_DELETE)) {
            for (GameObject go : activeObjList) {
                go.destroy();
            }

            Properties.clearSelection();
        }

        // Make keybinding of Shift + S = scale | Shift + T = translate
        if (KeyListener.isKeyTapped(GLFW_KEY_T, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(0);
        } else if (KeyListener.isKeyTapped(GLFW_KEY_S, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(1);
        }

        // Make keybinding of Ctrl + S = Save file | Ctrl + O = open file
        if (KeyListener.isKeyTapped(GLFW_KEY_S, GLFW_MOD_CONTROL)) {
            EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
        }

        if (KeyListener.isKeyTapped(GLFW_KEY_O, GLFW_MOD_CONTROL)) {
            EngineEventCallback.emit(null, new Event(EventType.LEVEL_LOAD));
        }
    }
}
