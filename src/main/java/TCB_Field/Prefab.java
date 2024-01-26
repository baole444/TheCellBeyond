package TCB_Field;

import components.Sprite;
import components.SpriteRender;
import org.joml.Vector2f;

public class Prefab {
    public static GameObject genSpsObj(Sprite sprite, float sizeX, float sizeY) {
        GameObject block = new GameObject("Sprite_object_gen",
                new Transform(
                        new Vector2f(),
                        new Vector2f(sizeX, sizeY)
                ), 0
        );

        SpriteRender render = new SpriteRender();
        render.setSprite(sprite);
        block.addComponent(render);

        return block;
    }
}
