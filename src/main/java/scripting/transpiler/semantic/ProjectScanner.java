package scripting.transpiler.semantic;

import scripting.transpiler.parse.HeaderScanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * First pass of the semantic pass, building the project class index. Used in resolving cross script and script to Java type references.
 * <p>
 * Both {@code .tcbs} and handwritten {@code .java} script files can be store anywhere within project's root.
 * The flat package layout {@code scripts} is appended to TCBscript file indices.
 */
public final class ProjectScanner {
    /**
     * Result of a project scan.
     * @param index class index, keyed by simple name
     * @param errors the list of error that occurred during scan
     */
    public record Result(Map<String, ProjectClassEntry> index, List<SemanticError> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private static final String ScriptPackage = "scripts";
    private static final String BuildDir = "build";
    private static final Pattern JavaPackage = Pattern.compile("(?m)^[ \\t]*package[ \\t]+([A-Za-z_][A-Za-z_0-9.]*)[ \\t]*;");
    private static final Pattern JavaType = Pattern.compile("(?m)^[ \\t]*(?:@[\\w.]+(?:\\([^)]*\\))?[ \\t]*)*(?:[a-z][\\w-]*[ \\t]+)*(class|enum)[ \\t]+([A-Za-z_]\\w*)(?:[ \\t]*<[^>]*>)?(?:[ \\t]+extends[ \\t]+([A-Za-z_][\\w.]*))?");

    private ProjectScanner() {}

    /**
     * Scan a project root to index script classes, excluding the Gradle default build directory.
     * @param scriptsRoot the project's {@code script-src} directory
     * @return the class index and collected errors
     */
    public static Result scan(Path scriptsRoot) {
        return scan(scriptsRoot, scriptsRoot.resolve(BuildDir));
    }

    /**
     * Scan a project root to index script classes, excluding the given build output directory,
     * so the transpiler's own generated {@code .java} is not indexed against its {@code .tcbs} source.
     * @param scriptsRoot the project's {@code script-src} directory
     * @param buildOutputDir the Gradle build output director to exclude, or null to exclude nothing
     * @return the class index and collected errors
     */
    public static Result scan(Path scriptsRoot, Path buildOutputDir) {
        Map<String, ProjectClassEntry> index = new HashMap<>();
        List<SemanticError> errors = new ArrayList<>();
        for (Path file : walk(scriptsRoot, ".tcbs", buildOutputDir)) scriptEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        for (Path file : walk(scriptsRoot, ".java", buildOutputDir)) javaEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        return new Result(index, errors);
    }

    private static Optional<ProjectClassEntry> scriptEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        return HeaderScanner.scan(source).map(header -> new ProjectClassEntry(header.className(), relative(root, file), ProjectClassEntry.Kind.Script, header.superName(), ScriptPackage, header.isEnum()));
    }

    private static Optional<ProjectClassEntry> javaEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        Matcher typeMatcher = JavaType.matcher(source);
        if (!typeMatcher.find()) return Optional.empty();
        boolean isEnum = typeMatcher.group(1).equals("enum");
        return Optional.of(new ProjectClassEntry(typeMatcher.group(2), relative(root, file), ProjectClassEntry.Kind.Java, simpleName(typeMatcher.group(3)), packageOf(source), isEnum));
    }

    private static void insert(Map<String, ProjectClassEntry> index, ProjectClassEntry entry, List<SemanticError> errors) {
        ProjectClassEntry existing = index.putIfAbsent(entry.simpleName(), entry);
        if (existing == null) return;
        errors.add(new SemanticError(entry.fileSource(), 0, 0, String.format("Duplicate class name '%s': also declared in %s", entry.simpleName(), existing.fileSource())));
    }

    private static String read(Path root, Path file, List<SemanticError> errors) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            errors.add(new SemanticError(relative(root, file), 0, 0, "Could not read file: " + e.getMessage()));
            return null;
        }
    }

    private static List<Path> walk(Path root, String extension, Path buildDir) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> buildDir == null || !p.startsWith(buildDir))
                    .filter(p -> p.toString().endsWith(extension))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + root, e);
        }
    }

    private static String packageOf(String source) {
        Matcher matcher = JavaPackage.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String simpleName(String dottedName) {
        if (dottedName == null) return null;
        int dot = dottedName.lastIndexOf('.');
        return dot < 0 ? dottedName : dottedName.substring(dot + 1);
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
