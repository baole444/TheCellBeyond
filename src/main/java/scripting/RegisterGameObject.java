package scripting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate that a class should be classified as a {@link TheCellBeyond.GameObject}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterGameObject {
    /**
     * Get the display label for the custom game object.
     * @return the defined label
     */
    String label() default "";
    /**
     * Get the display description for the custom game object.
     * @return the defined description
     */
    String description() default "";
}
