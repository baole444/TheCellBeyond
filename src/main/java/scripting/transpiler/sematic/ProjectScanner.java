package scripting.transpiler.sematic;

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
    private static final Pattern JavaClass = Pattern.compile("(?m)^[ \\t]*(?:@[\\w.]+(?:\\([^)]*\\))?[ \\t]*)*(?:[a-z][\\w-]*[ \\t]+)*class[ \\t]+([A-Za-z_]\\w*)(?:[ \\t]*<[^>]*>)?(?:[ \\t]+extends[ \\t]+([A-Za-z_][\\w.]*))?");

    private ProjectScanner() {}

    /**
     * Scan a project root to index script classes.
     * @param scriptsRoot the project's {@code script-src} directory
     * @return the class index and collected errors
     */
    public static Result scan(Path scriptsRoot) {
        Map<String, ProjectClassEntry> index = new HashMap<>();
        List<SemanticError> errors = new ArrayList<>();
        for (Path file : walk(scriptsRoot, ".tcbs")) scriptEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        for (Path file : walk(scriptsRoot, ".java")) javaEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        return new Result(index, errors);
    }

    private static Optional<ProjectClassEntry> scriptEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        return HeaderScanner.scan(source).map(header -> new ProjectClassEntry(header.className(), relative(root, file), ProjectClassEntry.Kind.Script, header.superName(), ScriptPackage));
    }

    private static Optional<ProjectClassEntry> javaEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        Matcher classMatcher = JavaClass.matcher(source);
        if (!classMatcher.find()) return Optional.empty();
        return Optional.of(new ProjectClassEntry(classMatcher.group(1), relative(root, file), ProjectClassEntry.Kind.Java, simpleName(classMatcher.group(2)), packageOf(source)));
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

    private static List<Path> walk(Path root, String extension) {
        if (!Files.isDirectory(root)) return List.of();
        Path buildDir = root.resolve(BuildDir);
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(buildDir))
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
