package scripting.transpiler.codegen;

import scripting.transpiler.ast.ClassDeclaration;
import scripting.transpiler.parse.ScriptParser;
import scripting.transpiler.semantic.ProjectClassEntry;
import scripting.transpiler.semantic.SemanticAnalyzer;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Entry point of TCBScript transpiler. Translation process includes: parse -> semantic resolution -> code generation.
 * <p>
 * On parse or semantic errors, no source java file is produced, as the errors are returned for the caller to handle.
 */
public final class Transpiler {
    static final EngineLog Logger = new EngineLog(Transpiler.class);
    /**
     * The outcome of a translation.
     * @param className the generated class's simple name, null on error
     * @param javaSource the rendered Java source, null or error
     * @param errors the parse and semantic errors, empty on success
     */
    public record Result(String className, String javaSource, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    /**
     * The flat package every generated script belong to.
     */
    public static final String GeneratedPackage = "scripts";

    private Transpiler() {}

    /**
     * Translate a script source into java source.
     * @param source the script text
     * @param fileName the source file name, used in positions, errors and the generated file header
     * @param projectIndex the project class index from the scanner, for cross script resolution
     * @return the translation result
     */
    public static Result transpile(String source, String fileName, Map<String, ProjectClassEntry> projectIndex) {
        ScriptParser.Result parsed = ScriptParser.parse(source, fileName);
        if (parsed.hasErrors()) return new Result(null, null, parsed.errors.stream().map(Object::toString).toList());
        SemanticAnalyzer.Result analyzed = SemanticAnalyzer.analyze(parsed.scriptFile, projectIndex);
        if (analyzed.hasErrors()) return new Result(null, null, analyzed.errors().stream().map(Object::toString).toList());
        ClassDeclaration classDeclaration = analyzed.scriptFile().classDeclaration;
        return new Result(classDeclaration.name, ClassEmitter.emit(classDeclaration, fileName), List.of());
    }

    /**
     * Clear the generated package directory, removing old output before new translation run.
     * @param generatedRoot the generated source root, such as {@code build/generated/script-java}
     * @return true when the directory was cleared or absent, or false when Io exception occurred
     */
    public static boolean clearGenerated(Path generatedRoot) {
        Path packageDir = generatedRoot.resolve(GeneratedPackage);
        if (!Files.exists(packageDir)) return true;
        try (Stream<Path> walk = Files.walk(packageDir)) {
            List<Path> entries = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path entry : entries) Files.delete(entry);
            return true;
        } catch (IOException e) {
            Logger.error(String.format("Failed to clear generated sources in %s: %s", packageDir, e.getMessage()));
            return false;
        }
    }

    /**
     * Write a generated class into the generated package directory.
     * @param generatedRoot the generated source root, such as {@code build/generated/script-java}
     * @param className the class's simple name, used for the file name
     * @param javaSource the rendered Java source
     * @return the written file path, or null when IO exception occurred
     */
    public static Path write(Path generatedRoot, String className, String javaSource) {
        Path file = generatedRoot.resolve(GeneratedPackage).resolve(className + ".java");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, javaSource, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            Logger.error(String.format("Failed to write generated source %s: %s", file, e.getMessage()));
            return null;
        }
    }
}
