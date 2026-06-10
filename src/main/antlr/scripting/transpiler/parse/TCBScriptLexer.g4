lexer grammar TCBScriptLexer;

@header {
    package scripting.transpiler.parse;
}

options {
    superClass = TCBScriptLexerBase;
}

// INDENT/DEDENT are emitted by TCBScriptLexerBase, not by matching rules.
tokens {
    INDENT,
    DEDENT
}

NEWLINE: ({atStartOfInput()}? SPACES | ('\r'? '\n' | '\r' | '\f') SPACES?) { onNewLine(); };

COLON: ':';

OPEN_PAREN : '(' { openBrace(); };
CLOSE_PAREN: ')' { closeBrace(); };
OPEN_BRACK : '[' { openBrace(); };
CLOSE_BRACK: ']' { closeBrace(); };
OPEN_BRACE : '{' { openBrace(); };
CLOSE_BRACE: '}' { closeBrace(); };

NAME: [a-zA-Z_][a-zA-Z_0-9]*;

SKIP_: [ \t]+ -> skip;

fragment SPACES: [ \t]+;
