package components;

import TheCellBeyond.Viewport;
import TheCellBeyond.Window;
import editor.preference.UserPreference;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.DebugDraw;
import utility.Settings;

public class Grid extends Component implements NotSerializeComponent {
    public static final Vector4f normalGridColor = new Vector4f(0.5f, 0.5f, 0.0f, 0.6f);
    public static final Vector4f centralLinesColor = new Vector4f(1.0f);
    public static final float epsilon = 0.01f;

    @Override
    public void editorUpdate(float dt) {
        if (dt < 0.0f) return;

        Viewport viewport = Window.getScene().viewport();
        Vector2f viewPos = viewport.position;
        Vector2f projectSize = viewport.getProjectionSize();

        float firstX = ((int) Math.floor(viewPos.x / Settings.GRID_WIDTH)) * Settings.GRID_WIDTH;
        float firstY = ((int) Math.floor(viewPos.y / Settings.GRID_HEIGHT)) * Settings.GRID_HEIGHT;
        float height = (int)(projectSize.y * viewport.getZoom()) + Settings.GRID_HEIGHT * 5;
        float width = (int)(projectSize.x * viewport.getZoom()) + Settings.GRID_WIDTH * 5;

        DebugDraw.addLine2(new Vector2f(0.0f, firstY), new Vector2f(0.0f, firstY + height), centralLinesColor);
        DebugDraw.addLine2(new Vector2f(firstX, 0.0f), new Vector2f(firstX + width, 0.0f), centralLinesColor);

        if (!UserPreference.editorPreferences().showGridLine()) return;

        int countVertical = (int)(projectSize.x * viewport.getZoom() / Settings.GRID_WIDTH) + 2;
        int countHorizontal = (int)(projectSize.y * viewport.getZoom() / Settings.GRID_HEIGHT) + 2;
        int maxLines = Math.max(countVertical, countHorizontal);


        for (int i = 0; i < maxLines; i++) {
            float x = firstX + (Settings.GRID_WIDTH * i);
            float y = firstY + (Settings.GRID_HEIGHT * i);

            if (i < countVertical && Math.abs(x) > epsilon) {
                DebugDraw.addLine2(new Vector2f(x, firstY), new Vector2f(x, firstY + height), normalGridColor);
            }

            if (i < countHorizontal && Math.abs(y) > epsilon) {
                DebugDraw.addLine2(new Vector2f(firstX, y), new Vector2f(firstX + width, y), normalGridColor);
            }
        }
    }
}
