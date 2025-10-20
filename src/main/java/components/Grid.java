package components;

import TheCellBeyond.Viewport;
import TheCellBeyond.Window;
import editor.preference.UserPreference;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import render.DebugDraw;
import utility.Settings;

public class Grid extends Component implements NotSerializeComponent {
    public static final Vector4f color = new Vector4f(0.5f, 0.5f, 0.0f, 0.6f);

    @Override
    public void editorUpdate(float dt) {
        if (dt < 0.0f) return;
        boolean showGridLine = UserPreference.editorPreferences().showGridLine();
        if (!showGridLine) return;
        Viewport viewport = Window.getScene().viewport();
        Vector2f viewPos = viewport.position;
        Vector2f projectSize = viewport.getProjectionSize();

        float firstX = ((int) Math.floor(viewPos.x / Settings.GRID_WIDTH)) * Settings.GRID_WIDTH;
        float firstY = ((int) Math.floor(viewPos.y / Settings.GRID_HEIGHT)) * Settings.GRID_HEIGHT;

        int countVertical = (int)(projectSize.x * viewport.getZoom() / Settings.GRID_WIDTH) + 2;
        int countHorizontal = (int)(projectSize.y * viewport.getZoom() / Settings.GRID_HEIGHT) + 2;

        float height = (int)(projectSize.y * viewport.getZoom()) + Settings.GRID_HEIGHT * 5;
        float width = (int)(projectSize.x * viewport.getZoom()) + Settings.GRID_WIDTH * 5;

        int maxLines = Math.max(countVertical, countHorizontal);


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
