package render;

import render.commands.TransformCommand;

public sealed interface BatchEntry permits RectEntry, MeshEntry, TextEntry {
    TransformCommand transform();
}
