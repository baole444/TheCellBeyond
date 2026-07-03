package scripting.transpiler.manifest;

import java.util.List;

/**
 * MemberInfo is a record of a public method or field belongs to an API class, in the script API manifest.
 * @param javaName original member's name in java class
 * @param snakeAlias member's name in snake case convention
 * @param signatures (method only) the JVM type descriptor for all the overload(s) that shared the snake alias
 * @param type (field only) the fully qualified type name of the member
 */
public record MemberInfo(String javaName, String snakeAlias, List<String> signatures, String type) {
    /**
     * Create a new {@link MemberInfo} record for a public method in an API class.
     * @param javaName original method's name in java class
     * @param snakeAlias method's name in snake case convention
     * @param signatures the JVM type descriptor for all the overload(s) that shared this snake case alias
     * @return a new {@link MemberInfo} represent the method
     */
    public static MemberInfo method(String javaName, String snakeAlias, List<String> signatures) {
        return new MemberInfo(javaName, snakeAlias, signatures, null);
    }

    /**
     * Create a new {@link MemberInfo} record for a public field in a API class.
     * @param javaName original field's name in java class
     * @param snakeAlias field's name in snake case convention
     * @param type the fully qualified type name of the field
     * @return a new {@link MemberInfo} represent the field
     */
    public static MemberInfo field(String javaName, String snakeAlias, String type) {
        return new MemberInfo(javaName, snakeAlias, null, type);
    }
}
