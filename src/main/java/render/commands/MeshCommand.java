package render.commands;

import TheCellBeyond.TileMap;
import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import render.texture.TileSet;

import java.util.Map;

public class MeshCommand extends RenderCommand {
    public Map<Vector2i, TileMap.TilePlacement> tilePlacements = null;
    public TileSet tileSet = null;
    public final Vector4f modulate = new Vector4f(1.0f);

    MeshCommand() {
        super(CommandType.Mesh);
    }

    public static MeshCommand acquire() {
        return RenderCommandPool.get().acquire(MeshCommand.class, MeshCommand::new);
    }

    @Override
    protected void reset() {
        super.reset();
        tilePlacements = null;
        tileSet = null;
        modulate.set(1.0f);
    }
}
