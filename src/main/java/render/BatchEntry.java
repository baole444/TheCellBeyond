package render;

import render.commands.TransformCommand;

/**
 * Sealed interface for various batch entry types, provide command method transform command and previous transform command.
 */
public sealed interface BatchEntry permits RectEntry, MeshEntry, TextEntry {
    /**
     * Get the transform command of the entry.
     * @return the transform command or null
     */
    TransformCommand transform();

    /**
     * Get the transform command for past transform of the entry, use in physic interpolation.
     * @return the transform command or null
     */
    TransformCommand previousTransform();
}
