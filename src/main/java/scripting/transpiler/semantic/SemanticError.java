package scripting.transpiler.semantic;

/**
 * A semantic analysis error report.
 * @param file the file where the error originated
 * @param line the line number of the error
 * @param column the column number of the error
 * @param message the error message
 */
public record SemanticError(String file, int line, int column, String message) {
    @Override
    public String toString() {
        return file + ":" + line + ":" + column + ": " + message;
    }
}
