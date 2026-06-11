package TCBTest;

import org.junit.jupiter.api.Test;
import scripting.transpiler.ast.*;
import scripting.transpiler.parse.HeaderScanner;
import scripting.transpiler.parse.ScriptParser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TCBScriptParserTest {
    private static final String WorkingExample = """
            class MainPlayer extends CharacterBody2D
            @export var speed : int = 200
            func _physic_update(dt : float) -> void:
                moveAndSlide()
            """;

    private static ScriptFile parse(String source) {
        ScriptParser.Result result = ScriptParser.parse(source, "test.tcbs");
        assertFalse(result.hasErrors(), () -> "unexpected parse errors: " + result.errors);
        assertNotNull(result.scriptFile);
        return result.scriptFile;
    }

    @Test
    public void parsesWorkedExample() {
        ClassDeclaration cls = parse(WorkingExample).classDeclaration;
        assertEquals("MainPlayer", cls.name);
        assertEquals("CharacterBody2D", cls.superType.name);

        assertEquals(1, cls.fields.size());
        FieldDeclaration speed = cls.fields.getFirst();
        assertEquals("speed", speed.name);
        assertEquals("int", speed.type.name);
        assertFalse(speed.isConst);
        assertEquals(Visibility.Public, speed.visibility);
        assertEquals(1, speed.annotations.size());
        assertEquals("export", speed.annotations.getFirst().name);
        assertInstanceOf(LiteralExpression.class, speed.initializer);
        assertEquals(LiteralExpression.Kind.Integer, ((LiteralExpression) speed.initializer).kind);
        assertEquals("200", ((LiteralExpression) speed.initializer).text);

        assertEquals(1, cls.methods.size());
        MethodDeclaration method = cls.methods.getFirst();
        assertEquals("_physic_update", method.name);
        assertEquals("void", method.returnType.name);
        assertEquals(1, method.parameters.size());
        assertEquals("dt", method.parameters.getFirst().name);
        assertEquals("float", method.parameters.getFirst().type.name);

        assertEquals(1, method.body.statements.size());
        Statement first = method.body.statements.getFirst();
        ExpressionStatement exprStmt = assertInstanceOf(ExpressionStatement.class, first);
        MethodCallExpression call = assertInstanceOf(MethodCallExpression.class, exprStmt.expression);
        assertNull(call.target, "moveAndSlide() is an unqualified call");
        assertEquals("moveAndSlide", call.methodName);
        assertTrue(call.arguments.isEmpty());
    }

    @Test
    public void handlesNestedIndentedBlocks() {
        ScriptFile file = parse("""
                class Nest extends Object
                func run() -> void:
                    if ready:
                        while looping:
                            for item in items:
                                act()
                """);
        Block body = file.classDeclaration.methods.getFirst().body;
        IfStatement ifStmt = assertInstanceOf(IfStatement.class, body.statements.getFirst());
        WhileStatement whileStmt = assertInstanceOf(WhileStatement.class, ifStmt.thenBlock.statements.getFirst());
        ForStatement forStmt = assertInstanceOf(ForStatement.class, whileStmt.body.statements.getFirst());
        assertEquals("item", forStmt.variable);
        assertInstanceOf(ExpressionStatement.class, forStmt.body.statements.getFirst());
    }

    @Test
    public void inlineSuiteAndTernaryAndLogicalAliases() {
        ScriptFile file = parse("""
                class Demo extends Object
                func pick(flag : bool) -> int:
                    if flag and not done: return 1
                    elif flag && other: return 2
                    return flag ? 4 : 5
                """);
        Block body = file.classDeclaration.methods.getFirst().body;

        IfStatement ifStmt = assertInstanceOf(IfStatement.class, body.statements.getFirst());
        // Inline suite: single-statement then-block.
        assertEquals(1, ifStmt.thenBlock.statements.size());
        assertInstanceOf(ReturnStatement.class, ifStmt.thenBlock.statements.getFirst());

        // `and` + `not` normalize to And / Not.
        BinaryExpression cond = assertInstanceOf(BinaryExpression.class, ifStmt.condition);
        assertEquals(BinaryExpression.Operator.And, cond.operator);
        UnaryExpression notExpr = assertInstanceOf(UnaryExpression.class, cond.right);
        assertEquals(UnaryExpression.Operator.Not, notExpr.operator);

        // `&&` alias also normalizes to And.
        BinaryExpression elifCond = assertInstanceOf(BinaryExpression.class, ifStmt.elifClauses.getFirst().condition);
        assertEquals(BinaryExpression.Operator.And, elifCond.operator);

        // Trailing ternary.
        ReturnStatement ret = assertInstanceOf(ReturnStatement.class, body.statements.get(1));
        ConditionalExpression ternary = assertInstanceOf(ConditionalExpression.class, ret.value);
        assertEquals("4", ((LiteralExpression) ternary.thenValue).text);
        assertEquals("5", ((LiteralExpression) ternary.elseValue).text);
    }

    @Test
    public void headerOnlyModeReadsClassAndSuper() {
        Optional<HeaderScanner.ClassHeader> header = HeaderScanner.scan(WorkingExample);
        assertTrue(header.isPresent());
        assertEquals("MainPlayer", header.get().className());
        assertEquals("CharacterBody2D", header.get().superName());
    }

    @Test
    public void headerScanIsFastEnoughForProjectScan() {
        // Standard #3: under 5 ms each. Run many iterations and assert the average stays well under.
        int iterations = 1000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) HeaderScanner.scan(WorkingExample);
        double msEach = (System.nanoTime() - start) / 1_000_000.0 / iterations;
        assertTrue(msEach < 5.0, () -> "header scan averaged " + msEach + " ms");
    }

    @Test
    public void everyNodeCarriesSourcePosition() {
        // AstPrinter appends node.position to every line; a null position would render "@null".
        String tree = AstPrinter.print(parse(WorkingExample));
        assertFalse(tree.contains("@null"), () -> "a node had a null position:\n" + tree);
        assertTrue(tree.contains("test.tcbs:"), "positions name the source file");
    }

    @Test
    public void syntaxErrorReportsFileAndLine() {
        // Missing ':' after the method header.
        ScriptParser.Result result = ScriptParser.parse("""
                class Bad extends Object
                func oops() -> void
                    return
                """, "bad.tcbs");
        assertTrue(result.hasErrors());
        assertNull(result.scriptFile);
        assertEquals("bad.tcbs", result.errors.getFirst().file());
        assertTrue(result.errors.getFirst().line() >= 2,
                () -> "error should point at the header line, was line " + result.errors.getFirst().line());
    }

    @Test
    public void parsingIsDeterministic() {
        String first = AstPrinter.print(parse(WorkingExample));
        String second = AstPrinter.print(parse(WorkingExample));
        assertEquals(first, second);
    }

    private static void assertHeader(String source, String className, String superName) {
        Optional<HeaderScanner.ClassHeader> header = HeaderScanner.scan(source);
        assertTrue(header.isPresent(), () -> "no header found in:\n" + source);
        assertEquals(className, header.get().className());
        assertEquals(superName, header.get().superName());
    }

    @Test
    public void headerScanToleratesExcessiveWhitespace() {
        assertHeader("class     Player   extends   CharacterBody2D    ", "Player", "CharacterBody2D");
        assertHeader("\tclass\tPlayer\textends\tCharacterBody2D\t", "Player", "CharacterBody2D");
    }

    @Test
    public void headerScanToleratesTrailingComment() {
        assertHeader("class Player extends CharacterBody2D  # the hero", "Player", "CharacterBody2D");
    }

    @Test
    public void headerScanToleratesLeadingCommentAndDocLines() {
        assertHeader("""
                # a plain comment
                ## a doc comment
                class Player extends CharacterBody2D
                """, "Player", "CharacterBody2D");
    }

    @Test
    public void headerScanToleratesLeadingWhitespace() {
        assertHeader("    class Player extends CharacterBody2D", "Player", "CharacterBody2D");
    }

    @Test
    public void headerScanIgnoresCommentedOutHeader() {
        // A commented-out header must not be picked up; the real one below wins.
        assertHeader("# class Ghost extends Nothing\nclass Real extends Base\n", "Real", "Base");
    }

    @Test
    public void headerScanReturnsEmptyWhenNoHeader() {
        assertTrue(HeaderScanner.scan("var x : int = 5\n").isEmpty());
        assertTrue(HeaderScanner.scan("").isEmpty());
    }

    @Test
    public void headerScanReadsPlainClassWithoutExtends() {
        Optional<HeaderScanner.ClassHeader> header = HeaderScanner.scan("class Holder\nvar value : int = 0\n");
        assertTrue(header.isPresent());
        assertEquals("Holder", header.get().className());
        assertNull(header.get().superName(), "no extends clause means a null super name");
    }

    @Test
    public void headerScanReadsExtendsOnNextLine() {
        assertHeader("class Foo\nextends Bar\nvar x : int = 1\n", "Foo", "Bar");
    }

    @Test
    public void parsesExtendsOnNextLine() {
        ScriptFile file = parse("""
                class Foo
                extends Bar
                var x : int = 1
                """);
        assertEquals("Foo", file.classDeclaration.name);
        assertEquals("Bar", file.classDeclaration.superType.name);
        assertEquals(1, file.classDeclaration.fields.size());
    }

    @Test
    public void parsesExtendsOnNextLineAfterBlankAndCommentLines() {
        // Blank/comment lines between the class line and the extends line are skipped by the lexer.
        ScriptFile file = parse("""
                class Foo

                # pick a base
                extends Bar
                func run() -> void:
                    pass
                """);
        assertEquals("Bar", file.classDeclaration.superType.name);
        assertEquals(1, file.classDeclaration.methods.size());
    }

    @Test
    public void parsesPlainClassWithoutExtends() {
        // A data/helper class need not extend an engine type (implicitly extends Object).
        ScriptFile file = parse("""
                class Holder
                var value : int = 0
                """);
        ClassDeclaration cls = file.classDeclaration;
        assertEquals("Holder", cls.name);
        assertNull(cls.superType, "no extends clause means a null super type (implicit Object)");
        assertEquals(1, cls.fields.size());
        assertEquals("value", cls.fields.getFirst().name);

        // The printer must tolerate a null super type and emit no extends clause.
        String tree = AstPrinter.print(file);
        assertFalse(tree.contains("@null"), () -> "a node had a null position:\n" + tree);
        assertFalse(tree.contains("extends"), "plain class prints without an extends clause");
    }

    @Test
    public void parserToleratesWhitespaceAndComments() {
        ScriptFile file = parse("""
                # a leading comment
                class    Spaced   extends   Object
                # a comment between members
                @export var    x : int = 1    # trailing comment
                func    run()   ->   void:
                    act()   # inline trailing comment
                """);
        ClassDeclaration cls = file.classDeclaration;
        assertEquals("Spaced", cls.name);
        assertEquals("Object", cls.superType.name);
        assertEquals(1, cls.fields.size());
        assertEquals("x", cls.fields.getFirst().name);
        assertEquals("export", cls.fields.getFirst().annotations.getFirst().name);
        assertEquals(1, cls.methods.size());
        assertEquals("run", cls.methods.getFirst().name);
    }
}
