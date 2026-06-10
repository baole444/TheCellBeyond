/*
 * Adapted from the ANTLR grammars-v4 project.
 * Source: python/python3/Java/Python3LexerBase.java
 * URL: https://github.com/antlr/grammars-v4/blob/6590fdd07b9fa1b4a66025c8315ee2c93bbcbe7d/python/python3/Java/Python3LexerBase.java
 * Retrieved: 2026-06-07. No licence was declared at the time of retrieval.
 *
 * Modification derived from the original not limited to class name, package, constant naming and overall code style.
 * The purpose of this notice is to honour the contribution from the original source code with appropriate attribution.
 */

package scripting.transpiler.parse;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Base lexer that emits synthetic INDENT/DEDENT tokens for TCBScript's indentation blocks.
 */
public abstract class TCBScriptLexerBase extends Lexer {
    /**
     * A queue where extra tokens are pushed on.
     */
    private LinkedList<Token> tokens = new LinkedList<>();
    /**
     * The stack that keeps track of the indentation level.
     */
    private Deque<Integer> indents = new ArrayDeque<>();
    /**
     * The amount of opened braces, brackets and parenthesis.
     */
    private int opened = 0;
    /**
     * The most recently produced token.
     */
    private Token lastToken = null;

    protected TCBScriptLexerBase(CharStream input) {
        super(input);
    }

    /**
     * Check if it is at the beginning of input.
     * @return true if the current position in line is 0 and is on line 1
     */
    protected boolean atStartOfInput() {
        return getCharPositionInLine() == 0 && getLine() == 1;
    }

    /**
     * Mark opened a brace, bracket or parentheses.
     */
    protected void openBrace() {
        opened++;
    }

    /**
     * Mark closed a brace, bracket or parentheses.
     */
    protected void closeBrace() {
        opened--;
    }

    /**
     * Strip newlines inside open clauses except if we are near EOF. We keep NEWLINEs near EOF to
     * satisfy the final newline needed by the single_put rule used by the REPL.
     * <p>
     * If we're inside a list or on a blank line, ignore all indents, dedents and line breaks.
     * Skip indents of the same size as the present indent-size.
     * </p>
     * Finally emit DEDENT tokens, can be more than 1.
     */
    protected void onNewLine() {
        String newLine = getText().replaceAll("[^\r\n\f]+", "");
        String spaces = getText().replaceAll("[\r\n\f]+", "");
        if (atSuppressedLinebreak()) {
            skip();
            return;
        }
        emit(commonToken(TCBScriptLexer.NEWLINE, newLine));
        int indent = countIndentations(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent == previous) {
            skip();
            return;
        }
        if (indent > previous) {
            indents.push(indent);
            emit(commonToken(TCBScriptLexer.INDENT, spaces));
            return;
        }
        while (!indents.isEmpty() && indents.peek() > indent) {
            emit(createDedent());
            indents.pop();
        }
    }

    @Override
    public Token nextToken() {
        if (atEOFWithIndents()) {
            for (int i = tokens.size() - 1; i >= 0; i--) {
                if (tokens.get(i).getType() == EOF) tokens.remove(i);
            }
            emit(commonToken(TCBScriptLexer.NEWLINE, "\n"));
            while (!indents.isEmpty()) {
                emit(createDedent());
                indents.pop();
            }
            emit(commonToken(Token.EOF, "<EOF>"));
        }
        Token next = super.nextToken();
        if (next.getChannel() == Token.DEFAULT_CHANNEL) lastToken = next;
        return tokens.isEmpty() ? next : tokens.poll();
    }

    @Override
    public void emit(Token token) {
        super.setToken(token);
        tokens.offer(token);
    }

    @Override
    public void reset() {
        tokens = new LinkedList<>();
        indents = new ArrayDeque<>();
        opened = 0;
        lastToken = null;
        super.reset();
    }

    /**
     * Check if the end-of-file is ahead and there are still some dedent expected.
     * @return true if at OEF and the indent token list is not empty
     */
    private boolean atEOFWithIndents() {
        return _input.LA(1) == EOF && !indents.isEmpty();
    }

    /**
     * Check if the current line break is emitting nothing.
     * All indents, dedents, and line breaks should be skipped for such case.
     * @return true if inside on open clause (implicit line join), on a blank line, or before a comment line
     */
    private boolean atSuppressedLinebreak() {
        int next = _input.LA(1);
        int nextNext = _input.LA(2);
        return opened > 0 || (nextNext != -1 && (next == '\r' || next == '\n' || next == '\f' || next == '#'));
    }

    private CommonToken commonToken(int type, String text) {
        int stop = getCharIndex() - 1;
        int start = text.isEmpty() ? stop : stop - text.length() + 1;
        return new CommonToken(_tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
    }

    private Token createDedent() {
        CommonToken dedent = commonToken(TCBScriptLexer.DEDENT, "");
        dedent.setLine(lastToken.getLine());
        return dedent;
    }

    /**
     * Calculates the indentation of the provided spaces, taking the following rules into account:
     * <p>
     * Tabs are replaced, from left to right, by one to eight spaces, such that the total number of characters,
     * up to and including the replacement is a multiple of eight.
     * @param spaces the spaces to count for
     * @return the number of indentations
     */
    private static int countIndentations(String spaces) {
        int count = 0;
        for (char c : spaces.toCharArray()) {
            if (c == '\t') count += 8 - (count % 8);
            else count++;
        }
        return count;
    }
}
