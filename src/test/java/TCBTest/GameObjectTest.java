package TCBTest;

import TheCellBeyond.GameObject;
import components.AnimatedSpriteRenderer;
import components.Component2D;
import components.SpriteRenderer;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameObjectTest {
    @Test
    public void getFirstComponentSupportSubclass() {
        GameObject gameObject = new GameObject("Test Object");
        SpriteRenderer spriteRenderer = new SpriteRenderer();

        gameObject.addComponent(spriteRenderer);
        assertNotNull(gameObject.getFirstComponent(SpriteRenderer.class), "The component should have been added");
        assertNotNull(gameObject.getFirstComponent(Component2D.class), "SpriteRenderer is subclass of Component2D and should have been returned");
        assertNull(gameObject.getFirstComponent(AnimatedSpriteRenderer.class), "There is no AnimatedSpriteRenderer added, the return should have been null");

        gameObject = new GameObject("Test Object 2");
        AnimatedSpriteRenderer animatedSpriteRenderer = new AnimatedSpriteRenderer();
        spriteRenderer = new SpriteRenderer();
        gameObject.addComponent(animatedSpriteRenderer);
        gameObject.addComponent(spriteRenderer);
        assertNotNull(gameObject.getFirstComponent(AnimatedSpriteRenderer.class), "The component should have been added");
        assertEquals(animatedSpriteRenderer ,gameObject.getFirstComponent(SpriteRenderer.class), "AnimatedSpriteRenderer is is subclass of SpriteRenderer and should have been returned");
        assertInstanceOf(AnimatedSpriteRenderer.class, gameObject.getFirstComponent(SpriteRenderer.class), "The returned component should be of instance AnimatedSpriteRenderer");
    }

    @Test
    public void getComponentsSupportSubclass() {
        GameObject gameObject = new GameObject("Test Object");
        SpriteRenderer renderer = new SpriteRenderer();
        AnimatedSpriteRenderer animatedRenderer = new AnimatedSpriteRenderer();
        gameObject.addComponent(renderer);
        gameObject.addComponent(animatedRenderer);
        List<AnimatedSpriteRenderer> animatedSpriteRenderers = gameObject.getComponents(AnimatedSpriteRenderer.class);
        assertFalse(animatedSpriteRenderers.contains(renderer), "SpriteRenderer is superclass of AnimatedSpriteRenderer and should not exist in a list of AnimatedSpriteRenderer");
        List<SpriteRenderer> spriteRenderers = gameObject.getComponents(SpriteRenderer.class);
        assertTrue(spriteRenderers.contains(animatedRenderer), "AnimatedSpriteRenderer is subclass of SpriteRenderer and should exist in a list of SpriteRenderer");
        List<Component2D> spatialComponents = gameObject.getComponents(Component2D.class);
        assertTrue(spatialComponents.contains(renderer) && spatialComponents.contains(animatedRenderer), "Both AnimatedSpriteRenderer and SpriteRenderer are subclasses of Component2D and should exist in a list of Component2D");
    }
}
