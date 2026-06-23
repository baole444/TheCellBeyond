package scripting.transpiler.codegen;

import org.junit.jupiter.api.Test;
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

public class CodeGenerationTest {    private static final String WorkedExample = """
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
                class User extends Object
                var partner : Helper
                """, "User.tcbs", index(script()));
        assertTrue(generated.contains("Helper partner;"), generated);
        assertFalse(generated.contains("import scripts.Helper;"), "a same package script needs no import");
        assertFalse(generated.contains("import scripts."), "no scripts package self import is ever emitted");
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
                class Chatty extends Object
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
                class Chatty extends Object
                func run() -> void:
                    print("hello")
                """, "Chatty.tcbs", Map.of()));
    }

    @Test
    public void noLoggerFieldWhenUnused() {
        String generated = ok("""
                class Quiet extends Object
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
    public void fieldModifiersComposeStaticAndFinalOrthogonally() {
        String generated = ok("""
                class Config extends Object
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
                class Caster extends Object
                var element : Element = Element.Fire
                func run() -> void:
                    if element == Element.Water:
                        pass
                """, "Caster.tcbs", index(enumEntry("Element")));
        assertTrue(generated.contains("public Element element = Element.Fire;"), generated);
        assertTrue(generated.contains("element == Element.Water"), generated);
    }

    @Test
    public void floatLiteralGainsSuffixForFloatTarget() {
        String generated = ok("""
                class Phys extends Object
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
    public void exportAnnotationCarriesArguments() {
        String generated = ok("""
                class Tuned extends Object
                @export(label = "Speed") var speed : int = 5
                """, "Tuned.tcbs", Map.of());
        assertTrue(generated.contains("@Export(label = \"Speed\")"), generated);
    }

    @Test
    public void boolTypeMapsToJavaBoolean() {
        String generated = ok("""
                class Flag extends Object
                var ready : bool = true
                """, "Flag.tcbs", Map.of());
        assertTrue(generated.contains("public boolean ready = true;"), generated);
    }

    @Test
    public void stringLiteralIsRequotedToDoubleQuotes() {
        String generated = ok("""
                class Greeter extends Object
                var name : String = 'world'
                """, "Greeter.tcbs", Map.of());
        assertTrue(generated.contains("public String name = \"world\";"), generated);
    }

    @Test
    public void forLoopOverArrayEmitsEnhancedForAndCompiles() {
        String generated = ok("""
                class Counter extends Object
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
        JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///" + Transpiler.GeneratedPackage + "/" + className + ".java"), JavaFileObject.Kind.SOURCE) {
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
}
