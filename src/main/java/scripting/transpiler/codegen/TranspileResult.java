package scripting.transpiler.codegen;

import java.util.List;

/**
 * Outcome of a project translation.
 * @param success the transpile process success status
 * @param transpileCount the number of scripts translated, 0 on failure or there is no script at all
 * @param errors the number of errors, 0 if success
 */
public record TranspileResult(boolean success, int transpileCount, List<String> errors) {
    /**
     * Check if the transpile process contain any error.
     * @return true when there is at least 1 error
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Get the result for a success translation process with no error.
     * @param transpileCount the number of scripts translated
     * @return the successful result
     */
    public static TranspileResult ok(int transpileCount) {
        return new TranspileResult(true, transpileCount, List.of());
    }

    /**
     * Get the result fpr a fail translation process.
     * @param errors the list of errors
     * @return the fail result
     */
    public static TranspileResult failed(List<String> errors) {
        return new TranspileResult(false, 0, List.copyOf(errors));
    }
}
