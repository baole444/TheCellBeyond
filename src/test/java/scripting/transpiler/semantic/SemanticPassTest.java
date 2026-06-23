package scripting.transpiler.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import scripting.transpiler.ast.*;
import scripting.transpiler.parse.ScriptParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticPassTest {
    private static final String WorkedExample = """
            class MainPlayer extends CharacterBody2D
            @export var speed : int = 200
            func _physic_update(dt : float) -> void:
                moveAndSlide()
            """;

    @Test
    public void workedExampleResolvesFully() {
        ClassDeclaration cls = ok(WorkedExample, Map.of());
        Resolution.APIClassResolution superRes = assertInstanceOf(Resolution.APIClassResolution.class, cls.superType.resolution);
        assertEquals("physic2d.CharacterBody2D", superRes.fqn());
        assertEquals(ClassRegistration.GameObject, cls.registration);
        MethodDeclaration hook = cls.methods.getFirst();
        Resolution.LifecycleResolution lifecycle = assertInstanceOf(Resolution.LifecycleResolution.class, hook.resolution);
        assertEquals("onPhysicUpdate", lifecycle.javaName());
        MethodCallExpression call = firstCall(hook);
        Resolution.APIMemberResolution member = assertInstanceOf(Resolution.APIMemberResolution.class, call.resolution);
        assertEquals("physic2d.CharacterBody2D", member.receiverClassFQN());
        assertEquals("moveAndSlide", member.javaName());
    }

    @Test
    public void userMemberResolution() {
        ClassDeclaration cls = ok("""
                class Logic extends Object
                var counter : int = 0
                func tick() -> void:
                    step()
                func step() -> void:
                    counter = counter + 1
                """, Map.of());
        MethodCallExpression call = firstCall(cls.methods.getFirst());
        Resolution.UserMemberResolution step = assertInstanceOf(Resolution.UserMemberResolution.class, call.resolution);
        assertEquals("step", step.name());
        AssignmentStatement assign = assertInstanceOf(AssignmentStatement.class, cls.methods.get(1).body.statements.getFirst());
        IdentifierExpression target = assertInstanceOf(IdentifierExpression.class, assign.target);
        assertInstanceOf(Resolution.UserMemberResolution.class, target.resolution);
    }

    @Test
    public void projectClassTypeResolution() {
        ClassDeclaration cls = ok("""
                class User extends Object
                var helper : Helper
                """, index(script("Helper", "Object")));
        Resolution.ProjectClassResolution helper = assertInstanceOf(Resolution.ProjectClassResolution.class, cls.fields.getFirst().type.resolution);
        assertEquals("scripts.Helper", helper.fqn());
    }

    @Test
    public void apiMemberResolvesSnakeAndCamel() {
        ClassDeclaration cls = ok("""
                class Mover extends CharacterBody2D
                func _physic_update(dt : float) -> void:
                    move_and_slide()
                    moveAndSlide()
                """, Map.of());
        List<Statement> body = cls.methods.getFirst().body.statements;
        for (int i = 0; i < 2; i++) {
            MethodCallExpression call = assertInstanceOf(MethodCallExpression.class, ((ExpressionStatement) body.get(i)).expression);
            Resolution.APIMemberResolution member = assertInstanceOf(Resolution.APIMemberResolution.class, call.resolution, "both snake and camel spellings resolve to the same API member");
            assertEquals("moveAndSlide", member.javaName());
        }
    }

    @Test
    public void apiClassTypeResolution() {
        ClassDeclaration cls = ok("""
                class Holder extends Object
                var body : CharacterBody2D
                """, Map.of());
        Resolution.APIClassResolution body = assertInstanceOf(Resolution.APIClassResolution.class, cls.fields.getFirst().type.resolution);
        assertEquals("physic2d.CharacterBody2D", body.fqn());
    }

    @Test
    public void unresolvedIdentifierErrors() {
        SemanticAnalyzer.Result result = run("""
                class Bad extends Object
                func run() -> void:
                    doesNotExist()
                """, Map.of());
        assertTrue(result.hasErrors());
        SemanticError error = result.errors().getFirst();
        assertEquals("test.tcbs", error.file());
        assertEquals(3, error.line(), "error points at the offending call");
        assertTrue(error.message().contains("doesNotExist"), error::message);
    }

    @Test
    public void qualifiedTypedReceiverResolves() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                var body : CharacterBody2D
                func run() -> void:
                    body.move_and_slide()
                """, Map.of());
        MethodCallExpression call = firstCall(cls.methods.getFirst());
        Resolution.APIMemberResolution member = assertInstanceOf(Resolution.APIMemberResolution.class, call.resolution);
        assertEquals("physic2d.CharacterBody2D", member.receiverClassFQN());
        assertEquals("moveAndSlide", member.javaName());
    }

    @Test
    public void staticClassReceiverResolves() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                func run() -> void:
                    Input.get_input_action("jump")
                """, Map.of());
        MethodCallExpression call = firstCall(cls.methods.getFirst());
        Resolution.APIMemberResolution member = assertInstanceOf(Resolution.APIMemberResolution.class, call.resolution);
        assertEquals("TheCellBeyond.Input", member.receiverClassFQN());
        assertEquals("getInputAction", member.javaName());
    }

    @Test
    public void builderChainResolvesThroughReturnType() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                var cam : Camera2D
                func run() -> void:
                    cam.copy().currently_active()
                """, Map.of());
        MethodCallExpression outer = firstCall(cls.methods.getFirst());
        assertEquals("currentlyActive", assertInstanceOf(Resolution.APIMemberResolution.class, outer.resolution).javaName());
        MethodCallExpression inner = assertInstanceOf(MethodCallExpression.class, outer.target);
        assertEquals("copy", assertInstanceOf(Resolution.APIMemberResolution.class, inner.resolution).javaName());
    }

    @Test
    public void chainDeadEndsGracefullyOnPrimitiveReturn() {
        SemanticAnalyzer.Result result = run("""
                class Probe extends Object
                var cam : Camera2D
                func run() -> void:
                    cam.currently_active().whatever()
                """, Map.of());
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        MethodCallExpression outer = firstCall(classOf(result).methods.getFirst());
        assertNull(outer.resolution, "member on a primitive-returning call passes through unresolved");
    }

    @Test
    public void projectTypedReceiverPassesThrough() {
        SemanticAnalyzer.Result result = run("""
                class Probe extends Object
                var helper : Helper
                func run() -> void:
                    helper.do_thing()
                """, index(script("Helper", "Object")));
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        MethodCallExpression call = firstCall(classOf(result).methods.getFirst());
        assertNull(call.resolution);
    }

    @Test
    public void typelessLocalVarIsRejectedByParser() {
        ScriptParser.Result parsed = ScriptParser.parse("""
                class C
                func run() -> void:
                    var x = 5
                """, "test.tcbs");
        assertTrue(parsed.hasErrors(), "a var without a type annotation must not parse");
    }

    @Test
    public void forLoopArrayElementTypeIsInferred() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                var bodies : CharacterBody2D[]
                func run() -> void:
                    for body in bodies:
                        body.move_and_slide()
                """, Map.of());
        ForStatement loop = assertInstanceOf(ForStatement.class, cls.methods.getFirst().body.statements.getFirst());
        MethodCallExpression call = assertInstanceOf(MethodCallExpression.class, ((ExpressionStatement) loop.body.statements.getFirst()).expression);
        Resolution.APIMemberResolution member = assertInstanceOf(Resolution.APIMemberResolution.class, call.resolution, "the loop variable takes the array element type, so the member resolves");
        assertEquals("physic2d.CharacterBody2D", member.receiverClassFQN());
        assertEquals("moveAndSlide", member.javaName());
    }

    @Test
    public void rangeIterableResolvesAsIndexLoopBuiltin() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                var total : int = 0
                func run(count : int) -> void:
                    for i in range(0, count):
                        total += i
                """, Map.of());
        ForStatement loop = assertInstanceOf(ForStatement.class, cls.methods.getFirst().body.statements.getFirst());
        MethodCallExpression iterable = assertInstanceOf(MethodCallExpression.class, loop.iterable);
        assertInstanceOf(Resolution.BuiltinRangeResolution.class, iterable.resolution);
    }

    @Test
    public void rangeWithTooManyArgumentsFails() {
        SemanticAnalyzer.Result result = run("""
                class Probe extends Object
                func run() -> void:
                    for i in range(0, 10, 2, 5):
                        pass
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("range"), () -> result.errors().toString());
    }

    @Test
    public void rangeIsContextualKeywordOnlyInLoopPosition() {
        ClassDeclaration cls = ok("""
                class Probe extends Object
                var total : int = 0
                func range(n : int) -> int:
                    return n
                func run() -> void:
                    var direct : int = range(5)
                    for i in range(3):
                        total += i
                """, Map.of());
        MethodDeclaration run = cls.methods.get(1);
        LocalVariableDeclaration local = assertInstanceOf(LocalVariableDeclaration.class, run.body.statements.getFirst());
        MethodCallExpression elsewhere = assertInstanceOf(MethodCallExpression.class, local.initializer);
        assertInstanceOf(Resolution.UserMemberResolution.class, elsewhere.resolution, "outside the loop, range(...) is the user method");
        ForStatement loop = assertInstanceOf(ForStatement.class, run.body.statements.get(1));
        MethodCallExpression iterable = assertInstanceOf(MethodCallExpression.class, loop.iterable);
        assertInstanceOf(Resolution.BuiltinRangeResolution.class, iterable.resolution, "in the loop position the range built in always wins");
    }

    @Test
    public void stateSubclassOverridesResolve() {
        ClassDeclaration cls = ok("""
                class Walking extends State
                func on_state_enter() -> void:
                    pass
                func update(dt : float) -> void:
                    pass
                func physic_update(dt : float) -> void:
                    pass
                func is_state_enter_condition_met() -> bool:
                    return true
                """, Map.of());
        assertEquals(ClassRegistration.None, cls.registration, "State is neither GameObject nor Component");
        assertOverride(cls.methods.get(0), "onStateEnter");
        assertOverride(cls.methods.get(1), "update");
        assertOverride(cls.methods.get(2), "physicUpdate");
        assertOverride(cls.methods.get(3), "isStateEnterConditionMet");
    }

    @Test
    public void sameNamedNewMethodIsNotAFalseOverride() {
        ClassDeclaration cls = ok("""
                class Walking extends State
                func update() -> void:
                    pass
                """, Map.of());
        assertNull(cls.methods.getFirst().resolution, "no-arg update is a new overload, not an override");
    }

    @Test
    public void selfCallToOwnOverrideResolvesToJavaName() {
        ClassDeclaration cls = ok("""
                class Walking extends State
                func on_state_enter() -> void:
                    pass
                func update(dt : float) -> void:
                    on_state_enter()
                """, Map.of());
        assertOverride(cls.methods.get(0), "onStateEnter");
        MethodCallExpression selfCall = firstCall(cls.methods.get(1));
        assertEquals("onStateEnter", assertInstanceOf(Resolution.APIMemberResolution.class, selfCall.resolution).javaName());
    }

    @Test
    public void selfCallToOwnLifecycleHookResolvesToJavaName() {
        ClassDeclaration cls = ok("""
                class Mover extends CharacterBody2D
                func _physic_update(dt : float) -> void:
                    pass
                func restart() -> void:
                    _physic_update(0.0)
                """, Map.of());
        MethodCallExpression selfCall = firstCall(cls.methods.get(1));
        assertEquals("onPhysicUpdate", assertInstanceOf(Resolution.LifecycleResolution.class, selfCall.resolution).javaName());
    }

    private static void assertOverride(MethodDeclaration method, String javaName) {
        Resolution.APIMemberResolution resolution = assertInstanceOf(Resolution.APIMemberResolution.class, method.resolution, () -> "method '" + method.name + "' should resolve as an API override");
        assertEquals(javaName, resolution.javaName());
        assertEquals("components.State", resolution.receiverClassFQN());
    }

    @Test
    public void componentRegistration() {
        ClassDeclaration cls = ok("""
                class Sprite extends AnimatedSpriteRenderer
                func _ready() -> void:
                    pass
                """, Map.of());
        assertEquals(ClassRegistration.Component, cls.registration);
        assertInstanceOf(Resolution.LifecycleResolution.class, cls.methods.getFirst().resolution);
    }

    @Test
    public void plainClassExtendingNonRegisterableApiIsAllowed() {
        ClassDeclaration cls = ok("""
                class Boom extends Sound
                var loud : bool = true
                """, Map.of());
        assertEquals(ClassRegistration.None, cls.registration);
    }

    @Test
    public void plainClassWithoutExtends() {
        ClassDeclaration cls = ok("""
                class Holder
                var value : int = 0
                """, Map.of());
        assertEquals(ClassRegistration.None, cls.registration);
        assertNull(cls.superType);
    }

    @Test
    public void extendingFinalTypeFails() {
        SemanticAnalyzer.Result result = run("""
                class Hack extends Input
                var x : int = 0
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().toLowerCase().contains("final"), () -> result.errors().toString());
    }

    @Test
    public void extendingBuiltinTypeFails() {
        SemanticAnalyzer.Result result = run("""
                class Weird extends String
                var x : int = 0
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("String"), () -> result.errors().toString());
    }

    @Test
    @Timeout(5)
    public void circularExtendsFailsWithoutInfiniteLoop() {
        SemanticAnalyzer.Result result = run("""
                class A extends B
                var x : int = 0
                """, index(script("A", "B"), script("B", "A")));
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().toLowerCase().contains("circular"), () -> result.errors().toString());
    }

    @Test
    public void exportOnPlainVarIsAllowed() {
        assertFalse(run("""
                class C
                @export var speed : int = 1
                """, Map.of()).hasErrors());
    }

    @Test
    public void exportOnConstFails() {
        SemanticAnalyzer.Result result = run("""
                class C
                @export const Max : int = 10
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("@export"), () -> result.errors().toString());
    }

    @Test
    public void exportOnStaticConstFails() {
        SemanticAnalyzer.Result result = run("""
                class C
                @export static const Max : int = 10
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("@export"), () -> result.errors().toString());
    }

    @Test
    public void exportOnStaticVarIsAllowed() {
        assertFalse(run("""
                class C
                @export static var shared : int = 1
                """, Map.of()).hasErrors());
    }

    @Test
    public void valuedEnumResolvesWithoutErrors() {
        assertFalse(run("""
                enum Element:
                    Fire(10, "fire")
                    Water(5, "water")

                    var damage : int
                    var label : String
                """, Map.of()).hasErrors());
    }

    @Test
    public void enumConstantArgCountMismatchFails() {
        SemanticAnalyzer.Result result = run("""
                enum Element:
                    Fire(10)

                    var damage : int
                    var label : String
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("argument"), () -> result.errors().toString());
    }

    @Test
    public void enumConstantArgTypeMismatchFails() {
        SemanticAnalyzer.Result result = run("""
                enum Element:
                    Fire("hot")

                    var damage : int
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("damage"), () -> result.errors().toString());
    }

    @Test
    public void classExtendingEnumFails() {
        SemanticAnalyzer.Result result = run("""
                class Spell extends Element
                var power : int = 1
                """, index(enumEntry("Element")));
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("enum"), () -> result.errors().toString());
    }

    @Test
    public void reservedKeywordAsFieldFails() {
        SemanticAnalyzer.Result result = run("""
                class C
                var new : int = 0
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("reserved"), () -> result.errors().toString());
    }

    @Test
    public void reservedKeywordAsMethodFails() {
        SemanticAnalyzer.Result result = run("""
                class C
                func synchronized() -> void:
                    pass
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("reserved"), () -> result.errors().toString());
    }

    @Test
    public void lifecycleArityMismatchFails() {
        SemanticAnalyzer.Result result = run("""
                class C extends CharacterBody2D
                func _physic_update() -> void:
                    pass
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("parameter"), () -> result.errors().toString());
    }

    @Test
    public void lifecycleParameterTypeMismatchFails() {
        SemanticAnalyzer.Result result = run("""
                class C extends CharacterBody2D
                func _physic_update(dt : int) -> void:
                    pass
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("float"), () -> result.errors().toString());
    }

    @Test
    public void lifecycleNonVoidReturnFails() {
        SemanticAnalyzer.Result result = run("""
                class C extends CharacterBody2D
                func _physic_update(dt : float) -> int:
                    return 0
                """, Map.of());
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().toLowerCase().contains("void"), () -> result.errors().toString());
    }

    @Test
    public void scannerIndexesScriptsAndJava(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/Player.tcbs", "class Player extends CharacterBody2D\n");
        writeFile(root, "src/main/java/util/Helper.java", "package util;\npublic class Helper extends Object {}\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        ProjectClassEntry player = result.index().get("Player");
        assertEquals(ProjectClassEntry.Kind.Script, player.kind());
        assertEquals("CharacterBody2D", player.superClassRef());
        assertEquals("scripts.Player", player.fqn());
        ProjectClassEntry helper = result.index().get("Helper");
        assertEquals(ProjectClassEntry.Kind.Java, helper.kind());
        assertEquals("util.Helper", helper.fqn());
    }

    @Test
    public void scannerIndexesEnums(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/State.tcbs", "enum State:\n    Idle\n    Running\n");
        writeFile(root, "src/script/Player.tcbs", "class Player extends Object\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        ProjectClassEntry state = result.index().get("State");
        assertTrue(state.isEnum());
        assertEquals(ProjectClassEntry.Kind.Script, state.kind());
        assertEquals("scripts.State", state.fqn());
        assertFalse(result.index().get("Player").isEnum());
    }

    @Test
    public void scannerIndexesJavaEnum(@TempDir Path root) throws IOException {
        writeFile(root, "src/main/java/data/Suit.java", "package data;\npublic enum Suit implements java.io.Serializable {\n    Hearts, Spades\n}\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        ProjectClassEntry suit = result.index().get("Suit");
        assertTrue(suit.isEnum());
        assertEquals(ProjectClassEntry.Kind.Java, suit.kind());
        assertEquals("data.Suit", suit.fqn());
        assertNull(suit.superClassRef(), "a Java enum has no extends target");
    }

    @Test
    public void scannerIgnoresGeneratedJavaEnum(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/Mood.tcbs", "enum Mood:\n    Happy\n    Sad\n");
        writeFile(root, "build/generated/script-java/scripts/Mood.java", "package scripts;\npublic enum Mood { Happy, Sad }\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        assertEquals(ProjectClassEntry.Kind.Script, result.index().get("Mood").kind(), "the .tcbs enum is the only Mood indexed; the generated Java enum under build/ is excluded");
    }

    @Test
    public void scannerDetectsDuplicateAcrossClassAndEnum(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/Shape.tcbs", "class Shape extends Object\n");
        writeFile(root, "src/script/ShapeEnum.tcbs", "enum Shape:\n    Round\n    Square\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertTrue(result.hasErrors());
        assertTrue(result.errors().getFirst().message().contains("Duplicate"), () -> result.errors().toString());
    }

    @Test
    public void scannerFindsScriptsAnywhere(@TempDir Path root) throws IOException {
        writeFile(root, "Loose.tcbs", "class Loose extends Object\n");
        writeFile(root, "src/script/Nested.tcbs", "class Nested extends Object\n");
        writeFile(root, "gameplay/enemies/Goblin.tcbs", "class Goblin extends Object\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        assertEquals("scripts.Loose", result.index().get("Loose").fqn());
        assertEquals("scripts.Nested", result.index().get("Nested").fqn());
        assertEquals("scripts.Goblin", result.index().get("Goblin").fqn());
    }

    @Test
    public void scannerParsesJavaHeaderVariants(@TempDir Path root) throws IOException {
        writeFile(root, "src/main/java/Rooted.java", "public class Rooted {}\n");
        writeFile(root, "src/main/java/pkg/Qualified.java", "package pkg;\npublic class Qualified extends a.b.Base {}\n");
        writeFile(root, "src/main/java/pkg/Generic.java", "package pkg;\nabstract class Generic<T> extends Holder<T> {}\n");
        writeFile(root, "src/main/java/pkg/Impl.java", "package pkg;\nfinal class Impl implements Runnable {}\n");
        writeFile(root, "src/main/java/pkg/Annotated.java", "package pkg;\n@Entity public class Annotated extends Object {}\n");
        writeFile(root, "src/main/java/pkg/WithArgs.java", "package pkg;\n@SuppressWarnings(\"x\") final class WithArgs {}\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        ProjectClassEntry rooted = result.index().get("Rooted");
        assertEquals("Rooted", rooted.fqn());
        assertNull(rooted.superClassRef());
        ProjectClassEntry qualified = result.index().get("Qualified");
        assertEquals("Base", qualified.superClassRef());
        assertEquals("pkg.Qualified", qualified.fqn());
        assertEquals("Holder", result.index().get("Generic").superClassRef());
        assertNull(result.index().get("Impl").superClassRef());
        assertEquals("Object", result.index().get("Annotated").superClassRef());
        assertNull(result.index().get("WithArgs").superClassRef());
    }

    @Test
    public void scannerFindsJavaInNonMainSourceSet(@TempDir Path root) throws IOException {
        writeFile(root, "src/custom/java/com/foo/Custom.java", "package com.foo;\npublic class Custom extends Object {}\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        ProjectClassEntry custom = result.index().get("Custom");
        assertEquals(ProjectClassEntry.Kind.Java, custom.kind());
        assertEquals("com.foo.Custom", custom.fqn());
    }

    @Test
    public void scannerIgnoresGeneratedBuildOutput(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/Hero.tcbs", "class Hero extends Object\n");
        writeFile(root, "build/generated/script-java/scripts/Hero.java", "package scripts;\npublic class Hero {}\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertFalse(result.hasErrors(), () -> result.errors().toString());
        assertEquals(ProjectClassEntry.Kind.Script, result.index().get("Hero").kind(), "the .tcbs source is the only Hero indexed");
    }

    @Test
    public void scannerDetectsDuplicateClassName(@TempDir Path root) throws IOException {
        writeFile(root, "src/script/Bar.tcbs", "class Foo extends Object\n");
        writeFile(root, "src/script/Foo.tcbs", "class Foo extends Object\n");
        ProjectScanner.Result result = ProjectScanner.scan(root);
        assertTrue(result.hasErrors());
        String message = result.errors().getFirst().message();
        assertTrue(message.contains("Bar.tcbs") && message.contains("Foo"), message);
        assertTrue(result.errors().getFirst().file().contains("Foo.tcbs"), () -> result.errors().toString());
    }

    @Test
    @Timeout(5)
    public void scannerCompletesUnderOneSecondFor100Scripts(@TempDir Path root) throws IOException {
        for (int i = 0; i < 100; i++) writeFile(root, "src/script/Script" + i + ".tcbs", "class Script" + i + " extends Object\n");
        long start = System.nanoTime();
        ProjectScanner.Result result = ProjectScanner.scan(root);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals(100, result.index().size());
        assertFalse(result.hasErrors());
        assertTrue(millis < 1000, () -> "scan of 100 scripts took " + millis + " ms");
    }

    private static SemanticAnalyzer.Result run(String source, Map<String, ProjectClassEntry> projectIndex) {
        ScriptParser.Result parsed = ScriptParser.parse(source, "test.tcbs");
        assertFalse(parsed.hasErrors(), () -> "unexpected parse errors: " + parsed.errors);
        return SemanticAnalyzer.analyze(parsed.scriptFile, projectIndex);
    }

    private static ClassDeclaration ok(String source, Map<String, ProjectClassEntry> projectIndex) {
        SemanticAnalyzer.Result result = run(source, projectIndex);
        assertFalse(result.hasErrors(), () -> "unexpected semantic errors: " + result.errors());
        return (ClassDeclaration) result.scriptFile().typeDeclaration;
    }

    private static ClassDeclaration classOf(SemanticAnalyzer.Result result) {
        return (ClassDeclaration) result.scriptFile().typeDeclaration;
    }

    private static MethodCallExpression firstCall(MethodDeclaration method) {
        ExpressionStatement statement = assertInstanceOf(ExpressionStatement.class, method.body.statements.getFirst());
        return assertInstanceOf(MethodCallExpression.class, statement.expression);
    }

    private static Map<String, ProjectClassEntry> index(ProjectClassEntry... entries) {
        Map<String, ProjectClassEntry> map = new HashMap<>();
        for (ProjectClassEntry entry : entries) map.put(entry.simpleName(), entry);
        return map;
    }

    private static ProjectClassEntry script(String name, String superName) {
        return new ProjectClassEntry(name, "src/script/" + name + ".tcbs", ProjectClassEntry.Kind.Script, superName, "scripts", false);
    }

    private static ProjectClassEntry enumEntry(String name) {
        return new ProjectClassEntry(name, "src/script/" + name + ".tcbs", ProjectClassEntry.Kind.Script, null, "scripts", true);
    }

    private static void writeFile(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
