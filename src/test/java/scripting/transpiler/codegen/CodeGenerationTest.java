package scripting.transpiler.codegen;

import org.junit.jupiter.api.Test;
import scripting.transpiler.TranspilerProperties;
import scripting.transpiler.semantic.ProjectClassEntry;

import javax.tools.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CodeGenerationTest {
    private static final String WorkedExample = """
            class MainPlayer extends CharacterBody2D
            @export var speed : int = 200
            func _physic_update(dt : float) -> void:
                moveAndSlide()
            """;

    @Test
    public void workedExampleMatchesTarget() {
        String generated = ok(WorkedExample, "MainPlayer.tcbs", Map.of());
        String target = """
                package scripts;

                import physic2d.CharacterBody2D;
                import scripting.Export;
                import scripting.RegisterGameObject;

                @RegisterGameObject
                public class MainPlayer extends CharacterBody2D {
                    @Export public int speed = 200;
                    @Override
                    protected void onPhysicUpdate(float dt) {
                        moveAndSlide();
                    }
                }
                """;
        assertEquals(normalize(target), normalize(generated));
    }

    @Test
    public void workedExampleCompiles() {
        assertCompiles("MainPlayer", ok(WorkedExample, "MainPlayer.tcbs", Map.of()));
    }

    @Test
    public void crossScriptReferenceStaysSamePackageNoImport() {
        String generated = ok("""
                class User
                var partner : Helper
                """, "User.tcbs", index(script()));
        assertTrue(generated.contains("Helper partner;"), generated);
        assertFalse(generated.contains("import scripts.Helper;"), "a same package script needs no import");
        assertFalse(generated.contains("import scripts."), "no scripts package self import is ever emitted");
    }

    @Test
    public void inheritedApiMemberResolvesOnScriptTypedReceiver() {
        String generated = ok("""
                class Spotter
                var box : Hitbox
                func check() -> void:
                    if box != null: box.get_parent()
                """, "Spotter.tcbs", index(hitbox()));
        assertTrue(generated.contains("box.getParent()"), "an inherited API method resolves through a script type's API ancestor");
        assertFalse(generated.contains("get_parent"), generated);
    }

    @Test
    public void multipleLifecycleOverridesEachGetOverride() {
        String generated = ok("""
                class Mover extends CharacterBody2D
                func _ready() -> void:
                    pass
                func _physic_update(dt : float) -> void:
                    moveAndSlide()
                """, "Mover.tcbs", Map.of());
        assertEquals(2, count(generated), generated);
        assertTrue(generated.contains("protected void onReady()"), generated);
        assertTrue(generated.contains("protected void onPhysicUpdate(float dt)"), generated);
    }

    @Test
    public void deterministicAcrossRuns() {
        String first = ok(WorkedExample, "MainPlayer.tcbs", Map.of());
        String second = ok(WorkedExample, "MainPlayer.tcbs", Map.of());
        assertEquals(first, second);
    }

    @Test
    public void noSuperCallsAnywhere() {
        String generated = ok(WorkedExample, "MainPlayer.tcbs", Map.of());
        assertFalse(generated.contains("super."), "generated output never calls super");
    }

    @Test
    public void loggingBuiltinInjectsLoggerAndRoutesLevels() {
        String generated = ok("""
                class Chatty
                func run() -> void:
                    print("hello")
                    print_warning("careful")
                    print_error("boom")
                """, "Chatty.tcbs", Map.of());
        assertTrue(generated.contains("private static final EngineLog Logger = new EngineLog(Chatty.class);"), generated);
        assertTrue(generated.contains("import utility.log.EngineLog;"), generated);
        assertTrue(generated.contains("Logger.info(\"hello\");"), generated);
        assertTrue(generated.contains("Logger.warning(\"careful\");"), generated);
        assertTrue(generated.contains("Logger.error(\"boom\");"), generated);
    }

    @Test
    public void loggingBuiltinCompiles() {
        assertCompiles("Chatty", ok("""
                class Chatty
                func run() -> void:
                    print("hello")
                """, "Chatty.tcbs", Map.of()));
    }

    @Test
    public void noLoggerFieldWhenUnused() {
        String generated = ok("""
                class Quiet
                var value : int = 0
                """, "Quiet.tcbs", Map.of());
        assertFalse(generated.contains("Logger"), "no logger is injected when logging is unused");
        assertFalse(generated.contains("EngineLog"), generated);
    }

    @Test
    public void plainClassHasNoExtendsAndNoRegistration() {
        String generated = ok("""
                class Holder
                var value : int = 0
                """, "Holder.tcbs", Map.of());
        assertTrue(generated.contains("public class Holder {"), generated);
        assertFalse(generated.contains("extends"), generated);
        assertFalse(generated.contains("@Register"), generated);
    }

    @Test
    public void explicitExtendsObjectIsNormalizedToNoClause() {
        String generated = ok("""
                class Holder extends Object
                var value : int = 0
                """, "Holder.tcbs", Map.of());
        assertTrue(generated.contains("public class Holder {"), generated);
        assertFalse(generated.contains("extends"), generated);
        assertFalse(generated.contains("@Register"), generated);
    }

    @Test
    public void fieldModifiersComposeStaticAndFinalOrthogonally() {
        String generated = ok("""
                class Config
                var count : int = 0
                static var shared : int = 1
                const Max : int = 10
                static const SharedMax : int = 20
                func run() -> void:
                    const local : int = 5
                """, "Config.tcbs", Map.of());
        assertTrue(generated.contains("public int count = 0;"), generated);
        assertTrue(generated.contains("public static int shared = 1;"), generated);
        assertTrue(generated.contains("public final int Max = 10;"), generated);
        assertTrue(generated.contains("public static final int SharedMax = 20;"), generated);
        assertTrue(generated.contains("final int local = 5;"), generated);
        assertCompiles("Config", generated);
    }

    @Test
    public void bareEnumEmitsAndCompiles() {
        String generated = ok("""
                enum State:
                    Idle
                    Running
                """, "State.tcbs", Map.of());
        assertTrue(generated.contains("public enum State {"), generated);
        assertTrue(generated.contains("    Idle,"), generated);
        assertTrue(generated.contains("    Running\n"), "the last bare constant carries no terminator");
        assertCompiles("State", generated);
    }

    @Test
    public void valuedEnumEmitsConstantsFieldsConstructorAndCompiles() {
        String generated = ok("""
                enum Element:
                    Fire(10, "fire")
                    Water(5, "water")

                    var damage : int
                    var label : String
                """, "Element.tcbs", Map.of());
        assertTrue(generated.contains("Fire(10, \"fire\"),"), generated);
        assertTrue(generated.contains("Water(5, \"water\");"), "the last valued constant ends the list with a semicolon");
        assertTrue(generated.contains("public final int damage;"), generated);
        assertTrue(generated.contains("public final String label;"), generated);
        assertTrue(generated.contains("Element(int damage, String label) {"), generated);
        assertTrue(generated.contains("this.damage = damage;"), generated);
        assertTrue(generated.contains("this.label = label;"), generated);
        assertCompiles("Element", generated);
    }

    @Test
    public void enumConstantFloatArgumentGainsSuffix() {
        String generated = ok("""
                enum Wave:
                    Calm(0.5)
                    Storm(9.0)

                    var height : float
                """, "Wave.tcbs", Map.of());
        assertTrue(generated.contains("Calm(0.5f),"), "a float field suffixes the constant's literal argument");
        assertTrue(generated.contains("Storm(9.0f);"), generated);
        assertCompiles("Wave", generated);
    }

    @Test
    public void enumUsedAsTypeAndConstantReferenceEmits() {
        String generated = ok("""
                class Caster
                var element : Element = Element.Fire
                func run() -> void:
                    if element == Element.Water:
                        pass
                """, "Caster.tcbs", index(enumEntry("Element")));
        assertTrue(generated.contains("public Element element = Element.Fire;"), generated);
        assertTrue(generated.contains("element == Element.Water"), generated);
    }

    @Test
    public void signalEmitsPublicFinalFieldWithBoxedContractAndCompiles() {
        String generated = ok("""
                class Weapon extends CharacterBody2D
                signal weapon_cooldown(cooldown : float)
                signal hit(damage : int, source : CharacterBody2D)
                signal died
                """, "Weapon.tcbs", Map.of());
        assertTrue(generated.contains("public final Signal weapon_cooldown = new Signal(Float.class);"), generated);
        assertTrue(generated.contains("public final Signal hit = new Signal(Integer.class, CharacterBody2D.class);"), generated);
        assertTrue(generated.contains("public final Signal died = new Signal();"), generated);
        assertTrue(generated.contains("import signal.Signal;"), generated);
        assertCompiles("Weapon", generated);
    }

    @Test
    public void signalEmitAndConnectionShortcutsCompile() {
        String generated = ok("""
                class Weapon extends CharacterBody2D
                signal weapon_cooldown(cooldown : float)
                func fire() -> void:
                    weapon_cooldown.emit(2.5)
                func _ready() -> void:
                    weapon_cooldown.connect(self.on_cooldown)
                    weapon_cooldown.connect(on_cooldown)
                func on_cooldown(cooldown : float) -> void:
                    pass
                """, "Weapon.tcbs", Map.of());
        assertTrue(generated.contains("weapon_cooldown.emit(2.5f)"), generated);
        assertTrue(generated.contains("weapon_cooldown.connect(Callable.get(this, \"on_cooldown\"));"), "self.handler and bare handler both lower identically");
        assertTrue(generated.contains("import signal.Callable;"), generated);
        assertCompiles("Weapon", generated);
    }

    @Test
    public void signalSugarMatchesHandwrittenConstructorForm() {
        String sugar = ok("""
                class A
                signal spawned(body : CharacterBody2D)
                signal died
                """, "A.tcbs", Map.of());
        String handwritten = ok("""
                class A
                const spawned : Signal = Signal(CharacterBody2D.class)
                const died : Signal = Signal()
                """, "A.tcbs", Map.of());
        assertEquals(normalize(handwritten), normalize(sugar), "the signal sugar emits the same Java as the const + constructor form");
    }

    @Test
    public void instanceMethodConnectsAsCallableAndCompiles() {
        String generated = ok("""
                class Probe
                var sig : Signal
                var other : CharacterBody2D
                func run() -> void:
                    sig.connect(other.move_and_slide)
                """, "Probe.tcbs", Map.of());
        assertTrue(generated.contains("sig.connect(Callable.get(other, \"moveAndSlide\"));"), "a method reference on another instance binds a callable to it");
        assertCompiles("Probe", generated);
    }

    @Test
    public void crossScriptInstanceMethodConnectsAsCallable() {
        String generated = ok("""
                class Listener
                var weapon : Weapon
                func ready() -> void:
                    weapon.fired.connect(weapon.on_fired)
                """, "Listener.tcbs", index(weapon()));
        assertTrue(generated.contains("weapon.fired.connect(Callable.get(weapon, \"on_fired\"));"), "even when the signal is cross-script, the handler instance binds a callable");
    }

    @Test
    public void crossScriptSignalConnectionEmitsWithPassthroughName() {
        String generated = ok("""
                class Listener
                var weapon : Weapon
                func ready() -> void:
                    weapon.weapon_cooldown.connect(self.on_cooldown)
                func on_cooldown(cooldown : float) -> void:
                    pass
                """, "Listener.tcbs", index(weapon()));
        assertTrue(generated.contains("weapon.weapon_cooldown.connect(Callable.get(this, \"on_cooldown\"));"), "the cross-script signal name passes through verbatim, matching the emitter's field");
    }

    @Test
    public void selfEmitsThisAsReceiverAndArgument() {
        String generated = ok("""
                class Hero extends CharacterBody2D
                func _ready() -> void:
                    self.move_and_slide()
                    Callable.get(self, "on_hit")
                func on_hit(damage : int) -> void:
                    pass
                """, "Hero.tcbs", Map.of());
        assertTrue(generated.contains("this.moveAndSlide();"), generated);
        assertTrue(generated.contains("Callable.get(this, \"on_hit\");"), generated);
        assertCompiles("Hero", generated);
    }

    @Test
    public void bareConstructorEmitsNewAndCompiles() {
        String generated = ok("""
                class Builder
                var sig : Signal = Signal()
                """, "Builder.tcbs", Map.of());
        assertTrue(generated.contains("public Signal sig = new Signal();"), generated);
        assertTrue(generated.contains("import signal.Signal;"), generated);
        assertCompiles("Builder", generated);
    }

    @Test
    public void newKeywordConstructorEmitsIdenticallyAndCompiles() {
        String generated = ok("""
                class Builder
                var sig : Signal = new Signal()
                """, "Builder.tcbs", Map.of());
        assertTrue(generated.contains("public Signal sig = new Signal();"), "a leading new emits identically to the bare form");
        assertCompiles("Builder", generated);
    }

    @Test
    public void classLiteralEmitsAndCompiles() {
        String generated = ok("""
                class Reflect
                var bodyType : Object = CharacterBody2D.class
                var stringType : Object = String.class
                """, "Reflect.tcbs", Map.of());
        assertTrue(generated.contains("public Object bodyType = CharacterBody2D.class;"), generated);
        assertTrue(generated.contains("public Object stringType = String.class;"), generated);
        assertTrue(generated.contains("import physic2d.CharacterBody2D;"), generated);
        assertCompiles("Reflect", generated);
    }

    @Test
    public void selfConstructorAndClassLiteralComposeAndCompile() {
        String generated = ok("""
                class Weapon extends CharacterBody2D
                const cooldown : Signal = Signal(String.class)
                func _ready() -> void:
                    cooldown.connect(Callable.get(self, "on_cooldown"))
                func on_cooldown() -> void:
                    pass
                """, "Weapon.tcbs", Map.of());
        assertTrue(generated.contains("public final Signal cooldown = new Signal(String.class);"), generated);
        assertTrue(generated.contains("cooldown.connect(Callable.get(this, \"on_cooldown\"));"), generated);
        assertCompiles("Weapon", generated);
    }

    @Test
    public void inferredFieldEmitsConcreteTypeAndCompiles() {
        String generated = ok("""
                class Inferred
                var speed = 200
                var ratio = 2.5
                var label = "hi"
                var ready = true
                const Max = 10
                """, "Inferred.tcbs", Map.of());
        assertTrue(generated.contains("public int speed = 200;"), generated);
        assertTrue(generated.contains("public float ratio = 2.5f;"), "an inferred float field keeps the f-suffix");
        assertTrue(generated.contains("public String label = \"hi\";"), generated);
        assertTrue(generated.contains("public boolean ready = true;"), generated);
        assertTrue(generated.contains("public final int Max = 10;"), generated);
        assertCompiles("Inferred", generated);
    }

    @Test
    public void exportOnInferredFieldEmitsAndCompiles() {
        String generated = ok("""
                class Tuned
                @export var speed = 200
                """, "Tuned.tcbs", Map.of());
        assertTrue(generated.contains("@Export"), generated);
        assertTrue(generated.contains("public int speed = 200;"), "the @export field's type is inferred to int");
        assertCompiles("Tuned", generated);
    }

    @Test
    public void inferredLocalEmitsConcreteTypeWhenObtainableAndCompiles() {
        String generated = ok("""
                class Locals
                func run() -> void:
                    var count = 5
                    var ratio = 1.5
                    var ready = true
                """, "Locals.tcbs", Map.of());
        assertTrue(generated.contains("int count = 5;"), generated);
        assertTrue(generated.contains("float ratio = 1.5f;"), "an inferred float local keeps the f-suffix");
        assertTrue(generated.contains("boolean ready = true;"), generated);
        assertCompiles("Locals", generated);
    }

    @Test
    public void inferredLocalFromApiCallEmitsConcreteTypeAndTranslatesCall() {
        String generated = ok("""
                class Probe
                var cam : Camera2D
                func run() -> void:
                    var copy = cam.copy()
                    copy.currently_active()
                """, "Probe.tcbs", Map.of());
        assertTrue(generated.contains("Camera2D copy = cam.copy();"), generated);
        assertTrue(generated.contains("copy.currentlyActive();"), "the inferred receiver type translates the snake-case call");
        assertCompiles("Probe", generated);
    }

    @Test
    public void inferredFieldFromArithmeticEmitsAndCompiles() {
        String generated = ok("""
                class Arith
                var a = 3
                var b = 5
                var sum = a + b
                var ratio = a * 1.5
                var less = a < b
                """, "Arith.tcbs", Map.of());
        assertTrue(generated.contains("public int sum = (a + b);"), generated);
        assertTrue(generated.contains("public float ratio = (a * 1.5f);"), "the inferred float result suffixes the literal");
        assertTrue(generated.contains("public boolean less = (a < b);"), generated);
        assertCompiles("Arith", generated);
    }

    @Test
    public void inferredFieldFromStringConcatEmitsAndCompiles() {
        String generated = ok("""
                class Concat
                var name = "hero"
                var greeting = "hi " + name
                """, "Concat.tcbs", Map.of());
        assertTrue(generated.contains("public String greeting = (\"hi \" + name);"), generated);
        assertCompiles("Concat", generated);
    }

    @Test
    public void inferredFieldFromUnaryEmitsAndCompiles() {
        String generated = ok("""
                class Unary
                var ready = true
                var blocked = not ready
                var a = 5
                var neg = -a
                """, "Unary.tcbs", Map.of());
        assertTrue(generated.contains("public boolean blocked = (!ready);"), generated);
        assertTrue(generated.contains("public int neg = (-a);"), generated);
        assertCompiles("Unary", generated);
    }

    @Test
    public void inferredLocalFallsBackToVarWhenConcreteTypeUnobtainable() {
        String generated = ok("""
                class Fallback
                func compute() -> int:
                    return 5
                func run() -> void:
                    var result = compute()
                    result += 1
                """, "Fallback.tcbs", Map.of());
        assertTrue(generated.contains("var result = compute();"), "a user-method call has no tracked return type, so the local falls back to var");
        assertCompiles("Fallback", generated);
    }

    @Test
    public void floatLiteralGainsSuffixForFloatTarget() {
        String generated = ok("""
                class Phys
                var gravity : float = 9.8
                var count : int = 5
                func run(step : float) -> void:
                    var speed : float = 1.5
                    var whole : float = 200
                    speed = 2.5
                    gravity = 3.5
                    step = 4.5
                """, "Phys.tcbs", Map.of());
        assertTrue(generated.contains("float gravity = 9.8f;"), generated);
        assertTrue(generated.contains("int count = 5;"), generated);
        assertTrue(generated.contains("float speed = 1.5f;"), generated);
        assertTrue(generated.contains("float whole = 200;"), "an int literal widens to float without a suffix");
        assertTrue(generated.contains("speed = 2.5f;"), "reassigning a float local suffixes the literal");
        assertTrue(generated.contains("gravity = 3.5f;"), "reassigning a float field suffixes the literal");
        assertTrue(generated.contains("step = 4.5f;"), "reassigning a float parameter suffixes the literal");
    }

    @Test
    public void floatLiteralInArgumentPositionGainsSuffix() {
        String generated = ok("""
                class Calc
                func scale(factor : float) -> void:
                    pass
                func run() -> void:
                    scale(1.5)
                """, "Calc.tcbs", Map.of());
        assertTrue(generated.contains("scale(1.5f)"), "a float literal argument keeps float precision, so it narrows into a float parameter");
        assertCompiles("Calc", generated);
    }

    @Test
    public void exportAnnotationCarriesArguments() {
        String generated = ok("""
                class Tuned
                @export(label = "Speed") var speed : int = 5
                """, "Tuned.tcbs", Map.of());
        assertTrue(generated.contains("@Export(label = \"Speed\")"), generated);
    }

    @Test
    public void boolTypeMapsToJavaBoolean() {
        String generated = ok("""
                class Flag
                var ready : bool = true
                """, "Flag.tcbs", Map.of());
        assertTrue(generated.contains("public boolean ready = true;"), generated);
    }

    @Test
    public void stringLiteralIsRequotedToDoubleQuotes() {
        String generated = ok("""
                class Greeter
                var name : String = 'world'
                """, "Greeter.tcbs", Map.of());
        assertTrue(generated.contains("public String name = \"world\";"), generated);
    }

    @Test
    public void forLoopOverArrayEmitsEnhancedForAndCompiles() {
        String generated = ok("""
                class Counter
                var total : int = 0
                var values : int[]
                func run() -> void:
                    for value in values:
                        total += value
                """, "Counter.tcbs", Map.of());
        assertTrue(generated.contains("public int[] values;"), generated);
        assertTrue(generated.contains("for (var value : values) {"), generated);
        assertTrue(generated.contains("total += value;"), generated);
        assertCompiles("Counter", generated);
    }

    @Test
    public void rangeLoopLowersToIndexedForAndCompiles() {
        String generated = ok("""
                class Ranger
                var total : int = 0
                func run(count : int) -> void:
                    for i in range(count):
                        total += i
                    for j in range(2, count):
                        total += j
                    for k in range(0, count, 2):
                        total += k
                """, "Ranger.tcbs", Map.of());
        assertTrue(generated.contains("for (int i = 0; i < count; i += 1) {"), generated);
        assertTrue(generated.contains("for (int j = 2; j < count; j += 1) {"), generated);
        assertTrue(generated.contains("for (int k = 0; k < count; k += 2) {"), generated);
        assertCompiles("Ranger", generated);
    }

    @Test
    public void arrayLoopStaysEnhancedForAndTranslatesElementCall() {
        String generated = ok("""
                class Mover
                var bodies : CharacterBody2D[]
                func run() -> void:
                    for body in bodies:
                        body.move_and_slide()
                """, "Mover.tcbs", Map.of());
        assertTrue(generated.contains("for (var body : bodies) {"), generated);
        assertTrue(generated.contains("body.moveAndSlide();"), "the inferred element type translates the snake-case call");
        assertCompiles("Mover", generated);
    }

    @Test
    public void controlFlowEmitsAndCompiles() {
        String generated = ok("""
                class Control
                var total : int = 0
                func run(limit : int) -> void:
                    var i : int = 0
                    while i < limit:
                        if i == 3:
                            break
                        elif i == 1:
                            i += 1
                            continue
                        else:
                            total += i
                        i += 1
                """, "Control.tcbs", Map.of());
        assertTrue(generated.contains("while ("), generated);
        assertTrue(generated.contains("} else if ("), generated);
        assertTrue(generated.contains("} else {"), generated);
        assertTrue(generated.contains("break;"), generated);
        assertTrue(generated.contains("continue;"), generated);
        assertCompiles("Control", generated);
    }

    private static String ok(String source, String fileName, Map<String, ProjectClassEntry> projectIndex) {
        Transpiler.Result result = Transpiler.transpile(source, fileName, projectIndex);
        assertFalse(result.hasErrors(), () -> "unexpected transpile errors: " + result.errors());
        assertNotNull(result.javaSource());
        return result.javaSource();
    }

    /**
     * Compile generated source in memory against the test classpath, which carries the engine classes and their dependencies.
     */
    private static void assertCompiles(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "a JDK java compiler is required for this test");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///" + TranspilerProperties.ScriptPackage + "/" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        List<String> options = List.of("-d", tempDir().toString(), "-classpath", System.getProperty("java.class.path"));
        boolean success = compiler.getTask(null, null, diagnostics, options, null, List.of(unit)).call();
        List<String> errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(Object::toString)
                .toList();
        assertTrue(success && errors.isEmpty(), () -> "generated source failed to compile: " + errors + "\n" + source);
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("tcbs-codegen");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String normalize(String source) {
        return source.lines()
                .filter(line -> !line.stripLeading().startsWith("//"))
                .reduce("", (a, b) -> a + "\n" + b)
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static int count(String script) {
        int total = 0;
        int from = 0;
        while ((from = script.indexOf("@Override", from)) >= 0) {
            total++;
            from += "@Override".length();
        }
        return total;
    }

    private static Map<String, ProjectClassEntry> index(ProjectClassEntry... entries) {
        Map<String, ProjectClassEntry> map = new HashMap<>();
        for (ProjectClassEntry entry : entries) map.put(entry.simpleName(), entry);
        return map;
    }

    private static ProjectClassEntry script() {
        return new ProjectClassEntry("Helper", "src/script/" + "Helper" + ".tcbs", ProjectClassEntry.Kind.Script, "Object", "scripts", false);
    }

    private static ProjectClassEntry enumEntry(String name) {
        return new ProjectClassEntry(name, "src/script/" + name + ".tcbs", ProjectClassEntry.Kind.Script, null, "scripts", true);
    }

    private static ProjectClassEntry weapon() {
        return new ProjectClassEntry("Weapon", "src/script/Weapon.tcbs", ProjectClassEntry.Kind.Script, "Object", "scripts", false);
    }

    private static ProjectClassEntry hitbox() {
        return new ProjectClassEntry("Hitbox", "src/script/Hitbox.tcbs", ProjectClassEntry.Kind.Script, "Area2D", "scripts", false);
    }
}
