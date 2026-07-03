package scripting.transpiler.parse;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import scripting.transpiler.ast.ScriptFile;

import java.util.List;

/**
 * ScriptParser is the entry point for parsing TCBScript source into an AST.
 * It wires the lexer, parser, error collection, along with running {@link ParseTreeToAst}.
 */
public final class ScriptParser {
    private ScriptParser() {}

    /**
     * The outcome of a parse, either AST success, or the collected errors.
     * When {@link #errors} is empty, {@link #scriptFile} is not null.
     */
    public static final class Result {
        public final ScriptFile scriptFile;
        public final List<ParseError> errors;

        private Result(ScriptFile scriptFile, List<ParseError> errors) {
            this.scriptFile = scriptFile;
            this.errors = errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    /**
     * Parse the {@code source}, attributing positions and errors to {@code file}.
     * @param source the script text
     * @param file the file name used in source positions and error messages
     * @return the parse result
     */
    public static Result parse(String source, String file) {
        CollectingErrorListener listener = new CollectingErrorListener(file);
        TCBScriptLexer lexer = new TCBScriptLexer(CharStreams.fromString(normalize(source)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        TCBScriptParser parser = new TCBScriptParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        TCBScriptParser.ScriptFileContext tree = parser.scriptFile();
        if (!listener.errors().isEmpty()) return new Result(null, List.copyOf(listener.errors()));
        ScriptFile ast = new ParseTreeToAst(file).convert(tree);
        return new Result(ast, List.of());
    }

    /**
     * Add a trailing line break to close the final block when there is none in the source.
     * This is the requirement for indentation lexer.
     * @param source the source to normalize
     * @return the original source if there is already a line break, otherwise it is the source with appended line break
     */
    private static String normalize(String source) {
        if (source.isEmpty()) return "\n";
        char last = source.charAt(source.length() - 1);
        if (last == '\n' || last == '\r') return source;
        return source + "\n";
    }
}
