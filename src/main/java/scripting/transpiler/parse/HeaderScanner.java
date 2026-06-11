package scripting.transpiler.parse;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader for script's {@code class Name extends Parent} header.
 * Used in indexing class and superclass names.
 */
public final class HeaderScanner {
    /**
     * Record of a class declaration.
     * @param className the name of the class
     * @param superName the super class it extended from
     */
    public record ClassHeader(String className, String superName) {}

    /**
     * Matches {@code class Name [extends Parent]} at the start of a line, ignores lines with leading comment.
     * {@code extends} section may sit on the same or drop to a following line.
     */
    private static final Pattern Header = Pattern.compile("(?m)^[ \\t]*class[ \\t]+([A-Za-z_][A-Za-z_0-9]*)(?:\\s+extends[ \\t]+([A-Za-z_][A-Za-z_0-9]*))?");

    private HeaderScanner() {}

    /**
     * Scan for the first class header in the source.
     * @param source the script text
     * @return the header, or empty if none is found
     */
    public static Optional<ClassHeader> scan(String source) {
        Matcher matcher = Header.matcher(source);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(new ClassHeader(matcher.group(1), matcher.group(2)));
    }
}
