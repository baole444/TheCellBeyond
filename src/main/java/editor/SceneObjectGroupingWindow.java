package editor;

import TCB_Field.GameObject;
import TCB_Field.Window;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

import java.util.List;

public class SceneObjectGroupingWindow {
    private static final String GROUPING_PAYLOAD = "ObjectGrouping";

    public void imgui() {
        ImGui.begin("Object Grouping");
        //ImGui.text("Phantom object might\nappear in this list.\nIt does not affect\nthe save file.");
        List<GameObject> gameObjects = Window.getScene().getGameObject();
        int index = 0;
        for (GameObject obj: gameObjects) {
            if (!obj.isSerialize()) {
                continue;
            }

            boolean openTreeNode = executeTreeNode(obj, index);

            if (ImGui.beginDragDropTarget()) {
                Object payloadObj = ImGui.acceptDragDropPayload(GROUPING_PAYLOAD);

                // Fallback check ensure.
                if (payloadObj != null) {
                    if (payloadObj.getClass().isAssignableFrom(GameObject.class)) {
                        GameObject userGameObj = (GameObject)payloadObj;
                        System.out.println("Drop successfully: " + userGameObj.name);
                    }

                }
                ImGui.endDragDropTarget();
            }

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

        return openTreeNode;
    }

}
