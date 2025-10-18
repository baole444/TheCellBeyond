package render;

import components.TileMap;
import org.jetbrains.annotations.NotNull;

public class TileBatch implements Comparable<TileBatch> {
    private TileMap tileMap;
    private int zIndex;
    private float[] vertices;
    private int[] indices;

    @Override
    public int compareTo(@NotNull TileBatch o) {
        return 0;
    }
}
