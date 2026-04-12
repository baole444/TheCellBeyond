package render.text;

/**
 * Text horizontal alignment enums control the offset of the horizontal offset of text,
 * relative to the position of the object component.
 */
public enum HorizontalAlignment {
    /**
     * Nop offset, text started from the base position toward the right.
     */
    Left,
    /**
     * Text started from the left, offset by half text total width compare to the base position.
     */
    Centre,
    /**
     * Text started from the left, offset by text total width so that it end at the base position.
     */
    Right
}
