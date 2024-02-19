package editor;

import TCB_Field.GameObject;
import TCB_Field.Window;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

import java.util.List;

public class SceneGroup {
    private static String payloadType = "SceneGroup";
    public void imgui() {
        ImGui.begin("Scene group");
        List<GameObject> gameObjects = Window.getScene().loadGameObjects();
        int index = 0;
        for (GameObject o: gameObjects) {
            if (!o.isSerialize()) {
                continue;
            }

            boolean isNode = isNode(o, index);

            if (isNode) {
                ImGui.treePop();
            }
            index++;

        }

        ImGui.end();
    }

    private boolean isNode(GameObject o, int index) {
        ImGui.pushID(index);
        boolean isNode = ImGui.treeNodeEx(o.name,
                ImGuiTreeNodeFlags.DefaultOpen |
                        ImGuiTreeNodeFlags.OpenOnArrow |
                        ImGuiTreeNodeFlags.SpanAvailWidth,
                o.name
        );
        ImGui.popID();

        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload(payloadType, o);

            ImGui.text(o.name);

            ImGui.endDragDropSource();
        }

        if (ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload(payloadType);

            if (payload != null) {
                if (payload.getClass().isAssignableFrom(GameObject.class)) {
                    GameObject obj = (GameObject)payload;
                    System.out.println("Moved " + obj.name);

                }
            }

            ImGui.endDragDropTarget();
        }

        return isNode;
    }
}
