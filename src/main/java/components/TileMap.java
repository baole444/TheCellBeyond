package components;

import imgui.ImGui;
import org.joml.Vector2f;
import render.texture.TileSet;
import utility.WorldUnit;

public class TileMap extends SpatialComponent {
    private volatile TileSet tileSet;

    private volatile transient boolean isTileSetDirty = true;

    @Override
    protected void additionalImGuiLogic() {
        ImGui.textDisabled("More control later");
    }

    @Override
    protected void additionalDirtyFlagLogic() {
        isTileSetDirty = true;
    }

    public void setTileSetDirty(boolean needsUpdate) {
        isTileSetDirty = needsUpdate;
        if (!needsUpdate && tileSet != null) tileSet.rendererUpdated();
    }

    public Vector2f getTileSetSize() {
        if (tileSet == null) return new Vector2f(1.0f);

        return new Vector2f(tileSet.getWidth(), tileSet.getHeight());
    }

    public Vector2f getSpriteSizeAsWorldUnit() {
        return WorldUnit.pixelToWorld(getTileSetSize());
    }
}
