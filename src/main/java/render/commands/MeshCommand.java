package render.commands;

import TheCellBeyond.TileMap;
import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.util.Map;

public class MeshCommand extends RenderCommand {
    public Map<Vector2i, TileMap.TilePlacement> tilePlacements = null;
    public ResourceID tileSetRID = null;
    public final Vector4f modulate = new Vector4f(1.0f);
    public final Vector2f position = new Vector2f();
    public float rotationDegrees = 0.0f;
    public final Vector2f scale = new Vector2f(1.0f);

    MeshCommand() {
        super(CommandType.Mesh);
    }

    public static MeshCommand acquire() {
        return RenderCommandPool.get().acquire(MeshCommand.class, MeshCommand::new);
    }

    public static void release(MeshCommand command) {
        RenderCommandPool.get().release(command);
    }

    @Override
    protected void reset() {
        super.reset();
        tilePlacements = null;
        tileSetRID = null;
        modulate.set(1.0f);
        position.zero();
        rotationDegrees = 0.0f;
        scale.set(1.0f);
    }
}
