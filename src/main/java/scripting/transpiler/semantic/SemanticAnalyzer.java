package scripting.transpiler.semantic;

import scripting.transpiler.ast.ScriptFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the second semantic pass on a parsed script.
 * This runs the {@link SymbolResolver}, which annotated the AST in place, followed by the {@link Validator}, returning the combined errors.
 * <p>
 * The caller is responsible for supplying the project class index from {@link ProjectScanner}.
 */
public final class SemanticAnalyzer {
    public record Result(ScriptFile scriptFile, List<SemanticError> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private SemanticAnalyzer() {}

    public static Result analyze(ScriptFile scriptFile, Map<String, ProjectClassEntry> projectIndex) {
        List<SemanticError> errors = new ArrayList<>();
        errors.addAll(new SymbolResolver(projectIndex).resolve(scriptFile));
        errors.addAll(new Validator().validate(scriptFile));
        return new Result(scriptFile, List.copyOf(errors));
    }
}
