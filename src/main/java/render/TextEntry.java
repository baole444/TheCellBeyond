package render;

import render.commands.TextCommand;
import render.commands.TransformCommand;

public record TextEntry(TextCommand command, TransformCommand transform) implements BatchEntry {}
