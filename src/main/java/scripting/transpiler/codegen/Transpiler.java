package scripting.transpiler.codegen;

import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.ast.ClassDeclaration;
import scripting.transpiler.ast.EnumDeclaration;
import scripting.transpiler.ast.TypeDeclaration;
import scripting.transpiler.parse.ScriptParser;
import scripting.transpiler.semantic.ProjectClassEntry;
import scripting.transpiler.semantic.ProjectScanner;
import scripting.transpiler.semantic.SemanticAnalyzer;
import utility.log.EngineLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Entry point of TCBScript transpiler. Translation process includes: parse -> semantic resolution -> code generation.
 * <p>
 * On parse or semantic errors, no source java file is produced, as the errors are returned for the caller to handle.
 */
public final class Transpiler {
    private static final EngineLog Logger = new EngineLog(Transpiler.class);
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
        TypeDeclaration type = analyzed.scriptFile().typeDeclaration;
        String javaSource = switch (type) {
            case ClassDeclaration c -> ClassEmitter.emit(c, fileName);
            case EnumDeclaration e -> EnumEmitter.emit(e, fileName);
        };
        return new Result(type.name, javaSource, List.of());
    }

    /**
     * Clear the generated package directory, removing old output before new translation run.
     * @param generatedRoot the generated source root, such as {@code build/generated/script-java}
     * @return true when the directory was cleared or absent, or false when Io exception occurred
     */
    public static boolean clearGenerated(Path generatedRoot) {
        Path packageDir = generatedRoot.resolve(TranspilerProperties.ScriptPackage);
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
        Path file = generatedRoot.resolve(TranspilerProperties.ScriptPackage).resolve(className + TranspilerProperties.JavaFileExtension);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, javaSource, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            Logger.error(String.format("Failed to write generated source %s: %s", file, e.getMessage()));
            return null;
        }
    }

    /**
     * Start the transpile process for the entire project under the given source root.
     * @param scriptsSrcRoot the project's script source root directory
     * @return the translating result
     */
    public static TranspileResult transpileProject(Path scriptsSrcRoot) {
        Logger.info("Translating scripts...");
        Path generatedRoot = scriptsSrcRoot.resolve(TranspilerProperties.TranspilerOutputDir);
        ProjectScanner.Result scan = ProjectScanner.scan(scriptsSrcRoot);
        if (scan.hasErrors()) return TranspileResult.failed(scan.errors().stream().map(Objects::toString).toList());
        List<Result> outputs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        scriptFiles(scriptsSrcRoot, generatedRoot).forEach(script -> {
            String source = readScript(scriptsSrcRoot, script, errors);
            if (source == null) return;
            Result result = transpile(source, relative(scriptsSrcRoot, script), scan.index());
            if (result.hasErrors()) errors.addAll(result.errors);
            else outputs.add(result);
        });
        if (!errors.isEmpty()) return TranspileResult.failed(errors);
        if (!clearGenerated(generatedRoot)) return TranspileResult.failed(List.of("Failed to clear generated sources at " + generatedRoot.resolve(TranspilerProperties.ScriptPackage)));
        for (Result output : outputs) {
            if (write(generatedRoot, output.className, output.javaSource) == null) return TranspileResult.failed(List.of("Failed to write generated source for " + output.className));
        }
        return TranspileResult.ok(outputs.size());
    }

    private static String readScript(Path root, Path script, List<String> errors) {
        try {
            return Files.readString(script, StandardCharsets.UTF_8);
        } catch (IOException e) {
            errors.add(relative(root, script) + ": could not read file: " + e.getMessage());
            return null;
        }
    }

    private static List<Path> scriptFiles(Path scriptsSrcRoot, Path buildOutputDir) {
        if (!Files.isDirectory(scriptsSrcRoot)) return List.of();
        try (Stream<Path> walk = Files.walk(scriptsSrcRoot)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> buildOutputDir == null || !p.startsWith(buildOutputDir))
                    .filter(p -> p.toString().endsWith(TranspilerProperties.ScriptFileExtension))
                    .sorted().toList();
        } catch (IOException e) {
            Logger.error(String.format("Failed to walk %s: %s", scriptsSrcRoot, e.getMessage()));
            return List.of();
        }
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
