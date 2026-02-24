package editor.components;

import TheCellBeyond.Viewport;
import TheCellBeyond.internal.LogicServer;
import components.Component;
import components.NotSerializeComponent;
import editor.preference.UserPreference;
import project.Project;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector4f;
import render.DebugDraw;
import utility.Settings;
import utility.WorldUnit;

import java.util.HashSet;

/**
 * EditorGrid hold the logic to draw debug lines in grid pattern when editing a scene.
 */
public final class EditorGrid extends Component implements NotSerializeComponent {
    /**
     * The minimum gap between two line in world units before one is skipped.
     */
    public static final float minimumGap = 0.01f;
    private static final Vector4f normalGridColor = new Vector4f(0.5f, 0.5f, 0.35f, 0.35f);
    private static final Vector4f centralLinesColor = new Vector4f(0.75f);
    private static final Vector4f verticalBoundColor = new Vector4f(0.5f, 0.5f, 1.0f, 0.75f);
    private static final Vector4f horizontalBoundColor = new Vector4f(1.0f, 0.0f, 1.0f, 0.75f);
    private static final HashSet<Class<?>> prioritizing = new HashSet<>();

    /**
     * Create a new {@link EditorGizmo} component.
     */
    public EditorGrid() {
        String name = EditorGrid.class.getSimpleName();
        super(name);
    }

    @Override
    public void editorUpdate(float dt) {
        if (dt < 0.0f) return;
        Viewport viewport = LogicServer.currentScene().viewport();
        Vector2f totalZoom = new Vector2f(viewport.getZoom()).div(Project.preference().textureGlobalScale());
        Vector2f viewPos = viewport.position;
        Vector2f projectSize = viewport.getProjectionSize();

        float firstX = ((int) Math.floor(viewPos.x / Settings.GRID_WIDTH)) * Settings.GRID_WIDTH;
        float firstY = ((int) Math.floor(viewPos.y / Settings.GRID_HEIGHT)) * Settings.GRID_HEIGHT;
        float width = (int) ( projectSize.x * totalZoom.x) + Settings.GRID_WIDTH * 5;
        float height = (int) (projectSize.y * totalZoom.y) + Settings.GRID_HEIGHT * 5;
        float gameWindowWidth = WorldUnit.pixelToWorld(Project.preference().gameWindowWidth());
        float gameWindowHeight = WorldUnit.pixelToWorld(Project.preference().gameWindowHeight());

        if (UserPreference.editorPreferences().showGridLine() && prioritizing.isEmpty()) {
            int countVertical = (int) (projectSize.x * totalZoom.x / Settings.GRID_WIDTH) + 2;
            int countHorizontal = (int) (projectSize.y * totalZoom.y / Settings.GRID_HEIGHT) + 2;
            int maxLines = Math.max(countVertical, countHorizontal);
            drawGrid(firstX, firstY, maxLines, countVertical, countHorizontal, width, height);
        }

        drawCentralLines(firstX, firstY, width, height);
        drawGameWindowBound(gameWindowWidth, gameWindowHeight);
    }

    private void drawCentralLines(float firstX, float firstY, float width, float height) {
        DebugDraw.addLine2(new Vector2f(0.0f, firstY), new Vector2f(0.0f, firstY + height), centralLinesColor);
        DebugDraw.addLine2(new Vector2f(firstX, 0.0f), new Vector2f(firstX + width, 0.0f), centralLinesColor);
    }

    private void drawGameWindowBound(float gameWindowWidth, float gameWindowHeight) {
        DebugDraw.addLine2(new Vector2f(gameWindowWidth, 0.0f), new Vector2f(gameWindowWidth, gameWindowHeight), verticalBoundColor);
        DebugDraw.addLine2(new Vector2f(0, gameWindowHeight), new Vector2f(gameWindowWidth, gameWindowHeight), horizontalBoundColor);
    }

    private void drawGrid(float firstX, float firstY, int maxLines, int countVertical, int countHorizontal, float width, float height) {
        for (int i = 0; i < maxLines; i++) {
            float x = firstX + (Settings.GRID_WIDTH * i);
            float y = firstY + (Settings.GRID_HEIGHT * i);
            if (canDrawVerticalLine(i, countVertical, x)) {
                DebugDraw.addLine2(new Vector2f(x, firstY), new Vector2f(x, firstY + height), normalGridColor);
            }
            if (canDrawHorizontalLine(i, countHorizontal, y)) {
                DebugDraw.addLine2(new Vector2f(firstX, y), new Vector2f(firstX + width, y), normalGridColor);
            }
        }
    }

    private boolean canDrawVerticalLine(int index, int verticalLineCount, float x) {
        if (index >= verticalLineCount) return false;
        return Math.abs(x) > minimumGap;
    }

    private boolean canDrawHorizontalLine(int index, int horizontalLineCount, float y) {
        if (index >= horizontalLineCount) return false;
        return Math.abs(y) > minimumGap;
    }

    /**
     * Take grid rendering priority from this editor grid.
     * @param receiver the class that want to take priority over this
     */
    public static void givePriority(Class<?> receiver) {
        prioritizing.add(receiver);
    }

    /**
     * Release priority and give it back to this editor grid.
     * @param releaser the class that took priority and now want to give it back
     */
    public static void releasePriority(Class<?> releaser) {
        prioritizing.remove(releaser);
    }
}
