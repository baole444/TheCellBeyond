package utility.prefabrication;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import components.*;
import org.joml.Vector4f;
import render.text.GlyphRange;
import render.texture.Sprite;
import utility.Settings;

public class Prefab {
    /**
     * Instantiate a prefab by name.
     * @param prefabName name of the prefab
     * @return a new GameObject instance or null if prefab not found
     */
    public static GameObject instantiate(String prefabName) {
        return PrefabManager.get().instantiatePrefab(prefabName);
    }

    /**
     * Instantiate a prefab by name and add it to scene.
     * @param prefabName name of the prefab
     * @return a new GameObject instance that was added to the scene or null if prefab not found
     */
    public static GameObject instantiateToScene(String prefabName) {
        GameObject instance = PrefabManager.get().instantiatePrefab(prefabName);
        if (instance != null && Window.getScene() != null) {
            Window.getScene().queueForObjectAddition(instance);
        }

        return instance;
    }

    // TODO: these outdated methods (below) will soon be removed as a new way to add component is introduced
    public static GameObject genSpsObj(Sprite sprite, float sizeX, float sizeY) {
        GameObject block = Window.getScene().generateObject("Sprite_object_gen");
        SpriteRenderer render = new SpriteRenderer();
        render.setSprite(sprite);
        block.addComponent(render);

        return block;
    }

    /**
     * Creates a new GameObject with text rendering capabilities.
     *
     * @param text The text to display
     * @param fontPath Path to the TTF font file
     * @param fontSize Size of the font in pixels
     * @param color Color of the text (RGBA)
     * @return A GameObject with a TextComponent
     */
    public static GameObject genText(String text, String fontPath, int fontSize, Vector4f color, GlyphRange glyphRange) {
        GameObject textObj = Window.getScene().generateObject("Text_object_gen");

        TextComponent textComponent = new TextComponent(text, fontPath, fontSize, color, glyphRange);
        textObj.addComponent(textComponent);

        return textObj;
    }

    /**
     * Creates a new GameObject with text using default settings.
     *
     * @param text The text to display
     * @return A GameObject with a TextComponent using default font and color
     */
    public static GameObject genText(String text) {
        return genText(text, Settings.PATH.CONSOLA, 16, new Vector4f(1, 1, 1, 1), GlyphRange.ASCII);
    }

    /**
     * Creates a new GameObject with text and specific alignment.
     *
     * @param text The text to display
     * @param fontPath Path to the TTF font file
     * @param fontSize Size of the font in pixels
     * @param color Color of the text (RGBA)
     * @param hAlign Horizontal alignment (LEFT, CENTER, RIGHT)
     * @param vAlign Vertical alignment (TOP, MIDDLE, BOTTOM)
     * @return A GameObject with an aligned TextComponent
     */
    public static GameObject genAlignedText(String text, String fontPath, int fontSize, Vector4f color, TextComponent.HorizontalAlignment hAlign, TextComponent.VerticalAlignment vAlign, GlyphRange glyphRange) {
        GameObject textObj = genText(text, fontPath, fontSize, color, glyphRange);
        TextComponent textComponent = textObj.getFirstComponent(TextComponent.class);

        textComponent.setHorizontalAlignment(hAlign);
        textComponent.setVerticalAlignment(vAlign);

        return textObj;
    }
}
