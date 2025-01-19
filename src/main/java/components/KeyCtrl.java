package components;

import TCB_Field.GameObject;
import TCB_Field.KeyListener;
import TCB_Field.Window;
import editor.Properties;
import utility.Settings;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A class dedicated to processing key's events for the editor.
 * Handle active objects, duplication and deletion of objects.
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
            properties.clearSelection();

            for (GameObject go : gameObjects) {
                GameObject copy = go.copy();
                Window.getScene().addObjToScene(copy);
                properties.addActiveObj(copy);
            }
        } else if (KeyListener.isKeyPressed(GLFW_KEY_DELETE)) {
            for (GameObject go : activeObjList) {
                go.destroy();
            }

            properties.clearSelection();
        }

        // Make keybind of Shift + S = scale | Shift + T = translate
        if (KeyListener.isKeyTapped(GLFW_KEY_T, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(0);
            //System.out.println("Using move gizmo");
        } else if (KeyListener.isKeyTapped(GLFW_KEY_S, GLFW_MOD_SHIFT)) {
            GizmoControl.setIsGizUse(1);
            //System.out.println("using scale gizmo");
        }
    }
}
