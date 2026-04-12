package scripting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate that a field should be exported into the editor for adjusting.
 * Currently, this only works for data type supported by {@link TypeHint} on game object or component.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Export {
    /**
     * Get the display label for the custom field.
     * @return the defined label
     */
    String label() default "";
    /**
     * Get the display description for the custom field (currently not implemented).
     * @return the defined label
     */
    String description() default "";
    /**
     * Get the hinted type for the custom field, by default it is {@link TypeHint#Auto}.
     * @return the hinted type
     */
    TypeHint type() default TypeHint.Auto;
}
