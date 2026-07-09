package scripting.transpiler.semantic;

import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.parse.HeaderScanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
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

    private ProjectScanner() {}

    /**
     * Scan a project root to index script classes, excluding the Gradle default build output directory and the transpiler's generated source directory,
     * so the transpiler's own generated {@code .java} is not indexed against its {@code .tcbs} source.
     * @param scriptsRoot the project's {@code script-src} directory
     * @return the class index and collected errors
     */
    public static Result scan(Path scriptsRoot) {
        Set<Path> excludedDirs = Set.of(scriptsRoot.resolve(TranspilerProperties.BuildDir), scriptsRoot.resolve(TranspilerProperties.TranspilerOutputDir));
        Map<String, ProjectClassEntry> index = new HashMap<>();
        List<SemanticError> errors = new ArrayList<>();
        for (Path file : walk(scriptsRoot, TranspilerProperties.ScriptFileExtension, excludedDirs)) scriptEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        for (Path file : walk(scriptsRoot, TranspilerProperties.JavaFileExtension, excludedDirs)) javaEntry(scriptsRoot, file, errors).ifPresent(entry -> insert(index, entry, errors));
        return new Result(index, errors);
    }

    private static Optional<ProjectClassEntry> scriptEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        return HeaderScanner.scan(source).map(header -> new ProjectClassEntry(header.className(), relative(root, file), ProjectClassEntry.Kind.Script, header.superName(), TranspilerProperties.ScriptPackage, header.isEnum()));
    }

    private static Optional<ProjectClassEntry> javaEntry(Path root, Path file, List<SemanticError> errors) {
        String source = read(root, file, errors);
        if (source == null) return Optional.empty();
        Matcher typeMatcher = TranspilerProperties.JavaType.matcher(source);
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

    private static List<Path> walk(Path root, String extension, Set<Path> excludeDirs) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> excludeDirs.stream().noneMatch(p::startsWith))
                    .filter(p -> p.toString().endsWith(extension))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + root, e);
        }
    }

    private static String packageOf(String source) {
        Matcher matcher = TranspilerProperties.JavaPackage.matcher(source);
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
