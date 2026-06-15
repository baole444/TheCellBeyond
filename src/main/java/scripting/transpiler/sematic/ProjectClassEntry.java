package scripting.transpiler.sematic;

/**
 * An entry in the project class index, for a {@code .tcbs} or {@code .java} script file under {@code scripts-src}.
 * <p>
 * Built by {@link ProjectScanner}, consumed by the resolver and validator to resolve project's local type references,
 * along with walking the class's extending lineage.
 * @param simpleName the class's simple name, unique across the project
 * @param fileSource the source file path, relative to the scripts root
 * @param kind whether the class is a TCBScript file or a Java file
 * @param superClassRef simple name of the declared superclass, or null when {@code extends} is omitted
 * @param packageHint the package that the class is in, empty if in default pacakge
 */
public record ProjectClassEntry(String simpleName, String fileSource, Kind kind, String superClassRef, String packageHint) {
    /**
     * The origin of a script class.
     */
    public enum Kind {
        Script,
        Java
    }

    /**
     * Get the class's fully qualified name. This is a combination of package hint, if existed, and  the class's simple name.
     * @return the class's fully qualified name
     */
    public String fqn() {
        return packageHint.isEmpty() ? simpleName : packageHint + "." + simpleName;
    }
}
