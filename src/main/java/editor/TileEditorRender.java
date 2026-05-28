package editor;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.Tile;
import render.texture.TileSet;
import utility.log.EngineLog;

import java.util.HashSet;
import java.util.List;

final class TileEditorRender {
    private static final EngineLog Logger = new EngineLog(TileEditorRender.class);
    static final int BgSquareSize = 32;
    static final int LightSquareColor = ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.5f);
    static final int DarkSquareColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.5f);
    static final int TileHighlightColor = ImGui.getColorU32(1.0f, 1.0f, 0.5f, 0.75f);
    static final int SelectedTileFillColor = ImGui.getColorU32(0.5f, 1.0f, 1.0f, 0.25f);
    static final int SelectedTileColor = ImGui.getColorU32(0.5f, 1.0f, 1.0f, 1.0f);
    static final int FirstSelectedTileColor = ImGui.getColorU32(1.0f, 0.5f, 0.5f, 1.0f);
    static final int FirstSelectedTileFillColor = ImGui.getColorU32(1.0f, 0.5f, 0.5f, 0.25f);
    static final float SelectedThickness = 2.0f;
    static final long TickLimitNanos = 10_000_000L;

    private static final int OutlineSegmentSize = 4;
    private static final int OutlineSegmentPerTile = 4;
    private static final int OutlineSegmentSizePerTile = OutlineSegmentSize * OutlineSegmentPerTile;
    private static final int SelectedRectSize = 2;
    private static final int MinSelectedRectIndices = SelectedRectSize * 4;

    private TileEditorRender() {}

    static void checkInvalidation(TileEditorRenderCache cache, TileSet tileSet, List<Tile> selectedTiles) {
        Sprite sprite = tileSet == null ? null : tileSet.tileSetSprite();
        float spriteWidth = 0.0f;
        float spriteHeight = 0.0f;
        if (sprite != null) {
            spriteWidth = sprite.getWidth();
            spriteHeight = sprite.getHeight();
        }
        if (sprite != cache.sprite || spriteWidth != cache.spriteWidth || spriteHeight != cache.spriteHeight) {
            invalidateChecker(cache);
            invalidateOutlines(cache);
            invalidateSelected(cache);
            cache.sprite = sprite;
            cache.spriteWidth = spriteWidth;
            cache.spriteHeight = spriteHeight;
        }
        if (tileSet == null || sprite == null) {
            cache.checkerReady = true;
            cache.checkerDirty = false;
            cache.outlineReady = true;
            cache.outlineDirty = false;
            cache.selectedReady = true;
            cache.selectedDirty = false;
            cache.outlineCount = 0;
            cache.selectedCount = 0;
            cache.hasFirstSelected = false;
            return;
        }
        Vector2i gridSize = tileSet.gridSize();
        Vector2i startPos = tileSet.startPosition();
        int tileCount = tileSet.tileCount();
        boolean gridChanged = gridSize.x != cache.gridX || gridSize.y != cache.gridY || startPos.x != cache.startX || startPos.y != cache.startY;
        boolean tileSetChanged = tileCount != cache.tileCount;
        if (gridChanged || tileSetChanged) {
            invalidateOutlines(cache);
            invalidateSelected(cache);
        }
        cache.gridX = gridSize.x;
        cache.gridY = gridSize.y;
        cache.startX = startPos.x;
        cache.startY = startPos.y;
        cache.tileCount = tileCount;
        int selectionSize = selectedTiles == null ? 0 : selectedTiles.size();
        Tile firstSelection = selectionSize == 0 ? null : selectedTiles.getFirst();
        if (cache.selectionDirty || selectionSize != cache.selectionSize || firstSelection != cache.firstSelection) invalidateSelected(cache);
        cache.selectionSize = selectionSize;
        cache.firstSelection = firstSelection;
        cache.selectionDirty = false;
    }

    static boolean notReady(TileEditorRenderCache cache) {
        return !cache.checkerReady || !cache.outlineReady || !cache.selectedReady || cache.checkerDirty || cache.outlineDirty || cache.selectedDirty;
    }

    static void tickBuild(TileEditorRenderCache cache, TileSet tileSet, List<Tile> selectedTiles) {
        long start = System.nanoTime();
        if (cache.checkerDirty) buildChecker(cache);
        if (cache.outlineDirty && System.nanoTime() - start < TickLimitNanos) buildOutline(cache, tileSet, start);
        if (cache.selectedDirty && System.nanoTime() - start < TickLimitNanos) buildSelected(cache, selectedTiles, start);
    }

    static void markSelectionDirty(TileEditorRenderCache cache) {
        cache.selectionDirty = true;
    }

    static void drawCheckerboard(TileEditorRenderCache cache, ImVec2 cursorScreenPos, float zoom) {
        if (!cache.checkerReady) return;
        if (cache.checkerCountX <= 0 || cache.checkerCountY <= 0) return;
        ImDrawList drawList = ImGui.getWindowDrawList();
        float squareSize = BgSquareSize * zoom;
        float width = cache.checkerImageWidth * zoom;
        float height = cache.checkerImageHeight * zoom;
        ImVec2 rectMin = cache.tmpMin;
        ImVec2 rectMax = cache.tmpMax;
        float x, y, w, h;
        boolean light;
        int color;
        for (int row = 0; row < cache.checkerCountY; row++) {
            for (int column = 0; column < cache.checkerCountX; column++) {
                light = (row + column) % 2 == 0;
                color = light ? LightSquareColor : DarkSquareColor;
                x = cursorScreenPos.x + column * squareSize;
                y = cursorScreenPos.y + row * squareSize;
                w = Math.min(squareSize, width - column * squareSize);
                h = Math.min(squareSize, height - row * squareSize);
                rectMin.set(x, y);
                rectMax.set(x + w, y + h);
                drawList.addRectFilled(rectMin, rectMax, color);
            }
        }
    }

    static void drawTileOutlines(TileEditorRenderCache cache, ImVec2 cursorScreenPos, float zoom) {
        if (!cache.outlineReady || cache.outlineCount == 0) return;
        ImDrawList drawList = ImGui.getWindowDrawList();
        ImVec2 startPoint = cache.tmpMin;
        ImVec2 endPoint = cache.tmpMax;
        for (int i = 0; i < cache.outlineCount; i++) {
            int index = i * OutlineSegmentSize;
            startPoint.set(cursorScreenPos.x + cache.outlineSegments[index] * zoom, cursorScreenPos.y + cache.outlineSegments[index + 1] * zoom);
            endPoint.set(cursorScreenPos.x + cache.outlineSegments[index + 2] * zoom, cursorScreenPos.y + cache.outlineSegments[index + 3] * zoom);
            drawList.addLine(startPoint, endPoint, TileHighlightColor);
        }
    }

    static void drawSelectedTiles(TileEditorRenderCache cache, ImVec2 cursorScreenPos, float zoom) {
        if (!cache.selectedReady || !cache.hasFirstSelected) return;
        ImDrawList drawList = ImGui.getWindowDrawList();
        float gridW = cache.gridX * zoom;
        float gridH = cache.gridY * zoom;
        ImVec2 min = cache.tmpMin;
        ImVec2 max = cache.tmpMax;
        int index;
        float x, y;
        for (int i = 0; i < cache.selectedCount; i++) {
            index = i * SelectedRectSize;
            x = cursorScreenPos.x + cache.selectedRects[index] * zoom;
            y = cursorScreenPos.y + cache.selectedRects[index + 1] * zoom;
            min.set(x, y);
            max.set(x + gridW, y + gridH);
            drawList.addRectFilled(min, max, SelectedTileFillColor, 0.0f);
            drawList.addRect(min, max, SelectedTileColor, 0.0f, SelectedThickness);
        }
        float firstX = cursorScreenPos.x + cache.firstSelectedX * zoom;
        float firstY = cursorScreenPos.y + cache.firstSelectedY * zoom;
        min.set(firstX, firstY);
        max.set(firstX + gridW, firstY + gridH);
        drawList.addRectFilled(min, max, FirstSelectedTileFillColor, 0.0f);
        drawList.addRect(min, max, FirstSelectedTileColor, 0.0f, SelectedThickness);
    }

    static void drawLoadingPlaceholder() {
        String text = "Loading...";
        ImVec2 avail = ImGui.getContentRegionAvail();
        ImVec2 textSize = ImGui.calcTextSize(text);
        float offsetX = Math.max(0.0f, (avail.x - textSize.x) * 0.5f);
        float offsetY = Math.max(0.0f, (avail.y - textSize.y) * 0.5f);
        ImGui.setCursorPos(ImGui.getCursorPosX() + offsetX, ImGui.getCursorPosY() + offsetY);
        ImGui.text(text);
    }

    private static void invalidateChecker(TileEditorRenderCache cache) {
        cache.checkerDirty = true;
        cache.checkerReady = false;
    }

    private static void invalidateOutlines(TileEditorRenderCache cache) {
        cache.outlineDirty = true;
        cache.outlineReady = false;
        cache.outlineBuildCursor = 0;
        cache.outlineBuildList = null;
        cache.outlineBuildSet = null;
    }

    private static void invalidateSelected(TileEditorRenderCache cache) {
        cache.selectedDirty = true;
        cache.selectedReady = false;
        cache.selectedBuildCursor = 0;
        cache.selectedBuildList = null;
        cache.selectedBuildFirstCoordinate = null;
    }

    private static void buildChecker(TileEditorRenderCache cache) {
        float width = cache.spriteWidth;
        float height = cache.spriteHeight;
        if (width <= 0.0f || height <= 0.0f) {
            cache.checkerCountX = 0;
            cache.checkerCountY = 0;
            cache.checkerImageWidth = 0;
            cache.checkerImageHeight = 0;
        } else {
            cache.checkerCountX = (int) Math.ceil(width / BgSquareSize);
            cache.checkerCountY = (int) Math.ceil(height / BgSquareSize);
            cache.checkerImageWidth = width;
            cache.checkerImageHeight = height;
        }
        cache.checkerReady = true;
        cache.checkerDirty = false;
    }

    private static void initOutlineBuild(TileEditorRenderCache cache, TileSet tileSet) {
        HashSet<Vector2i> coordinates = tileSet.getTileCoordinates();
        cache.outlineBuildSet = coordinates;
        cache.outlineBuildList = coordinates.toArray(new Vector2i[0]);
        cache.outlineBuildCursor = 0;
        cache.outlineSegments = new float[Math.max(OutlineSegmentSizePerTile, coordinates.size() * OutlineSegmentSizePerTile)];
        cache.outlineCount = 0;
    }

    private static void buildOutline(TileEditorRenderCache cache, TileSet tileSet, long start) {
        if (cache.outlineBuildList == null) initOutlineBuild(cache, tileSet);
        Vector2i[] outlineBuilds = cache.outlineBuildList;
        HashSet<Vector2i> outlines = cache.outlineBuildSet;
        int gridX = cache.gridX;
        int gridY = cache.gridY;
        int startX = cache.startX;
        int startY = cache.startY;
        Vector2i probe = new Vector2i();
        while (cache.outlineBuildCursor < outlineBuilds.length) {
            Vector2i tile = outlineBuilds[cache.outlineBuildCursor++];
            float x = startX + tile.x * gridX;
            float y = startY + tile.y * gridY;
            float right = x + gridX;
            float bottom = y + gridY;
            if (!outlines.contains(probe.set(tile.x, tile.y - 1))) appendSegment(cache, x, y, right, y);
            if (!outlines.contains(probe.set(tile.x + 1, tile.y))) appendSegment(cache, right, y, right, bottom);
            if (!outlines.contains(probe.set(tile.x, tile.y + 1))) appendSegment(cache, x, bottom, right, bottom);
            if (!outlines.contains(probe.set(tile.x - 1, tile.y))) appendSegment(cache, x, y, x, bottom);
            if (System.nanoTime() - start > TickLimitNanos) return;
        }
        cache.outlineReady = true;
        cache.outlineDirty = false;
        cache.outlineBuildList = null;
        cache.outlineBuildSet = null;
        Logger.debug(String.format("Tile outline build complete with %d segments from %d tile(s)", cache.outlineCount, outlineBuilds.length));
    }

    private static void appendSegment(TileEditorRenderCache cache, float startX, float startY, float endX, float endY) {
        int required = (cache.outlineCount + 1) * OutlineSegmentSize;
        if (required > cache.outlineSegments.length) {
            int newSize = Math.max(required, Math.max(OutlineSegmentSizePerTile, cache.outlineSegments.length * 2));
            float[] outlines = new float[newSize];
            System.arraycopy(cache.outlineSegments, 0, outlines, 0, cache.outlineCount * OutlineSegmentSize);
            cache.outlineSegments = outlines;
        }
        int index = cache.outlineCount * OutlineSegmentSize;
        cache.outlineSegments[index] = startX;
        cache.outlineSegments[index + 1] = startY;
        cache.outlineSegments[index + 2] = endX;
        cache.outlineSegments[index + 3] = endY;
        cache.outlineCount++;
    }

    private static void buildSelected(TileEditorRenderCache cache, List<Tile> selectedTiles, long start) {
        if (cache.selectedBuildList == null) initSelectedBuild(cache, selectedTiles);
        Tile[] selectedBuilds = cache.selectedBuildList;
        Vector2i firstCoordinate = cache.selectedBuildFirstCoordinate;
        int gridX = cache.gridX;
        int gridY = cache.gridY;
        int startX = cache.startX;
        int startY = cache.startY;
        while (cache.selectedBuildCursor < selectedBuilds.length) {
            Tile tile = selectedBuilds[cache.selectedBuildCursor++];
            if (tile == null || tile.setCoordinate == null) continue;
            if (tile.setCoordinate.equals(firstCoordinate)) continue;
            float x = startX + tile.setCoordinate.x * gridX;
            float y = startY + tile.setCoordinate.y * gridY;
            appendSelectedRect(cache, x, y);
            if (System.nanoTime() - start > TickLimitNanos) return;
        }
        cache.selectedReady = true;
        cache.selectedDirty = false;
        cache.selectedBuildList = null;
        cache.selectedBuildFirstCoordinate = null;
    }

    private static void initSelectedBuild(TileEditorRenderCache cache, List<Tile> selectedTiles) {
        int size = selectedTiles == null ? 0 : selectedTiles.size();
        cache.selectedBuildList = size == 0 ? new Tile[0] : selectedTiles.toArray(new Tile[0]);
        cache.selectedBuildCursor = 0;
        cache.selectedRects = new float[Math.max(MinSelectedRectIndices, size * SelectedRectSize)];
        cache.selectedCount = 0;
        cache.hasFirstSelected = false;
        if (size == 0) return;
        Tile first = cache.selectedBuildList[0];
        if (first == null || first.setCoordinate == null) {
            cache.selectedBuildCursor = 1;
            return;
        }
        cache.selectedBuildFirstCoordinate = new Vector2i(first.setCoordinate);
        cache.firstSelectedX = cache.startX + first.setCoordinate.x * cache.gridX;
        cache.firstSelectedY = cache.startY + first.setCoordinate.y * cache.gridY;
        cache.hasFirstSelected = true;
        cache.selectedBuildCursor = 1;
    }

    private static void appendSelectedRect(TileEditorRenderCache cache, float x, float y) {
        int required = (cache.selectedCount + 1) * SelectedRectSize;
        if (required > cache.selectedRects.length) {
            int newSize = Math.max(required, Math.max(MinSelectedRectIndices, cache.selectedRects.length * 2));
            float[] selectedRects = new float[newSize];
            System.arraycopy(cache.selectedRects, 0, selectedRects, 0, cache.selectedCount * SelectedRectSize);
            cache.selectedRects = selectedRects;
        }
        int index = cache.selectedCount * SelectedRectSize;
        cache.selectedRects[index] = x;
        cache.selectedRects[index + 1] = y;
        cache.selectedCount++;
    }
}
