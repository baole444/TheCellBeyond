package scripting.transpiler;

import scripting.Export;
import scripting.RegisterComponent;
import scripting.RegisterGameObject;
import scripting.transpiler.semantic.LifecycleTable;
import signal.Signal;
import utility.log.EngineLog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Properties used by the transpiler pipeline.
 */
public final class TranspilerProperties {
    /**
     * Matches {@code class Name [extends Parent]} or {@code enum Name} at the start of a line, ignores lines with leading comment.
     * {@code extends} section may sit on the same or drop to a following line.
     */
    public static final Pattern TypeHeaderRegex = Pattern.compile("(?m)^[ \\t]*(class|enum)[ \\t]+([A-Za-z_][A-Za-z_0-9]*)(?:\\s+extends[ \\t]+([A-Za-z_][A-Za-z_0-9]*))?");
    /**
     * Matches the Java package declaration.
     */
    public static final Pattern JavaPackage = Pattern.compile("(?m)^[ \\t]*package[ \\t]+([A-Za-z_][A-Za-z_0-9.]*)[ \\t]*;");
    /**
     * Matches the Java class and enum declaration.
     */
    public static final Pattern JavaType = Pattern.compile("(?m)^[ \\t]*(?:@[\\w.]+(?:\\([^)]*\\))?[ \\t]*)*(?:[a-z][\\w-]*[ \\t]+)*(class|enum)[ \\t]+([A-Za-z_]\\w*)(?:[ \\t]*<[^>]*>)?(?:[ \\t]+extends[ \\t]+([A-Za-z_][\\w.]*))?");
    /**
     * Classpath where the API Manifest is stored.
     */
    public static final String ManifestIndexPath = "META-INF/script-api-index.json";
    /**
     * The directory under Gradle's build output directory where the transpiler put the translated script classes into.
     */
    public static final String TranspilerOutputDir = "generated/tcb-script-java";
    /**
     * TCBScript file extension.
     */
    public static final String ScriptFileExtension = ".tcbs";
    /**
     * Java source file extension.
     */
    public static final String JavaFileExtension = ".java";
    /**
     * The flat package where every scripts' generated java files are belong to.
     */
    public static final String ScriptPackage = "scripts";
    /**
     * Default build output of Gradle build tool.
     */
    public static final String BuildDir = "build";
    /**
     * 4 spaces per indent unit, equal to 1 TAB, {@code /t}.
     */
    public static final String IndentUnit = "    ";
    /**
     * The main Java package.
     */
    public static final String JavaLangPackage = "java.lang";
    /**
     * Java {@link Object}'s simple name.
     */
    public static final String ObjectType = Object.class.getSimpleName();
    /**
     * TCB engine {@link Signal}'s simple name.
     */
    public static final String SignalType = Signal.class.getSimpleName();
    /**
     * TCBScript export annotation.
     */
    public static final String ExportAnnotation = "export";
    /**
     * TCB engine {@link Export} annotation FQN.
     */
    public static final String ExportFQN = Export.class.getName();
    /**
     * TCB engine {@link EngineLog} record FQN.
     */
    public static final String EngineLogFQN = EngineLog.class.getName();
    /**
     * TCB engine {@link RegisterGameObject} annotation FQN.
     */
    public static final String RegisterGameObjectFQN = RegisterGameObject.class.getName();
    /**
     * TCB engine {@link RegisterComponent} annotation FQN.
     */
    public static final String RegisterComponentFQN = RegisterComponent.class.getName();
    /**
     * TCB engine {@link Signal} class FQN.
     */
    public static final String SignalFQN = Signal.class.getName();
    /**
     * The set of TCBScript builtin types.
     */
    public static final Set<String> BuiltInTypes = Set.of("int", "float", "bool", "String", "void", ObjectType);
    /**
     * TCBScript builtin type names mapped to their java spelling.
     */
    public static final Map<String, String> BuiltInToJavas = Map.of(
            "int", "int",
            "float", "float",
            "bool", "boolean",
            "String", "String",
            "void", "void",
            ObjectType, ObjectType
    );
    /**
     * TCBScript life cycle function hook's names to its engine target.
     */
    public static final Map<String, LifecycleTable.LifecycleHook> LifeCycle = Map.of(
            "_start", new LifecycleTable.LifecycleHook("onStart", List.of()),
            "_editor_start", new LifecycleTable.LifecycleHook("onEditorStart", List.of()),
            "_ready", new LifecycleTable.LifecycleHook("onReady", List.of()),
            "_update", new LifecycleTable.LifecycleHook("onUpdate", List.of("float")),
            "_editor_update", new LifecycleTable.LifecycleHook("onEditorUpdate", List.of("float")),
            "_physic_update", new LifecycleTable.LifecycleHook("onPhysicUpdate", List.of("float")),
            "_destroy", new LifecycleTable.LifecycleHook("onDestroy", List.of()),
            "_transform_dirty", new LifecycleTable.LifecycleHook("onTransformDirty", List.of())
    );
    /**
     * TCBScript builtin logging functions.
     */
    public static final Map<String, String> LogBuiltIns = Map.of(
            "print", "info",
            "print_debug", "debug",
            "print_info", "info",
            "print_warning", "warning",
            "print_error", "error"
    );
    /**
     * The set of Java's reserved keyword.
     */
    public static final Set<String> JavaKeywords = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    );
}
