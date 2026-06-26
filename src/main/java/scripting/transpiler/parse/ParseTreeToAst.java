package scripting.transpiler.parse;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import scripting.transpiler.ast.*;
import scripting.transpiler.parse.TCBScriptParser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ParseTreeToAst walks the ANTLR parse tree and produces the dedicated AST.
 * Only the expression alternatives are dispatched through the generated base visitor, allow resolving expression of any form.
 * Structural and statement nodes are built by direct, typed recursion.
 * <p>
 * It also populates the {@link SourcePosition} from token line and column info for error tracking.
 */
public final class ParseTreeToAst extends TCBScriptParserBaseVisitor<AstNode> {
    private final String file;

    public ParseTreeToAst(String file) {
        this.file = file;
    }

    public ScriptFile convert(ScriptFileContext context) {
        return new ScriptFile(position(context), typeDeclaration(context.typeDeclaration()));
    }

    private TypeDeclaration typeDeclaration(TypeDeclarationContext context) {
        if (context.classDeclaration() != null) return classDeclaration(context.classDeclaration());
        return enumDeclaration(context.enumDeclaration());
    }

    @Override
    public AstNode visitLiteralExpr(LiteralExprContext context) {
        LiteralContext literal = context.literal();
        LiteralExpression.Kind kind;
        if (literal.INTEGER_LITERAL() != null) kind = LiteralExpression.Kind.Integer;
        else if (literal.FLOAT_LITERAL() != null) kind = LiteralExpression.Kind.Float;
        else if (literal.STRING_LITERAL() != null) kind = LiteralExpression.Kind.String;
        else if (literal.NULL() != null) kind = LiteralExpression.Kind.Null;
        else kind = LiteralExpression.Kind.Boolean;
        return new LiteralExpression(position(literal), kind, literal.getText());
    }

    @Override
    public AstNode visitNameExpr(NameExprContext context) {
        return new IdentifierExpression(position(context), context.NAME().getText());
    }

    @Override
    public AstNode visitCallExpr(CallExprContext context) {
        return new MethodCallExpression(position(context), null, context.NAME().getText(), arguments(context.argumentList()));
    }

    @Override
    public AstNode visitSelfExpr(SelfExprContext context) {
        return new SelfExpression(position(context));
    }

    @Override
    public AstNode visitNewExpr(NewExprContext context) {
        TypeReference type = new TypeReference(position(context.NAME()), context.NAME().getText(), 0);
        return new ConstructorCallExpression(position(context), type, arguments(context.argumentList()));
    }

    @Override
    public AstNode visitClassLiteralExpr(ClassLiteralExprContext context) {
        TypeReference type = new TypeReference(position(context.NAME()), context.NAME().getText(), 0);
        return new ClassLiteralExpression(position(context), type);
    }

    @Override
    public AstNode visitParenExpr(ParenExprContext context) {
        return expression(context.expression());
    }

    @Override
    public AstNode visitIndexExpr(IndexExprContext context) {
        return new IndexExpression(position(context), expression(context.expression(0)), expression(context.expression(1)));
    }

    @Override
    public AstNode visitMethodCallExpr(MethodCallExprContext context) {
        return new MethodCallExpression(position(context), expression(context.expression()), context.NAME().getText(), arguments(context.argumentList()));
    }

    @Override
    public AstNode visitMemberAccessExpr(MemberAccessExprContext context) {
        return new MemberAccessExpression(position(context), expression(context.expression()), context.NAME().getText());
    }

    @Override
    public AstNode visitCastExpr(CastExprContext context) {
        return new CastExpression(position(context), expression(context.expression()), typeReference(context.typeReference()));
    }

    @Override
    public AstNode visitTypeCheckExpr(TypeCheckExprContext context) {
        return new TypeCheckExpression(position(context), expression(context.expression()), typeReference(context.typeReference()));
    }

    @Override
    public AstNode visitUnaryExpr(UnaryExprContext context) {
        UnaryExpression.Operator operator = context.MINUS() != null ? UnaryExpression.Operator.Negate : UnaryExpression.Operator.Not;
        return new UnaryExpression(position(context), operator, expression(context.expression()));
    }

    @Override
    public AstNode visitMultiplicativeExpr(MultiplicativeExprContext context) {
        BinaryExpression.Operator operator;
        if (context.STAR() != null) operator = BinaryExpression.Operator.Multiply;
        else if (context.SLASH() != null) operator = BinaryExpression.Operator.Divide;
        else operator = BinaryExpression.Operator.Modulo;
        return binary(context, operator, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitAdditiveExpr(AdditiveExprContext context) {
        BinaryExpression.Operator operator = context.PLUS() != null ? BinaryExpression.Operator.Add : BinaryExpression.Operator.Subtract;
        return binary(context, operator, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitRelationalExpr(RelationalExprContext context) {
        BinaryExpression.Operator operator;
        if (context.LT() != null) operator = BinaryExpression.Operator.Less;
        else if (context.GT() != null) operator = BinaryExpression.Operator.Greater;
        else if (context.LE() != null) operator = BinaryExpression.Operator.LessEqual;
        else operator = BinaryExpression.Operator.GreaterEqual;
        return binary(context, operator, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitEqualityExpr(EqualityExprContext context) {
        BinaryExpression.Operator operator = context.EQ() != null ? BinaryExpression.Operator.Equal : BinaryExpression.Operator.NotEqual;
        return binary(context, operator, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitLogicalAndExpr(LogicalAndExprContext context) {
        return binary(context, BinaryExpression.Operator.And, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitLogicalOrExpr(LogicalOrExprContext context) {
        return binary(context, BinaryExpression.Operator.Or, context.expression(0), context.expression(1));
    }

    @Override
    public AstNode visitConditionalExpr(ConditionalExprContext context) {
        return new ConditionalExpression(position(context), expression(context.expression(0)), expression(context.expression(1)), expression(context.expression(2)));
    }

    private EnumDeclaration enumDeclaration(EnumDeclarationContext context) {
        List<EnumConstant> constants = new ArrayList<>();
        for (EnumConstantContext constant : context.enumConstant()) constants.add(enumConstant(constant));
        List<FieldDeclaration> fields = new ArrayList<>();
        for (EnumFieldContext field : context.enumField()) fields.add(enumField(field));
        return new EnumDeclaration(position(context), context.NAME().getText(), constants, fields);
    }

    private EnumConstant enumConstant(EnumConstantContext context) {
        return new EnumConstant(position(context), context.NAME().getText(), arguments(context.argumentList()));
    }

    private FieldDeclaration enumField(EnumFieldContext context) {
        boolean isConst = context.CONST() != null;
        return new FieldDeclaration(position(context), List.of(), Visibility.Public, false, isConst, context.NAME().getText(), typeReference(context.typeReference()), null);
    }

    private ClassDeclaration classDeclaration(ClassDeclarationContext context) {
        String name = context.NAME().getText();
        TypeReference superType = context.typeReference() != null ? typeReference(context.typeReference()) : null;
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        for (ClassMemberContext member : context.classMember()) {
            if (member.fieldDeclaration() != null) fields.add(fieldDeclaration(member.fieldDeclaration()));
            else methods.add(methodDeclaration(member.methodDeclaration()));
        }
        return new ClassDeclaration(position(context), name, superType, fields, methods);
    }

    private FieldDeclaration fieldDeclaration(FieldDeclarationContext context) {
        if (context instanceof VarFieldContext var) {
            Expression init = var.expression() != null ? expression(var.expression()) : null;
            return new FieldDeclaration(position(var), annotations(var.annotation()), visibility(var.visibility()), var.STATIC() != null, false, var.NAME().getText(), optionalType(var.typeReference()), init);
        }
        ConstFieldContext constField = (ConstFieldContext) context;
        return new FieldDeclaration(position(constField), annotations(constField.annotation()), visibility(constField.visibility()), constField.STATIC() != null, true, constField.NAME().getText(), optionalType(constField.typeReference()), expression(constField.expression()));
    }

    private MethodDeclaration methodDeclaration(MethodDeclarationContext context) {
        List<ParameterDeclaration> params = context.parameterList() != null ? parameters(context.parameterList()) : List.of();
        TypeReference returnType = context.typeReference() != null ? typeReference(context.typeReference()) : null;
        return new MethodDeclaration(position(context), visibility(context.visibility()), context.STATIC() != null, context.NAME().getText(), params, returnType, suite(context.suite()));
    }

    private List<ParameterDeclaration> parameters(ParameterListContext context) {
        List<ParameterDeclaration> parameters = new ArrayList<>();
        for (ParameterContext param : context.parameter()) {
            Expression def = param.expression() != null ? expression(param.expression()) : null;
            parameters.add(new ParameterDeclaration(position(param), param.NAME().getText(), typeReference(param.typeReference()), def));
        }
        return parameters;
    }

    private List<Annotation> annotations(List<AnnotationContext> contexts) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationContext context : contexts) {
            List<AnnotationArgument> args = new ArrayList<>();
            if (context.annotationArgumentList() != null) for (AnnotationArgumentContext arg : context.annotationArgumentList().annotationArgument()) args.add(new AnnotationArgument(position(arg), arg.NAME().getText(), expression(arg.expression())));
            annotations.add(new Annotation(position(context), context.NAME().getText(), args));
        }
        return annotations;
    }

    private TypeReference typeReference(TypeReferenceContext context) {
        return new TypeReference(position(context), context.NAME().getText(), context.OPEN_BRACK().size());
    }

    private TypeReference optionalType(TypeReferenceContext context) {
        return context != null ? typeReference(context) : null;
    }

    private Block suite(SuiteContext context) {
        List<Statement> statements = new ArrayList<>();
        if (context.simpleStatement() != null) statements.add(simpleStatement(context.simpleStatement()));
        else for (StatementContext s : context.statement()) statements.add(statement(s));
        return new Block(position(context), statements);
    }

    private Statement statement(StatementContext context) {
        if (context.simpleStatement() != null) return simpleStatement(context.simpleStatement());
        return compoundStatement(context.compoundStatement());
    }

    private Statement simpleStatement(SimpleStatementContext context) {
        if (context.localVariableDeclaration() != null) return localVariableDeclaration(context.localVariableDeclaration());
        if (context.returnStatement() != null) return returnStatement(context.returnStatement());
        if (context.breakStatement() != null) return new BreakStatement(position(context.breakStatement()));
        if (context.continueStatement() != null) return new ContinueStatement(position(context.continueStatement()));
        if (context.passStatement() != null) return new PassStatement(position(context.passStatement()));
        return expressionStatement(context.expressionStatement());
    }

    private Statement compoundStatement(CompoundStatementContext context) {
        if (context.ifStatement() != null) return ifStatement(context.ifStatement());
        if (context.whileStatement() != null) return whileStatement(context.whileStatement());
        return forStatement(context.forStatement());
    }

    private Statement localVariableDeclaration(LocalVariableDeclarationContext context) {
        if (context instanceof LocalVarContext var) {
            Expression init = var.expression() != null ? expression(var.expression()) : null;
            return new LocalVariableDeclaration(position(var), false, var.NAME().getText(), optionalType(var.typeReference()), init);
        }
        LocalConstContext constField = (LocalConstContext) context;
        return new LocalVariableDeclaration(position(constField), true, constField.NAME().getText(), optionalType(constField.typeReference()), expression(constField.expression()));
    }

    private ReturnStatement returnStatement(ReturnStatementContext context) {
        Expression value = context.expression() != null ? expression(context.expression()) : null;
        return new ReturnStatement(position(context), value);
    }

    private Statement expressionStatement(ExpressionStatementContext context) {
        Expression target = expression(context.expression(0));
        if (context.assignmentOperator() == null) return new ExpressionStatement(position(context), target);
        return new AssignmentStatement(position(context), target, assignmentOperator(context.assignmentOperator()), expression(context.expression(1)));
    }

    private static AssignmentStatement.Operator assignmentOperator(AssignmentOperatorContext context) {
        if (context.PLUS_ASSIGN() != null) return AssignmentStatement.Operator.AddAssign;
        if (context.MINUS_ASSIGN() != null) return AssignmentStatement.Operator.SubtractAssign;
        if (context.STAR_ASSIGN() != null) return AssignmentStatement.Operator.MultiplyAssign;
        if (context.SLASH_ASSIGN() != null) return AssignmentStatement.Operator.DivideAssign;
        if (context.PERCENT_ASSIGN() != null) return AssignmentStatement.Operator.ModuloAssign;
        return AssignmentStatement.Operator.Assign;
    }

    private IfStatement ifStatement(IfStatementContext context) {
        Block thenBlock = suite(context.suite());
        List<ElifClause> elifs = new ArrayList<>();
        for (ElifClauseContext elif : context.elifClause()) elifs.add(new ElifClause(position(elif), expression(elif.expression()), suite(elif.suite())));
        Block elseBlock = context.elseClause() != null ? suite(context.elseClause().suite()) : null;
        return new IfStatement(position(context), expression(context.expression()), thenBlock, elifs, elseBlock);
    }

    private WhileStatement whileStatement(WhileStatementContext context) {
        return new WhileStatement(position(context), expression(context.expression()), suite(context.suite()));
    }

    private ForStatement forStatement(ForStatementContext context) {
        return new ForStatement(position(context), context.NAME().getText(), expression(context.expression()), suite(context.suite()));
    }

    private Expression expression(ExpressionContext context) {
        return (Expression) visit(context);
    }

    private BinaryExpression binary(ParserRuleContext context, BinaryExpression.Operator operator, ExpressionContext left, ExpressionContext right) {
        return new BinaryExpression(position(context), operator, expression(left), expression(right));
    }

    private List<Expression> arguments(ArgumentListContext context) {
        if (context == null) return List.of();
        List<Expression> expressions = new ArrayList<>();
        for (ExpressionContext e : context.expression()) expressions.add(expression(e));
        return expressions;
    }

    private SourcePosition position(ParserRuleContext context) {
        return token(context.getStart());
    }

    private SourcePosition position(TerminalNode node) {
        return token(node.getSymbol());
    }

    private SourcePosition token(Token token) {
        return new SourcePosition(file, token.getLine(), token.getCharPositionInLine());
    }

    private static Visibility visibility(VisibilityContext context) {
        if (context == null) return Visibility.Public;
        if (context.PRIVATE() != null) return Visibility.Private;
        if (context.PROTECTED() != null) return Visibility.Protected;
        return Visibility.Public;
    }
}
