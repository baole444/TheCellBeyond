package render;

import render.commands.RectCommand;
import render.commands.TransformCommand;

public record RectEntry(RectCommand command, TransformCommand transform, TransformCommand previousTransform) implements BatchEntry {}
