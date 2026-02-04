package editor.payload;

import render.texture.Sprite;

public class SpriteDragDropPayload {
    private static final String PayloadType = "Sprite_Assignment_Payload";
    private static Sprite currentPayload = null;

    public static void setPayload(Sprite sprite) {
        currentPayload = sprite;
    }

    public static Sprite getPayload() {
        return currentPayload;
    }

    public static void clearPayload() {
        currentPayload = null;
    }

    public static String getPayloadType() {
        return PayloadType;
    }
}
