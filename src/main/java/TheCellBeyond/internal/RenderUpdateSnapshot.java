package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import components.Component;

import java.util.List;

public record RenderUpdateSnapshot(List<GameObject> updateObjects) {}
