package scripting.transpiler.parse;

import scripting.transpiler.TranspilerProperties;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Reader for script's {@code class Name extends Parent} or {@code enum Name} header.
 * Used in indexing type and superclass names.
 */
public final class HeaderScanner {
    /**
     * Record of a top level type declaration.
     * @param className the name of the type
     * @param superName the super class it extended from, null for an enum or when {@code extends} is omitted
     * @param isEnum true when the type is an enum
     */
    public record ClassHeader(String className, String superName, boolean isEnum) {}

    private HeaderScanner() {}

    /**
     * Scan for the first type header in the source.
     * @param source the script text
     * @return the header, or empty if none is found
     */
    public static Optional<ClassHeader> scan(String source) {
        Matcher matcher = TranspilerProperties.TypeHeaderRegex.matcher(source);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(new ClassHeader(matcher.group(2), matcher.group(3), matcher.group(1).equals("enum")));
    }
}
