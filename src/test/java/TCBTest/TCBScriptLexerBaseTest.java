package TCBTest;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.junit.jupiter.api.Test;
import scripting.transpiler.parse.TCBScriptLexer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TCBScriptLexerBaseTest {
    /**
     * Lex a source string into the sequence of emitted token symbolic names (EOF -> "EOF".)
     * @param source string to covert
     * @return symbolic rules
     */
    private static List<String> lex(String source) {
        TCBScriptLexer lexer = new TCBScriptLexer(CharStreams.fromString(source));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        stream.fill();
        Vocabulary vocab = lexer.getVocabulary();
        List<String> names = new ArrayList<>();
        for (Token token : stream.getTokens()) {
            names.add(token.getType() == Token.EOF ? "EOF" : vocab.getSymbolicName(token.getType()));
        }
        return names;
    }

    @Test
    public void singleIndentEmitsIndentDedent() {
        assertEquals(
                List.of("NAME", "COLON", "NEWLINE", "INDENT", "NAME", "NEWLINE", "DEDENT", "EOF"),
                lex("a:\n    b\n"));
    }

    @Test
    public void multiLevelIndent() {
        assertEquals(
                List.of("NAME", "COLON", "NEWLINE", "INDENT", "NAME", "COLON", "NEWLINE", "INDENT",
                        "NAME", "NEWLINE", "DEDENT", "DEDENT", "EOF"),
                lex("a:\n    b:\n        c\n"));
    }

    @Test
    public void dedentToMidLevel() {
        assertEquals(
                List.of("NAME", "COLON", "NEWLINE", "INDENT", "NAME", "COLON", "NEWLINE", "INDENT",
                        "NAME", "NEWLINE", "DEDENT", "NAME", "NEWLINE", "DEDENT", "EOF"),
                lex("a:\n    b:\n        c\n    d\n"));
    }

    @Test
    public void eofWithOpenBlocks() {
        assertEquals(
                List.of("NAME", "COLON", "NEWLINE", "INDENT", "NAME", "NEWLINE", "DEDENT", "EOF"),
                lex("a:\n    b"));
    }

    @Test
    public void implicitJoinInParens() {
        List<String> tokens = lex("f(\n    a\n)\n");
        assertEquals(
                List.of("NAME", "OPEN_PAREN", "NAME", "CLOSE_PAREN", "NEWLINE", "EOF"),
                tokens);
        assertFalse(tokens.contains("INDENT"));
        assertFalse(tokens.contains("DEDENT"));
    }

    @Test
    public void blankLineInBlockDoesNotDedent() {
        assertEquals(
                List.of("NAME", "COLON", "NEWLINE", "INDENT", "NAME", "NEWLINE", "NAME", "NEWLINE", "DEDENT", "EOF"),
                lex("a:\n    b\n\n    c\n"));
    }
}
