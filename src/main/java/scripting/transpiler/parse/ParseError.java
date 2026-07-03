package scripting.transpiler.parse;

public record ParseError(String file, int line, int column, String message) {
    @Override
    public String toString() {
        return file + ":" + line + ":" + column + ": " + message;
    }
}
