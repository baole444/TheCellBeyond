package scripting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate that a class should be classified as a {@link components.Component}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@API
public @interface RegisterComponent {
    /**
     * Get the display label for the custom component.
     * @return the defined label
     */
    String label() default "";
    /**
     * Get the display description for the custom component.
     * @return the defined description
     */
    String description() default "";
}
