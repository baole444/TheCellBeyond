package utility;

import TheCellBeyond.GameObject;
import TheCellBeyond.Window;
import components.*;
import org.joml.Vector4f;
import render.text.GlyphRange;

public class Prefab {
    public static GameObject genSpsObj(Sprite sprite, float sizeX, float sizeY) {
        GameObject block = Window.getScene().generateObject("Sprite_object_gen");
        block.transform.scale.x = sizeX;
        block.transform.scale.y = sizeY;

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
        TextComponent textComponent = textObj.getComponent(TextComponent.class);

        textComponent.setHorizontalAlignment(hAlign);
        textComponent.setVerticalAlignment(vAlign);

        return textObj;
    }

    /*
    // TODO: a universal animation generator, which read from a separated yml for the animation.
    public static GameObject genWheelSpin() {
        SpriteSheet wheelSprites = AssetsPool.loadSpSheet("assets/texture/animation_test.png");
        GameObject wheelSpin = genSpsObj(wheelSprites.spriteIndex(0), 0.32f, 0.32f);

        AnimationState spin = new AnimationState();

        spin.title = "Spin";
        float defaultFrameTime = 0.2f;
        spin.addFrame(wheelSprites.spriteIndex(0), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(1), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(2), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(3), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(4), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(5), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(6), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(7), defaultFrameTime);

        spin.setLoop(true);

        StateEngine stateEngine = new StateEngine();
        stateEngine.addState(spin);
        stateEngine.setDefaultState(spin.title);

        wheelSpin.addComponent(stateEngine);

        PillBoxCollider pillBoxCollider = new PillBoxCollider();
        pillBoxCollider.setWidth(0.32f);
        pillBoxCollider.setHeight(0.32f);
        PhysicBody2D flatPhysicBody = new PhysicBody2D();
        flatPhysicBody.setObjectClassification(PhysicBodyType.Dynamic);
        flatPhysicBody.setNoneStopCollision(false);
        flatPhysicBody.setMass(10.0f);

        wheelSpin.addComponent(flatPhysicBody);
        wheelSpin.addComponent(pillBoxCollider);
        wheelSpin.addComponent(new CharacterControl());

        return wheelSpin;
    }
     */
}
