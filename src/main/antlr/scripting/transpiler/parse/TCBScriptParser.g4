parser grammar TCBScriptParser;

options {
    tokenVocab = TCBScriptLexer;
}

@header {
    package scripting.transpiler.parse;
}

// File / type

scriptFile: NEWLINE* typeDeclaration NEWLINE* EOF;

// One class or type per file
typeDeclaration: classDeclaration | enumDeclaration;

// Declaration of class support multiple and inline, where extends section can be dropped entirely (implicit Object.)
classDeclaration: CLASS NAME ( EXTENDS typeReference NEWLINE
                             | NEWLINE (EXTENDS typeReference NEWLINE)?
                             ) classMember*;

classMember: fieldDeclaration | methodDeclaration;

// Enum body is an indented block, constants first then an optional traling field declaration
enumDeclaration: ENUM NAME COLON NEWLINE INDENT enumConstant+ enumField* DEDENT;

enumConstant: NAME (OPEN_PAREN argumentList? CLOSE_PAREN)? NEWLINE;

enumField: (VAR | CONST) NAME COLON typeReference NEWLINE;

// Members

// Anotation may sit linline with the field or drop to their own  preceding lines.
// The type clause is optional, inferred from the initializer in the semantic pass when omitted.
fieldDeclaration
    : (annotation NEWLINE*)* visibility? STATIC? VAR NAME (COLON typeReference)? (ASSIGN expression)? NEWLINE #varField
    | (annotation NEWLINE*)* visibility? STATIC? CONST NAME (COLON typeReference)? ASSIGN expression NEWLINE #constField
    ;

methodDeclaration: visibility? STATIC? FUNC NAME OPEN_PAREN parameterList? CLOSE_PAREN (ARROW typeReference)? COLON suite;

parameterList: parameter (COMMA parameter)*;

parameter: NAME COLON typeReference (ASSIGN expression)?;

annotation: AT NAME (OPEN_PAREN annotationArgumentList? CLOSE_PAREN)?;

annotationArgumentList: annotationArgument (COMMA annotationArgument)*;

annotationArgument: NAME ASSIGN expression;

visibility: PUBLIC | PRIVATE | PROTECTED;

typeReference: NAME (OPEN_BRACK CLOSE_BRACK)*;

// Statements

suite: simpleStatement NEWLINE | NEWLINE INDENT statement+ DEDENT;

statement: simpleStatement NEWLINE | compoundStatement;

simpleStatement
    : localVariableDeclaration
    | returnStatement
    | breakStatement
    | continueStatement
    | passStatement
    | expressionStatement
    ;

compoundStatement: ifStatement | whileStatement | forStatement;

localVariableDeclaration
    : VAR NAME (COLON typeReference)? (ASSIGN expression)? #localVar
    | CONST NAME (COLON typeReference)? ASSIGN expression #localConst
    ;

returnStatement: RETURN expression?;

breakStatement: BREAK;

continueStatement: CONTINUE;

passStatement: PASS;

// A bare expression statement, optionally an assignment.
// The assignment target's validity (is an lvalue) is enforced in semantic pass, not in grammar.
expressionStatement: expression (assignmentOperator expression)?;

assignmentOperator
    : ASSIGN
    | PLUS_ASSIGN
    | MINUS_ASSIGN
    | STAR_ASSIGN
    | SLASH_ASSIGN
    | PERCENT_ASSIGN
    ;

ifStatement: IF expression COLON suite elifClause* elseClause?;

elifClause: ELIF expression COLON suite;

elseClause: ELSE COLON suite;

whileStatement: WHILE expression COLON suite;

forStatement: FOR NAME IN expression COLON suite;

// Expression, precdence by aolternative order, highest first.

expression
    : literal                                                           #literalExpr
    | NAME OPEN_PAREN argumentList? CLOSE_PAREN                         #callExpr
    | NAME                                                              #nameExpr
    | OPEN_PAREN expression CLOSE_PAREN                                 #parenExpr
    | expression OPEN_BRACK expression CLOSE_BRACK                      #indexExpr
    | expression DOT NAME OPEN_PAREN argumentList? CLOSE_PAREN          #methodCallExpr
    | expression DOT NAME                                               #memberAccessExpr
    | expression AS typeReference                                       #castExpr
    | expression IS typeReference                                       #typeCheckExpr
    | (MINUS | NOT | BANG) expression                                   #unaryExpr
    | expression (STAR | SLASH | PERCENT) expression                    #multiplicativeExpr
    | expression (PLUS | MINUS) expression                              #additiveExpr
    | expression (LT | GT | LE | GE) expression                         #relationalExpr
    | expression (EQ | NEQ) expression                                  #equalityExpr
    | expression (AND | AMP_AMP) expression                             #logicalAndExpr
    | expression (OR | PIPE_PIPE) expression                            #logicalOrExpr
    | <assoc=right> expression QUESTION expression COLON expression     #conditionalExpr
    ;

argumentList: expression (COMMA expression)*;

literal: INTEGER_LITERAL | FLOAT_LITERAL | STRING_LITERAL | TRUE | FALSE | NULL;