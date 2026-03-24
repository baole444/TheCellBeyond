package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.RenderableObject;
import components.Component;
import components.RenderableComponent;
import eventviewer.EngineEventListener;
import eventviewer.event.Event;
import eventviewer.event.SceneEvent;
import org.joml.Vector2f;
import org.joml.Vector4f;
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
    /**
     * Physic interpolation (not coming soon.)
     */
    public static float interpolationFactor = 1.0f;
    private final CopyOnWriteArrayList<RenderNode> roots = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Renderable, RenderNode> nodes = new ConcurrentHashMap<>();
    private final List<RenderCommand> nodeLinks = new ArrayList<>();
    private final List<RenderCommand> cloneChainHeads = new ArrayList<>();
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
        chainHead = chainNodes(roots, nodeLinks, previousTail, cloneChainHeads);
    }

    public RenderCommand chainHead() {
        return chainHead;
    }

    public void postFrameClear() {
        for (RenderCommand tail : nodeLinks) tail.next = null;
        nodeLinks.clear();
        for (RenderCommand head : cloneChainHeads) releaseCommandChain(head);
        cloneChainHeads.clear();
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
        node.commandTail = null;
        releaseTransform(node);
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
        node.commandTail = null;
        releaseTransform(node);
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
        releaseTransform(node);
        for (RenderNode child : node.renderingChildren) releaseTree(child);
    }

    private static void accumulateTransform(RenderNode node, TransformCommand command) {
        RenderNode parent = node.renderingParent;
        while (parent != null) {
            command.visible = command.visible && parent.nodeOwner.visible();
            Vector4f mod = parent.nodeOwner.selfModulate();
            if (mod != null) command.modulate.mul(mod);
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
                releaseCommandChain(node.commandHeader);
                if (node.previousTransform != null) node.previousTransform.release();
                node.previousTransform = node.transform;
                node.transform = node.nodeOwner.buildTransformCommand();
                node.commandHeader = node.nodeOwner.buildRenderCommand();
                node.commandTail = node.commandHeader;
                if (node.commandTail != null) while (node.commandTail.next != null) node.commandTail = node.commandTail.next;
                if (node.transform != null) accumulateTransform(node, node.transform);
                TransformCommand resolved = resolveTransform(node);
                applyTransform(node.commandHeader, resolved);
                node.nodeOwner.renderDirty(false);
            }
            refreshCommands(node.renderingChildren);
        }
    }

    private static TransformCommand resolveTransform(RenderNode node) {
        RenderNode current = node;
        while (current != null) {
            if (current.transform != null) return current.transform;
            current = current.renderingParent;
        }
        return null;
    }

    private static void applyTransform(RenderCommand head, TransformCommand transform) {
        RenderCommand current = head;
        while (current != null) {
            current.transform = transform;
            current = current.next;
        }
    }

    private static RenderCommand chainNodes(List<RenderNode> nodes, List<RenderCommand> nodeLinks, RenderCommand[] previousTail, List<RenderCommand> clonedChainHeads) {
        RenderCommand head = null;
        for (RenderNode node : nodes) {
            if (node.commandHeader != null) {
                if (head == null) head = node.commandHeader;
                if (previousTail[0] != null) {
                    previousTail[0].next = node.commandHeader;
                    nodeLinks.add(previousTail[0]);
                }
                previousTail[0] = node.commandTail;
            }
            RenderCommand childHead = chainNodes(node.renderingChildren, nodeLinks, previousTail, clonedChainHeads);
            if (head == null) head = childHead;
            if (!node.nodeOwner.repeatSource() || node.nodeOwner.repeatTime() < 1) continue;
            int positive = (node.nodeOwner.repeatTime() + 1) / 2;
            int negative = node.nodeOwner.repeatTime() / 2;
            for (int i = -negative; i <= positive; i++) {
                if (i == 0) continue;
                Vector2f offset = new Vector2f(node.nodeOwner.repeatSize()).mul(i);
                RenderCommand cloneHead = cloneSubTree(node, offset, null, nodeLinks, clonedChainHeads);
                if (cloneHead == null || previousTail[0] == null) continue;
                previousTail[0].next = cloneHead;
                nodeLinks.add(previousTail[0]);
                RenderCommand cloneTail = cloneHead;
                while (cloneTail.next != null) cloneTail = cloneTail.next;
                previousTail[0] = cloneTail;
            }
        }
        return head;
    }

    private static RenderCommand cloneSubTree(RenderNode node, Vector2f offset, TransformCommand parentClonedTransform, List<RenderCommand> nodeLinks, List<RenderCommand> cloneChainHeads) {
        TransformCommand clonedTransform = null;
        if (node.transform != null) {
            clonedTransform = (TransformCommand) RenderCommand.acquireCopy(node.transform);
            clonedTransform.position.add(offset);
            cloneChainHeads.add(clonedTransform);
        }
        TransformCommand finalTransform = clonedTransform;
        if (finalTransform == null) {
            if (parentClonedTransform != null) finalTransform = parentClonedTransform;
            else finalTransform = resolveTransform(node);
        }
        RenderCommand head = cloneChain(node.commandHeader, node.commandTail, finalTransform);
        if (head != null) cloneChainHeads.add(head);
        RenderCommand tail = head;
        if (tail != null) while (tail.next != null) tail = tail.next;
        for (RenderNode child : node.renderingChildren) {
            if (child.nodeOwner.nonRepeatable()) continue;
            RenderCommand childHead = cloneSubTree(child, offset, finalTransform, nodeLinks, cloneChainHeads);
            if (childHead == null) continue;
            if (tail != null) {
                tail.next = childHead;
                nodeLinks.add(tail);
            } else head = childHead;
            tail = childHead;
            while (tail.next != null) tail = tail.next;
        }
        return head;
    }

    private static RenderCommand cloneChain(RenderCommand chainHeader, RenderCommand chainTail, TransformCommand transform) {
        if (chainHeader == null) return null;
        RenderCommand cloneHead = null, cloneTail = null;
        RenderCommand current = chainHeader;
        while (current != null) {
            RenderCommand clone = RenderCommand.acquireCopy(current);
            if (clone != null) {
                clone.transform = transform;
                if (cloneHead == null) cloneHead = clone;
                if (cloneTail != null) cloneTail.next = clone;
                cloneTail = clone;
            }
            if (current == chainTail) break;
            current = current.next;
        }
        return cloneHead;
    }

    private void releaseTransform(RenderNode node) {
        if (node.transform != null) {
            node.transform.release();
            node.transform = null;
        }
        if (node.previousTransform == null) return;
        node.previousTransform.release();
        node.previousTransform = null;
    }
}
