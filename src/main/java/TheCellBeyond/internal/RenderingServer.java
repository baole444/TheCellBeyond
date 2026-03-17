package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.RenderableObject;
import components.Component;
import components.RenderableComponent;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import render.RenderNode;
import render.Renderable;
import render.commands.RenderCommand;
import render.commands.TransformCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RenderingServer implements EngineEventListener {
    private static volatile RenderingServer instance;
    private final CopyOnWriteArrayList<RenderNode> roots = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Renderable, RenderNode> nodes = new ConcurrentHashMap<>();
    private final List<RenderCommand> nodeLinks = new ArrayList<>();
    private RenderCommand chainHead = null;

    private RenderingServer() {
        register();
    }

    public static void init() {
        if (instance == null) instance = new RenderingServer();
    }

    public static RenderingServer get() {
        return instance;
    }

    public void update() {
        refreshCommands(roots);
        nodeLinks.clear();
        RenderCommand[] previousTail = {null};
        chainHead = chainNodes(roots, nodeLinks, previousTail);
    }

    public RenderCommand chainHead() {
        return chainHead;
    }

    public void postFrameClear() {
        for (RenderCommand tail : nodeLinks) tail.next = null;
        nodeLinks.clear();
        chainHead = null;
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        if (!(event instanceof SceneEvent sceneEvent)) return;
        switch (sceneEvent.type) {
            case SceneEntered -> {
                if (sceneEvent.scene == null) return;
                for (GameObject go : sceneEvent.scene.getGameObjects()) registerObject(go);
            }
            case SceneLeaved -> {
                for (RenderNode root : roots) releaseTree(root);
                roots.clear();
                nodes.clear();
            }
            case ObjectAdded -> {
                if (sceneEvent.params.isEmpty()) return;
                if (sceneEvent.params.getFirst() instanceof GameObject go) registerObject(go);
            }
            case ObjectRemoved -> {
                if (sceneEvent.params.isEmpty()) return;
                if (sceneEvent.params.getFirst() instanceof GameObject go) unregisterObject(go);
            }
            case ComponentAdded -> {
                if (sceneEvent.params.isEmpty()) return;
                if (sceneEvent.params.getFirst() instanceof RenderableComponent component) registerComponent(component);
            }
            case ComponentRemoved -> {
                if (sceneEvent.params.isEmpty()) return;
                if (sceneEvent.params.getFirst() instanceof RenderableComponent component) unregisterComponent(component);
            }
            default -> {}
        }
    }

    private void registerObject(GameObject go) {
        if (!(go instanceof RenderableObject renderableObject)) return;
        if (nodes.containsKey(renderableObject)) return;
        RenderNode renderParent = getRenderParent(go);
        RenderNode node = new RenderNode(renderableObject, renderParent);
        nodes.put(renderableObject, node);
        if (renderParent != null) renderParent.addChild(node);
        else roots.add(node);
        for (Component c : go.getComponents()) {
            if (c instanceof RenderableComponent component) registerComponent(component, node);
        }
    }

    private void registerComponent(RenderableComponent renderableComponent) {
        if (!(renderableComponent.gameObject instanceof RenderableObject renderableObject)) return;
        RenderNode objectNode = nodes.get(renderableObject);
        if (objectNode == null) return;
        registerComponent(renderableComponent, objectNode);
    }

    private void registerComponent(RenderableComponent renderableComponent, RenderNode objectNode) {
        if (nodes.containsKey(renderableComponent)) return;
        RenderNode componentNode = new RenderNode(renderableComponent, objectNode);
        nodes.put(renderableComponent, componentNode);
        objectNode.renderingChildren.addFirst(componentNode);
    }

    private void unregisterObject(GameObject go) {
        if (!(go instanceof RenderableObject renderableObject)) return;
        RenderNode node = nodes.remove(renderableObject);
        if (node == null) return;
        releaseCommandChain(node.commandHeader);
        node.commandHeader = null;
        if (node.renderingParent != null) node.renderingParent.removeChild(node);
        else roots.remove(node);
        for (RenderNode child : node.renderingChildren) {
            if (child.nodeOwner instanceof RenderableObject childRenderingParent) {
                unregisterObject(childRenderingParent);
                continue;
            }
            if (child.nodeOwner instanceof RenderableComponent component) unregisterComponent(component);
        }
    }

    private void unregisterComponent(RenderableComponent renderableComponent) {
        RenderNode node = nodes.remove(renderableComponent);
        if (node == null) return;
        releaseCommandChain(node.commandHeader);
        node.commandHeader = null;
        if (node.renderingParent != null) node.renderingParent.removeChild(node);
    }

    private RenderNode getRenderParent(GameObject go) {
        GameObject current = go.getParent();
        while (current != null) {
            if (current instanceof RenderableObject renderableObject) {
                RenderNode node = nodes.get(renderableObject);
                if (node != null) return node;
            }
            current = current.getParent();
        }
        return null;
    }

    private void releaseTree(RenderNode node) {
        releaseCommandChain(node.commandHeader);
        for (RenderNode child : node.renderingChildren) releaseTree(child);
    }

    private static void accumulateTransform(RenderNode node) {
        if (!(node.commandHeader instanceof TransformCommand command)) return;
        RenderNode parent = node.renderingParent;
        while (parent != null) {
            if (parent.nodeOwner instanceof RenderableObject go) {
                command.visible = command.visible && go.visible;
                command.modulate.mul(go.selfModulate);
            }
            parent = parent.renderingParent;
        }
    }

    private static void releaseCommandChain(RenderCommand header) {
        RenderCommand command = header;
        while (command != null) {
            RenderCommand next = command.next;
            command.release();
            command = next;
        }
    }

    private static void refreshCommands(List<RenderNode> nodes) {
        for (RenderNode node : nodes) {
            if (node.nodeOwner.renderDirty()) {
                RenderCommand oldHeader = node.commandHeader;
                node.commandHeader = node.nodeOwner.buildRenderCommand();
                releaseCommandChain(oldHeader);
                node.nodeOwner.renderDirty(false);
                accumulateTransform(node);
            }
            refreshCommands(node.renderingChildren);
        }
    }

    private static RenderCommand chainNodes(List<RenderNode> nodes, List<RenderCommand> nodeLinks, RenderCommand[] previousTail) {
        RenderCommand head = null;
        for (RenderNode node : nodes) {
            if (node.commandHeader == null) {
                RenderCommand childHead = chainNodes(node.renderingChildren, nodeLinks, previousTail);
                if (head == null) head = childHead;
                continue;
            }
            if (head == null) head = node.commandHeader;
            if (previousTail[0] != null) {
                previousTail[0].next = node.commandHeader;
                nodeLinks.add(previousTail[0]);
            }
            RenderCommand tail = node.commandHeader;
            while (tail.next != null) tail = tail.next;
            previousTail[0] = tail;
            chainNodes(node.renderingChildren, nodeLinks, previousTail);
        }
        return head;
    }
}
