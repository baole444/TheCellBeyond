package scripting;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;

public enum TypeHint {
    Auto(null),
    Color(Vector4f.class),
    Vector2(Vector2f.class),
    Boolean(boolean.class),
    Integer(int.class),
    Float(float.class),
    String(java.lang.String.class);

    public final Class<?> dataType;
    /**
     * An unmodifiable view of a list of type hint except for {@link #Auto}.
     */
    public static final List<TypeHint> noneAutoTypes = Arrays.stream(TypeHint.values()).skip(1).toList();

    TypeHint(Class<?> dataType) {
        this.dataType = dataType;
    }
}
