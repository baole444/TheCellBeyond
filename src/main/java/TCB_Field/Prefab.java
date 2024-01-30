package TCB_Field;

import components.Sprite;
import components.SpriteRender;

public class Prefab {
    public static GameObject genSpsObj(Sprite sprite, float sizeX, float sizeY) {
        GameObject block = Window.getScene().generateObject("Sprite_object_gen");
        block.transform.scale.x = sizeX;
        block.transform.scale.y = sizeY;

        SpriteRender render = new SpriteRender();
        render.setSprite(sprite);
        block.addComponent(render);

        return block;
    }
}
