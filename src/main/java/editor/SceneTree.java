package editor;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

import java.util.List;

public class SceneTree {
    private static final String GROUPING_PAYLOAD = "ObjectGrouping";

    public void imgui() {
        ImGui.begin("Object Grouping");
        List<GameObject> gameObjects = Window.getScene().getSerializedObject();
        int index = 0;
        for (GameObject obj: gameObjects) {
            if (!obj.isSerialize()) {
                continue;
            }

            boolean openTreeNode = executeTreeNode(obj, index);

            if (openTreeNode) {
                ImGui.treePop();
            }
            index++;
        }
        ImGui.end();
    }

    /*
    Pay load
    - ImGui have a global payload method (begin/end).
    - Drop is accept when have matching type.
    - Type is string set by player.
    - Type name string limit is 32 character.

     */
    private boolean executeTreeNode(GameObject obj, int index) {
        ImGui.pushID(index);

        // Tree node drag drop payload
        boolean openTreeNode = ImGui.treeNodeEx(
                obj.name,
                ImGuiTreeNodeFlags.DefaultOpen |
                        ImGuiTreeNodeFlags.FramePadding |
                        ImGuiTreeNodeFlags.OpenOnArrow |
                        ImGuiTreeNodeFlags.SpanAvailWidth,
                obj.name
        );
        ImGui.popID();

        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload(GROUPING_PAYLOAD, obj);
            ImGui.text(obj.name);
            // Can add more code to this

            //
            ImGui.endDragDropSource();
        }

        if (ImGui.beginDragDropTarget()) {
            Object payloadObj = ImGui.acceptDragDropPayload(GROUPING_PAYLOAD);

            if (payloadObj != null) {
                if (payloadObj.getClass().isAssignableFrom(GameObject.class)) {
                    GameObject go = (GameObject) payloadObj;
                }
            }

            ImGui.endDragDropTarget();
        }

        return openTreeNode;
    }

}
