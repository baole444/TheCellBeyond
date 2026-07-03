package scripting.transpiler.ast;

/**
 * Source location of an AST node matching ANTLR's {@code Token.getLine()} and {@code Token.getCharPositionInLine()}.
 * @param file the originating file
 * @param line line number, 1-base
 * @param column column number, 0-base
 */
public record SourcePosition(String file, int line, int column) {
    @Override
    public String toString() {
        return file + ":" + line + ":" + column;
    }
}
