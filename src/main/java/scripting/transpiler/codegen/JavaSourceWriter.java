package scripting.transpiler.codegen;

import scripting.transpiler.TranspilerProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * The source writer emit valid java source text, including imports, string and char escaping, indentation and declaration frames.
 * This is used by the code generator as it walk the AST tree recursively to build the mechanical shaping underneath.
 * <p>
 * Escaping policy: the escape set is {@code \n \t \r \\}, with {@code \"} for string, and {@code \'} for chars.
 * Control chars below {@code 0x20} or char {@code 0x7F} become {@code \\uXXXX}.
 * Other character, including printable none ASCII, is emitted as is, relying on UTF-8 encoding.
 */
public final class JavaSourceWriter {
    private final String packageName;
    private final Map<String, String> importBySimpleName = new HashMap<>();
    private final StringBuilder body = new StringBuilder();
    private int indentDepth;
    private String fileComment;

    public JavaSourceWriter(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Register a fully qualified name and return the simple name to use in the body.
     * The same FQN may be registered repeatedly.
     * <p>
     * On {@code java.lang} or type in the same package register without emitting an import.
     * @param fqn the fully qualified name to register, such as {@code physic2d.CharacterBody2D}
     * @return the simple name to emit in the body
     * @throws IllegalStateException when two distinct FQNs map to the same simple name, this should already be caught during semantic pass
     */
    public String importType(String fqn) {
        String simpleName = simpleNameOf(fqn);
        String current = importBySimpleName.get(simpleName);
        if (current != null && !current.equals(fqn)) throw new IllegalStateException(String.format("Import collision for simple name '%s': %s vs %s", simpleName, current, fqn));
        importBySimpleName.put(simpleName, fqn);
        return simpleName;
    }

    public void fileComment(String text) {
        this.fileComment = text;
    }

    public void annotation(String text) {
        line(text);
    }

    public void openType(String declarationLine) {
        line(declarationLine + " {");
        indent();
    }

    public void closeType() {
        dedent();
        line("}");
    }

    public void field(String declarationLine) {
        line(declarationLine + ";");
    }

    public void openMethod(String signatureLine) {
        line(signatureLine + " {");
        indent();
    }

    public void closeMethod() {
        dedent();
        line("}");
    }

    public void indent() {
        indentDepth++;
    }

    public void dedent() {
        if (indentDepth > 0) indentDepth--;
    }

    /**
     * Append the current indentation, text, and a new line.
     * @param text the line content to append after the indentation
     */
    public void line(String text) {
        body.repeat(TranspilerProperties.IndentUnit, Math.max(0, indentDepth));
        body.append(text).append('\n');
    }

    public void blankLine() {
        body.append('\n');
    }

    /**
     * Assemble the compilation unit, including file comment, package line, sorted import block, then the buffered body.
     * @return the compiled Java source in a single string
     */
    public String render() {
        StringBuilder result = new StringBuilder();
        if (fileComment != null) result.append(fileComment).append('\n');
        result.append("package ").append(packageName).append(";\n");
        String imports = renderImports();
        if (!imports.isEmpty()) result.append('\n').append(imports);
        if (!body.isEmpty()) result.append('\n').append(body);
        return result.toString();
    }

    /**
     * Add quotation and escaping java string literal for the given value.
     * @param value the raw string to escape
     * @return the escaped literal, including the surrounding double quotes
     */
    public static String escapeStringLiteral(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2);
        result.append('"');
        for (int i = 0; i < value.length(); i++) appendEscaped(result, value.charAt(i), false);
        result.append('"');
        return result.toString();
    }

    /**
     * Add quotation and escaping java char literal for the given value.
     * @param value the character to escape
     * @return the escaped literal, including the surrounding single quotes
     */
    public static String escapeCharLiteral(char value) {
        StringBuilder result = new StringBuilder(4);
        result.append('\'');
        appendEscaped(result, value, true);
        result.append('\'');
        return result.toString();
    }

    private String renderImports() {
        TreeSet<String> FQNs = new TreeSet<>();
        importBySimpleName.values().stream().filter(this::shouldImport).forEach(FQNs::add);
        if (FQNs.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        FQNs.forEach(fqn -> result.append("import ").append(fqn).append(";\n"));
        return result.toString();
    }

    private boolean shouldImport(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot < 0) return false;
        String pkg = fqn.substring(0, lastDot);
        if (pkg.equals(TranspilerProperties.JavaLangPackage)) return false;
        return !pkg.equals(packageName);
    }

    private static String simpleNameOf(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private static void appendEscaped(StringBuilder result, char value, boolean charContext) {
        switch (value) {
            case '\n' -> result.append("\\n");
            case '\t' -> result.append("\\t");
            case '\r' -> result.append("\\r");
            case '\\' -> result.append("\\\\");
            case '"' -> result.append(charContext ? "\"" : "\\\"");
            case '\'' -> result.append(charContext ? "\\'" : "'");
            default -> result.append(isControl(value) ? unicodeEscape(value) : String.valueOf(value));
        }
    }

    private static boolean isControl(char value) {
        return value < 0x20 || value == 0x7F;
    }

    private static String unicodeEscape(char value) {
        return String.format("\\u%04x", (int) value);
    }
}
