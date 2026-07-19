package editor.widgets;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A scrollable, read only text view that renders coloured rows and supports character level selection,
 * clipboard copy and a copy context menu.
 * <p>
 * Callers supply the rows each frame to the widget and are responsible for preserving {@link Row} instances across frames,
 * so selection and the layout cache stay valid.
 */
public final class SelectableTextView {
    /**
     * A logical line of text drawn with a single colour. The text may contain {@code '\n'}, which is treated as a hard line break.
     * Object identity anchors selection across frames, so callers should reuse instances for unchanged content.
     */
    public static final class Row {
        public final String text;
        public final int color;

        public Row(String text, int color) {
            this.text = text == null ? "" : text;
            this.color = color;
        }
    }

    private static final class RowLayout {
        final int[][] segments;
        final String[] segmentText;
        final float maxWidth;

        RowLayout(int[][] segments, String[] segmentText, float maxWidth) {
            this.segments = segments;
            this.segmentText = segmentText;
            this.maxWidth = maxWidth;
        }
    }

    private record Hit(int rowIndex, int charOffset) {}
    private record SelectionSpan(int startRow, int startChar, int endRow, int endChar, int endVisual) {}
    /**
     * Default grey colour shown behind a hovered rowIndex.
     */
    public static final int DefaultHoverColor = ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 0.15f);
    /**
     * Default light grey shown behind a selection.
     */
    public static final int DefaultSelectionColor = ImGui.colorConvertFloat4ToU32(0.7f, 0.7f, 0.7f, 0.35f);
    /**
     * Default override colour for selected text, keeping it readable over the selection highlight.
     */
    public static final int DefaultSelectedTextColor = ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean wrap = true;
    private boolean stickToBottom = false;
    private int hoverColor = DefaultHoverColor;
    private int selectionColor = DefaultSelectionColor;
    private int selectedTextColor = DefaultSelectedTextColor;
    private final IdentityHashMap<Row, RowLayout> layoutCache = new IdentityHashMap<>();
    private float cachedWrapWidth = -1.0f;
    private Row anchorRow, caretRow, contextRow;
    private int anchorChar, caretChar;
    private boolean selecting;

    private List<Row> frameRows = List.of();
    private final IdentityHashMap<Row, Integer> frameIndex = new IdentityHashMap<>();
    private RowLayout[] frameLayouts = new RowLayout[0];
    private int[] frameFirstVisual = new int[0];
    private int frameTotalVisual;
    private float frameContentWidth;
    private float frameMaxWidth;
    private float frameLineHeight;
    private float frameRectMinX, frameRectMinY;

    private Row[] rowSnapshot = new Row[0];
    private int rowSnapshotCount = -1;

    public boolean wrap() {
        return wrap;
    }

    public void wrap(boolean wrap) {
        this.wrap = wrap;
    }

    public boolean stickToBottom() {
        return stickToBottom;
    }

    public void stickToBottom(boolean stickToBottom) {
        this.stickToBottom = stickToBottom;
    }

    public void hoverColor(int packedColor) {
        this.hoverColor = packedColor;
    }

    public void selectionColor(int packedColor) {
        this.selectionColor = packedColor;
    }

    public void selectedTextColor(int packedColor) {
        this.selectedTextColor = packedColor;
    }

    public void clearSelection() {
        anchorRow = null;
        caretRow = null;
        anchorChar = 0;
        caretChar = 0;
        selecting = false;
    }

    /**
     * Check if there is a selection in the supplied rows.
     * @return true if there is selection
     */
    public boolean hasSelection() {
        if (anchorRow == null || caretRow == null) return false;
        if (anchorRow == caretRow && anchorChar == caretChar) return false;
        return frameIndex.containsKey(anchorRow) && frameIndex.containsKey(caretRow);
    }

    /**
     * Get the text currently being selected.
     * @return the selected string, joined with {@code '\n'} across rows
     */
    public String selectedText() {
        SelectionSpan sel = selectionSpan();
        if (sel == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int r = sel.startRow(); r <= sel.endRow(); r++) {
            String text = frameRows.get(r).text;
            int from = r == sel.startRow() ? sel.startChar() : 0;
            int to = r == sel.endRow() ? sel.endChar() : text.length();
            sb.append(text, Math.clamp(from, 0, text.length()), Math.clamp(to, 0, text.length()));
            if (r < sel.endRow()) sb.append('\n');
        }
        return sb.toString();
    }

    public void render(String id, List<Row> rows) {
        render(id, rows, null);
    }

    /**
     * Render the rows inside a scrolling child filling the available region.
     * @param id unique widget id
     * @param rows the rows ton display, instances should be stable across frames
     * @param extraContextMenu optional callback invoked inside the right click context menu
     */
    public void render(String id, List<Row> rows, Consumer<Row> extraContextMenu) {
        if (rows == null) rows = List.of();
        int windowFlags = wrap ? ImGuiWindowFlags.None : ImGuiWindowFlags.HorizontalScrollbar;
        if (!ImGui.beginChild(id, 0.0f, 0.0f, ImGuiChildFlags.Borders, windowFlags)) {
            ImGui.endChild();
            return;
        }
        float viewportHeight = ImGui.getContentRegionAvailY();
        prepareFrame(rows);
        ImGui.invisibleButton(id + "_STV_hovering_region", frameContentWidth, Math.max(frameTotalVisual * frameLineHeight, frameLineHeight));
        boolean hovered = ImGui.isItemHovered();
        ImVec2 rectMin = ImGui.getItemRectMin();
        frameRectMinX = rectMin.x;
        frameRectMinY = rectMin.y;
        handleInput(rows, hovered, viewportHeight);
        validateSelection();
        drawContent(rows, hovered, viewportHeight);
        applyStickToBottom();
        handleCopyShortcut();
        handleContextMenu(id, extraContextMenu);
        ImGui.endChild();
    }

    private void prepareFrame(List<Row> rows) {
        frameRows = rows;
        frameLineHeight = ImGui.getTextLineHeight();
        float availableWidth = ImGui.getContentRegionAvailX();
        float wrapWidth = wrap ? Math.max(availableWidth, 1.0f) : Float.MAX_VALUE;
        boolean layoutDirty = wrapWidth != cachedWrapWidth;
        if (layoutDirty) {
            layoutCache.clear();
            cachedWrapWidth = wrapWidth;
        }
        if (layoutDirty || !rowUnchanged(rows)) {
            rebuildFrame(rows, wrapWidth);
            snapshotRows(rows);
        }
        frameContentWidth = wrap ? availableWidth : Math.max(availableWidth, frameMaxWidth);
    }

    private void rebuildFrame(List<Row> rows, float wrapWidth) {
        int rowCount = rows.size();
        frameIndex.clear();
        if (frameLayouts.length < rowCount) {
            frameLayouts = new RowLayout[rowCount];
            frameFirstVisual = new int[rowCount];
        }
        int visualCount = 0;
        float maxWidth = 0.0f;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Row row = rows.get(rowIndex);
            frameIndex.put(row, rowIndex);
            RowLayout layout = layoutFor(row, wrapWidth);
            frameLayouts[rowIndex] = layout;
            frameFirstVisual[rowIndex] = visualCount;
            visualCount += layout.segments.length;
            if (layout.maxWidth > maxWidth) maxWidth = layout.maxWidth;
        }
        layoutCache.keySet().retainAll(frameIndex.keySet());
        frameTotalVisual = visualCount;
        frameMaxWidth = maxWidth;
    }

    private boolean rowUnchanged(List<Row> rows) {
        int rowCount = rows.size();
        if (rowCount != rowSnapshotCount) return false;
        for (int i = 0; i < rowCount; i++) {
            if (rows.get(i) != rowSnapshot[i]) return false;
        }
        return true;
    }

    private RowLayout layoutFor(Row row, float wrapWidth) {
        RowLayout layout = layoutCache.get(row);
        if (layout != null) return layout;
        layout = computeLayout(row.text, wrapWidth);
        layoutCache.put(row, layout);
        return layout;
    }

    private void snapshotRows(List<Row> rows) {
        int rowCount = rows.size();
        if (rowSnapshot.length < rowCount) rowSnapshot = new Row[rowCount];
        for (int i = 0; i < rowCount; i++) rowSnapshot[i] = rows.get(i);
        for (int i = rowCount; i < rowSnapshotCount; i++) rowSnapshot[i] = null;
        rowSnapshotCount = rowCount;
    }

    private RowLayout computeLayout(String text, float wrapWidth) {
        List<int[]> segments = new ArrayList<>();
        int length = text.length();
        int position = 0;
        while (position <= length) {
            int newline = text.indexOf('\n', position);
            int hardEnd = newline < 0 ? length : newline;
            if (!wrap || position == hardEnd) segments.add(new int[]{position, hardEnd});
            else softWrap(text, position, hardEnd, wrapWidth, segments);
            position = hardEnd + 1;
        }
        int[][] segmentArray = segments.toArray(new int[0][]);
        String[] segmentTexts = new String[segmentArray.length];
        float maxWidth = 0.0f;
        for (int segment = 0; segment < segmentArray.length; segment++) {
            String segmentText = text.substring(segmentArray[segment][0], segmentArray[segment][1]);
            segmentTexts[segment] = segmentText;
            float width = segmentText.isEmpty() ? 0.0f : ImGui.calcTextSizeX(segmentText);
            if (width > maxWidth) maxWidth = width;
        }
        return new RowLayout(segmentArray, segmentTexts, maxWidth);
    }

    private void softWrap(String text, int start, int end, float maxWidth, List<int[]> segments) {
        int lineStart = start;
        int lastSpace = -1;
        int index = start;
        while (index < end) {
            char c = text.charAt(index);
            float width = columnX(text, lineStart, index + 1);
            if (width > maxWidth && index > lineStart) {
                boolean wordBreak = lastSpace > lineStart;
                int breakAt = wordBreak ? lastSpace : index;
                segments.add(new int[]{lineStart, breakAt});
                lineStart = wordBreak ? lastSpace + 1 : index;
                lastSpace = -1;
                index = lineStart;
                continue;
            }
            if (c == ' ' || c == '\t') lastSpace = index;
            index++;
        }
        segments.add(new int[]{lineStart, end});
    }

    private void handleInput(List<Row> rows, boolean hovered, float viewportHeight) {
        if (rows.isEmpty()) return;
        if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            Hit hit = hitTest(ImGui.getMousePosX(), ImGui.getMousePosY());
            anchorRow = caretRow = rows.get(hit.rowIndex);
            anchorChar = caretChar = hit.charOffset;
            selecting = true;
        }
        if (selecting && ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            Hit hit = hitTest(ImGui.getMousePosX(), ImGui.getMousePosY());
            caretRow = rows.get(hit.rowIndex());
            caretChar = hit.charOffset();
            autoScroll(ImGui.getMousePosY(), viewportHeight);
        } else selecting = false;
        if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            Hit hit = hitTest(ImGui.getMousePosX(), ImGui.getMousePosY());
            contextRow = rows.get(hit.rowIndex());
        }
    }

    private Hit hitTest(float mouseX, float mouseY) {
        int visualRow = (int) Math.floor((mouseY - frameRectMinY) / frameLineHeight);
        visualRow = Math.clamp(visualRow, 0, frameTotalVisual - 1);
        int rowIndex = rowOfVisual(visualRow);
        RowLayout layout = frameLayouts[rowIndex];
        int segment = Math.clamp(visualRow - frameFirstVisual[rowIndex], 0, layout.segments.length - 1);
        int segmentStart = layout.segments[segment][0];
        int segmentEnd = layout.segments[segment][1];
        int charOffset = hitChar(frameRows.get(rowIndex).text, segmentStart, segmentEnd, mouseX - frameRectMinX);
        return new Hit(rowIndex, charOffset);
    }

    private int hitChar(String text, int segmentStart, int segmentEnd, float localX) {
        if (localX <= 0.0f) return segmentStart;
        if (localX >= columnX(text, segmentStart, segmentEnd)) return segmentEnd;
        int low = segmentStart;
        int high = segmentEnd;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (columnX(text, segmentStart, mid) > localX) high = mid - 1;
            else low = mid;
        }
        if (low >= segmentEnd) return low;
        float pastWidth = columnX(text, segmentStart, low);
        float newWidth = columnX(text, segmentStart, low + 1);
        return Math.abs(localX - newWidth) < Math.abs(localX - pastWidth) ? low + 1 : low;
    }

    private void drawContent(List<Row> rows, boolean hovered, float viewportHeight) {
        if (rows.isEmpty()) return;
        float scrollY = ImGui.getScrollY();
        int firstVisible = Math.max(0, (int) Math.floor(scrollY / frameLineHeight) - 1);
        int lastVisible = Math.min(frameTotalVisual - 1, (int) Math.ceil((scrollY + viewportHeight) / frameLineHeight) + 1);
        int hoverVisual = hovered ? (int) Math.floor((ImGui.getMousePosY() - frameRectMinY) / frameLineHeight) : -1;
        SelectionSpan selection = selectionSpan();
        ImDrawList drawList = ImGui.getWindowDrawList();
        for (int rowIndex = rowOfVisual(firstVisible); rowIndex < rows.size(); rowIndex++) {
            if (frameFirstVisual[rowIndex] > lastVisible) break;
            drawRow(drawList, rows.get(rowIndex), rowIndex, firstVisible, lastVisible, hoverVisual, selection);
        }
    }

    private void drawRow(ImDrawList drawList, Row row, int rowIndex, int firstVisible, int lastVisible, int hoverVisual, SelectionSpan selection) {
        RowLayout layout = frameLayouts[rowIndex];
        int rowFirstVisible = frameFirstVisual[rowIndex];
        boolean rowHovered = hoverVisual >= rowFirstVisible && hoverVisual <= rowFirstVisible + layout.segments.length - 1;
        boolean rowSelected = selection != null && rowIndex >= selection.startRow && rowIndex <= selection.endRow;
        int rowFrom = rowSelected && rowIndex == selection.startRow ? selection.startChar : 0;
        int rowTo = rowSelected && rowIndex == selection.endRow ? selection.endChar : row.text.length();
        for (int segment = 0; segment < layout.segments.length; segment++) {
            int visualRow = rowFirstVisible + segment;
            if (visualRow < firstVisible || visualRow > lastVisible) continue;
            float y = frameRectMinY + visualRow * frameLineHeight;
            int segmentStart = layout.segments[segment][0];
            int segmentEnd = layout.segments[segment][1];
            int selectedFrom = rowSelected ? Math.clamp(rowFrom, segmentStart, segmentEnd) : segmentStart;
            int selectedTo = rowSelected ? Math.clamp(rowTo, segmentStart, segmentEnd) : segmentStart;
            boolean continues = rowSelected && visualRow < selection.endVisual;
            if (rowHovered) drawList.addRectFilled(frameRectMinX, y, frameRectMinX + frameContentWidth, y + frameLineHeight, hoverColor);
            drawSelectionBackground(drawList, row.text, segmentStart, selectedFrom, selectedTo, continues, y);
            drawSegmentText(drawList, row, layout.segmentText[segment], segmentStart, selectedFrom, selectedTo, y);
        }
    }

    private void drawSelectionBackground(ImDrawList drawList, String text, int segmentStart, int selectedFrom, int selectedTo, boolean continues, float y) {
        if (selectedFrom >= selectedTo && !continues) return;
        float startX = frameRectMinX + columnX(text, segmentStart, selectedFrom);
        float endX = continues ? frameRectMinX + frameContentWidth : frameRectMinX + columnX(text, segmentStart, selectedTo);
        if (endX > startX) drawList.addRectFilled(startX, y, endX, y + frameLineHeight, selectionColor);
    }

    private void drawSegmentText(ImDrawList drawList, Row row, String text, int segmentStart, int selectedFrom, int selectedTo, float y) {
        if (text.isEmpty()) return;
        if (selectedFrom >= selectedTo) {
            drawList.addText(frameRectMinX, y, row.color, text);
            return;
        }
        int segmentEnd = segmentStart + text.length();
        if (selectedFrom > segmentStart) drawList.addText(frameRectMinX, y, row.color, text.substring(0, selectedFrom - segmentStart));
        drawList.addText(frameRectMinX + columnX(row.text, segmentStart, selectedFrom), y, selectedTextColor, text.substring(selectedFrom - segmentStart, selectedTo - segmentStart));
        if (selectedTo < segmentEnd) drawList.addText(frameRectMinX + columnX(row.text, segmentStart, selectedTo), y, row.color, text.substring(selectedTo - segmentStart));
    }

    private SelectionSpan selectionSpan() {
        if (anchorRow == null || caretRow == null) return null;
        Integer anchorIndex = frameIndex.get(anchorRow);
        Integer caretIndex = frameIndex.get(caretRow);
        if (anchorIndex == null || caretIndex == null) return null;
        boolean anchorFirst = anchorIndex < caretIndex || (anchorIndex.equals(caretIndex) && anchorChar <= caretChar);
        int startRow = anchorFirst ? anchorIndex : caretIndex;
        int startChar = anchorFirst ? anchorChar : caretChar;
        int endRow = anchorFirst ? caretIndex : anchorIndex;
        int endChar = anchorFirst ? caretChar : anchorChar;
        if (startRow == endRow && startChar == endChar) return null;
        int endVisual = frameFirstVisual[endRow] + segmentIndexOf(frameLayouts[endRow], endChar);
        return new SelectionSpan(startRow, startChar, endRow, endChar, endVisual);
    }

    private void autoScroll(float mouseY, float viewportHeight) {
        float top = frameRectMinY + ImGui.getScrollY();
        float bottom = top + viewportHeight;
        if (mouseY < top + frameLineHeight) {
            ImGui.setScrollY(Math.max(0.0f, ImGui.getScrollY() - frameLineHeight));
            return;
        }
        if (mouseY > bottom - frameLineHeight) ImGui.setScrollY(Math.min(ImGui.getScrollMaxY(), ImGui.getScrollY() + frameLineHeight));
    }

    private void validateSelection() {
        if (anchorRow == null || caretRow == null) return;
        if (!frameIndex.containsKey(anchorRow) || !frameIndex.containsKey(caretRow)) clearSelection();
    }

    private void applyStickToBottom() {
        if (!stickToBottom || selecting) return;
        if (ImGui.getScrollY() >= ImGui.getScrollMaxY() - 1.0f) ImGui.setScrollHereY(1.0f);
    }

    private void handleCopyShortcut() {
        if (!ImGui.isWindowFocused()) return;
        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.C, false) && hasSelection()) ImGui.setClipboardText(selectedText());
    }

    private void handleContextMenu(String id, Consumer<Row> extraContextMenu) {
        if (!ImGui.beginPopupContextItem(id + "_STV_ContextMenu_")) return;
        if (contextRow != null && ImGui.menuItem("Copy line##" + id + "_STV_CopyLine")) ImGui.setClipboardText(contextRow.text);
        if (hasSelection() && ImGui.menuItem("Copy selection##" + id + "_STV_CopySelection")) ImGui.setClipboardText(selectedText());
        if (extraContextMenu != null) extraContextMenu.accept(contextRow);
        ImGui.endPopup();
    }

    private int rowOfVisual(int visualRow) {
        int low = 0;
        int high = frameRows.size() - 1;
        int result = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (frameFirstVisual[mid] > visualRow) {
                high = mid - 1;
                continue;
            }
            result = mid;
            low = mid + 1;
        }
        return result;
    }

    private static int segmentIndexOf(RowLayout layout, int charOffset) {
        for (int i = 0; i < layout.segments.length; i++) {
            if (charOffset <= layout.segments[i][1]) return i;
        }
        return layout.segments.length - 1;
    }

    private static float columnX(String text, int segmentStart, int charOffset) {
        if (charOffset <= segmentStart) return 0.0f;
        return ImGui.calcTextSizeX(text.substring(segmentStart, charOffset));
    }

}
