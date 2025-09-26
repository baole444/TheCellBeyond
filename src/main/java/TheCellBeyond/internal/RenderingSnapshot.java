package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import components.Component;

import java.util.List;

public record RenderingSnapshot(List<GameObject> updateObjects, List<GameObject> removeObjects, List<Component> removeComponents) {}
