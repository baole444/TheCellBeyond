package scripting;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;

/**
 * TypeHint is optional parameter in {@link Export} to enforce specific type for an exporting field.
 * @see Export
 */
@API
public enum TypeHint {
    /**
     * Let the editor detect the type automatically.
     */
    Auto(null),
    /**
     * Hint that the field is of vector type for RGBA colour.
     */
    Color(Vector4f.class),
    /**
     * Hint that the field is of vector 2 type.
     */
    Vector2(Vector2f.class),
    /**
     * Hint that the field is of boolean type.
     */
    Boolean(boolean.class),
    /**
     * Hint that the field is of int type.
     */
    Integer(int.class),
    /**
     * Hint that the field is of float type.
     */
    Float(float.class),
    /**
     * Hint that the field is of string type.
     */
    String(java.lang.String.class),
    /**
     * Hint that the field is of Enum type.
     */
    Enum(java.lang.Enum.class);

    /**
     * The underlying class represented by the hint.
     */
    public final Class<?> dataType;
    /**
     * An unmodifiable view of a list of type hint except for {@link #Auto}.
     */
    public static final List<TypeHint> noneAutoTypes = Arrays.stream(TypeHint.values()).skip(1).toList();

    TypeHint(Class<?> dataType) {
        this.dataType = dataType;
    }
}
