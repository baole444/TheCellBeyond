package components;

import TCB_Field.Viewport;
import TCB_Field.Window;
import org.joml.Vector2f;
import org.joml.Vector3f;
import render.DebugDraw;
import utility.Settings;

public class Grid extends Component {

    @Override
    public void updateEditor(float dt) {
        Viewport viewport = Window.getScene().viewport();
        Vector2f viewPos = viewport.position;
        Vector2f projectSize = viewport.loadProjectSize();

        float firstX = ((int)(viewPos.x / Settings.GRID_WIDTH)) * Settings.GRID_WIDTH;
        float firstY = ((int)(viewPos.y / Settings.GRID_HEIGHT)) * Settings.GRID_HEIGHT;

        int countVertical = (int)(projectSize.x * viewport.loadZoom() / Settings.GRID_WIDTH) + 3;
        int countHorizontal = (int)(projectSize.y *viewport.loadZoom() / Settings.GRID_HEIGHT) + 3;

        float height = (int)(projectSize.y * viewport.loadZoom()) + (5 * Settings.GRID_HEIGHT);
        float width = (int)(projectSize.x * viewport.loadZoom()) + (5 * Settings.GRID_WIDTH);

        int maxLines = Math.max(countVertical, countHorizontal);
        Vector3f color = new Vector3f(0.4f, 0.4f, 0.0f);

        for (int i = 0; i < maxLines; i++) {
            float x = firstX + (Settings.GRID_WIDTH * i);
            float y = firstY + (Settings.GRID_HEIGHT * i);

            if (i < countVertical) {
                DebugDraw.addLine2(new Vector2f(x, firstY), new Vector2f(x, firstY + height), color);
            }

            if (i < countHorizontal) {
                DebugDraw.addLine2(new Vector2f(firstX, y), new Vector2f(firstX + width, y), color);
            }
        }

    }
}
