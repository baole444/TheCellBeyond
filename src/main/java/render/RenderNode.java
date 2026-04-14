package render;

import render.commands.RenderCommand;
import render.commands.TransformCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RenderNode {
    public final Renderable owner;
    public RenderNode parent;
    public final List<RenderNode> children = new CopyOnWriteArrayList<>();
    public RenderCommand commandHeader = null;
    public RenderCommand commandTail = null;
    public TransformCommand currentTransform = TransformCommand.createOwned();
    public TransformCommand previousTransform = TransformCommand.createOwned();

    public RenderNode(Renderable nodeOwner, RenderNode renderingParent) {
        this.owner = nodeOwner;
        this.parent = renderingParent;
    }

    public void addChild(RenderNode child) {
        children.add(child);
        child.parent = this;
    }

    public void removeChild(RenderNode child) {
        children.remove(child);
        child.parent = null;
    }
}
