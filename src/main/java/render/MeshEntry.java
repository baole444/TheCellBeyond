package render;

import render.commands.MeshCommand;
import render.commands.TransformCommand;

public record MeshEntry(MeshCommand command, TransformCommand transform, TransformCommand previousTransform) implements BatchEntry {}
