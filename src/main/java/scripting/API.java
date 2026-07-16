package scripting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for the engine's public scripting API.
 * <p>
 * A type with this annotation mean that its entire public and protected surface is API that maybe be used by user script.
 * The build compute the transitive closure of every seed over that surface, including supertypes, interfaces, field types, method parameter/return/throws types and their generic arguments.
 * That closure is the set of classes packaged into the API jar. Every export is a deliberated annotation.
 * </p>
 * The marker is at class level, and can't be opt out. If a type has members that should not be public API, it is a sign
 * that the type itself is wrong, not that a member should be hidden.
 * <p>
 * The retention policy mean this marker only exist in build, and not part of the engine's API, or is any part of a surface it describe.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface API {}
