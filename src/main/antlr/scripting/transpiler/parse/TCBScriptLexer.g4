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

// Note: to keep blocks of token declaration more readable, their value can be aligned by the longest token in that block.

NEWLINE: ({atStartOfInput()}? SPACES | ('\r'? '\n' | '\r' | '\f') SPACES?) { onNewLine(); };

// Keyword declarations above NAME to prioritize equal-length matching.
CLASS    : 'class';
ENUM     : 'enum';
EXTENDS  : 'extends';
FUNC     : 'func';
NEW      : 'new';
SELF     : 'self';
VAR      : 'var';
CONST    : 'const';
STATIC   : 'static';
RETURN   : 'return';
IF       : 'if';
ELIF     : 'elif';
ELSE     : 'else';
WHILE    : 'while';
FOR      : 'for';
IN       : 'in';
BREAK    : 'break';
CONTINUE : 'continue';
PASS     : 'pass';
AND      : 'and';
OR       : 'or';
NOT      : 'not';
IS       : 'is';
AS       : 'as';
PUBLIC   : 'public';
PRIVATE  : 'private';
PROTECTED: 'protected';
TRUE     : 'true';
FALSE    : 'false';
NULL     : 'null';

// Multichars operatators (above theior singlechar counterparts.)
ARROW         : '->';
EQ            : '==';
NEQ           : '!=';
LE            : '<=';
GE            : '>=';
PLUS_ASSIGN   : '+=';
MINUS_ASSIGN  : '-=';
STAR_ASSIGN   : '*=';
SLASH_ASSIGN  : '/=';
PERCENT_ASSIGN: '%=';
AMP_AMP       : '&&';
PIPE_PIPE     : '||';

// Singlechar operators and punctuation.
ASSIGN  : '=';
LT      : '<';
GT      : '>';
PLUS    : '+';
MINUS   : '-';
STAR    : '*';
SLASH   : '/';
PERCENT : '%';
BANG    : '!';
QUESTION: '?';
DOT     : '.';
COMMA   : ',';
AT      : '@';
COLON   : ':';

OPEN_PAREN : '(' { openBrace(); };
CLOSE_PAREN: ')' { closeBrace(); };
OPEN_BRACK : '[' { openBrace(); };
CLOSE_BRACK: ']' { closeBrace(); };
OPEN_BRACE : '{' { openBrace(); };
CLOSE_BRACE: '}' { closeBrace(); };

// Literals.
FLOAT_LITERAL
    : DIGITS '.' DIGITS? EXPONENT?
    | '.' DIGITS EXPONENT?
    | DIGITS EXPONENT
    ;

INTEGER_LITERAL
    : '0' [xX] HEX_DIGIT ('_'? HEX_DIGIT)*
    | DIGITS
    ;

STRING_LITERAL
    : '"' (~["\\\r\n] | STRING_ESCAPE)* '"'
    | '\'' (~['\\\r\n] | STRING_ESCAPE)* '\''
    ;

NAME: [a-zA-Z_][a-zA-Z_0-9]*;

COMMENT: '#' ~[\r\n\f]* -> skip;

SKIP_: [ \t]+ -> skip;

fragment SPACES: [ \t]+;
fragment DIGITS: [0-9]('_'? [0-9])*;
fragment HEX_DIGIT: [0-9a-fA-F];
fragment EXPONENT: [eE][+-]? DIGITS;
fragment STRING_ESCAPE: '\\' [btnfr"'\\];
