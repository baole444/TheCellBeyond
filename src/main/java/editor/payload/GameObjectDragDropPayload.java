package editor.payload;

import TheCellBeyond.GameObject;

public class GameObjectDragDropPayload {
    private static final String PayloadType = "GameObject_Assignment_Payload";
    private static GameObject currentPayload = null;

    public static void setPayload(GameObject gameObject) {
        currentPayload = gameObject;
    }

    public static GameObject getPayload() {
        return currentPayload;
    }

    public static void clearPayload() {
        currentPayload = null;
    }

    public static String getPayloadType() {
        return PayloadType;
    }
}
