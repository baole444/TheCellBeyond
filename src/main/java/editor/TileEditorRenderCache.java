package editor;

import imgui.ImVec2;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;

import java.util.HashSet;

final class TileEditorRenderCache {
    Sprite sprite = null;
    float spriteWidth = -1.0f;
    float spriteHeight = -1.0f;
    int gridX = -1;
    int gridY = -1;
    int startX = -1;
    int startY = -1;
    int tileCount = -1;
    int selectionSize = -1;
    Tile firstSelection = null;
    boolean selectionDirty = true;

    boolean checkerDirty = true;
    boolean outlineDirty = true;
    boolean selectedDirty = true;

    int checkerCountX = 0;
    int checkerCountY = 0;
    float checkerImageWidth = 0.0f;
    float checkerImageHeight = 0.0f;
    boolean checkerReady = false;

    float[] outlineSegments = new float[0];
    int outlineCount = 0;
    boolean outlineReady = false;
    int outlineBuildCursor = 0;
    Vector2i[] outlineBuildList = null;
    HashSet<Vector2i> outlineBuildSet = null;

    float[] selectedRects = new float[0];
    int selectedCount = 0;
    float firstSelectedX = 0.0f;
    float firstSelectedY = 0.0f;
    boolean hasFirstSelected = false;
    boolean selectedReady = false;
    int selectedBuildCursor = 0;
    Tile[] selectedBuildList = null;
    Vector2i selectedBuildFirstCoordinate = null;

    final ImVec2 tmpMin = new ImVec2();
    final ImVec2 tmpMax = new ImVec2();

    void reset() {
        sprite = null;
        spriteWidth = -1.0f;
        spriteHeight = -1.0f;
        gridX = -1;
        gridY = -1;
        startX = -1;
        startY = -1;
        tileCount = -1;
        selectionSize = -1;
        firstSelection = null;
        selectionDirty = true;

        checkerDirty = true;
        outlineDirty = true;
        selectedDirty = true;

        checkerCountX = 0;
        checkerCountY = 0;
        checkerImageWidth = 0.0f;
        checkerImageHeight = 0.0f;
        checkerReady = false;

        outlineSegments = new float[0];
        outlineCount = 0;
        outlineReady = false;
        outlineBuildCursor = 0;
        outlineBuildList = null;
        outlineBuildSet = null;

        selectedRects = new float[0];
        selectedCount = 0;
        hasFirstSelected = false;
        selectedReady = false;
        selectedBuildCursor = 0;
        selectedBuildList = null;
        selectedBuildFirstCoordinate = null;
    }
}
