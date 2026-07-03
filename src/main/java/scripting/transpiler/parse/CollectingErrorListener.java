package scripting.transpiler.parse;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

final class CollectingErrorListener extends BaseErrorListener {
    private final String file;
    private final List<ParseError> errors = new ArrayList<>();

    CollectingErrorListener(String file) {
        this.file = file;
    }

    List<ParseError> errors() {
        return errors;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        errors.add(new ParseError(file, line, charPositionInLine, msg));
    }
}
