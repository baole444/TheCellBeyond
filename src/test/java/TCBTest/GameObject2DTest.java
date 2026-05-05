package TCBTest;

import TheCellBeyond.GameObject2D;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameObject2DTest {
    private static final float epsilon = 0.001f;

    @Test
    void globalPosition() {
        GameObject2D parent = new GameObject2D("parent");
        GameObject2D child = new GameObject2D("child");

        parent.addChild(child);

        Vector2f parentGlobalPos = parent.globalPosition();
        assertEquals(0.0f, parentGlobalPos.x, epsilon, "Parent global x should be 0.0");
        assertEquals(0.0f, parentGlobalPos.y, epsilon, "Parent global y should be 0.0");

        child.globalPosition(4.0f, 4.0f);

        Vector2f childGlobalPos = child.globalPosition();
        assertEquals(4.0f, childGlobalPos.x, epsilon, "Child global x should be 4.0");
        assertEquals(4.0f, childGlobalPos.y, epsilon, "Child global y should be 4.0");
    }
}