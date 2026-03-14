package render;

import render.commands.RenderCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RenderNode {
    public final Renderable nodeOwner;
    public RenderNode renderingParent;
    public final List<RenderNode> renderingChildren = new CopyOnWriteArrayList<>();
    public RenderCommand commandHeader = null;

    public RenderNode(Renderable nodeOwner, RenderNode renderingParent) {
        this.nodeOwner = nodeOwner;
        this.renderingParent = renderingParent;
    }

    public void addChild(RenderNode child) {
        renderingChildren.add(child);
        child.renderingParent = this;
    }

    public void removeChild(RenderNode child) {
        renderingChildren.remove(child);
        child.renderingParent = null;
    }
}
