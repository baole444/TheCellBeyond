package scripting.transpiler.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JavaSourceWriterTest {
    @Test
    public void escapeString_plain() {
        assertEquals("\"hello\"", JavaSourceWriter.escapeStringLiteral("hello"));
    }

    @Test
    public void escapeString_specials() {
        assertEquals("\"\\n\"", JavaSourceWriter.escapeStringLiteral("\n"));
        assertEquals("\"\\t\"", JavaSourceWriter.escapeStringLiteral("\t"));
        assertEquals("\"\\r\"", JavaSourceWriter.escapeStringLiteral("\r"));
        assertEquals("\"\\\"\"", JavaSourceWriter.escapeStringLiteral("\""));
        assertEquals("\"\\\\\"", JavaSourceWriter.escapeStringLiteral("\\"));
    }

    @Test
    public void escapeString_controlCharsToUnicode() {
        assertEquals("\"\\u0007\"", JavaSourceWriter.escapeStringLiteral(""));
        assertEquals("\"é\"", JavaSourceWriter.escapeStringLiteral("é"));
    }

    @Test
    public void escapeChar_basic() {
        assertEquals("'a'", JavaSourceWriter.escapeCharLiteral('a'));
        assertEquals("'\\n'", JavaSourceWriter.escapeCharLiteral('\n'));
        assertEquals("'\\''", JavaSourceWriter.escapeCharLiteral('\''));
    }

    @Test
    public void importType_returnsSimpleName() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        assertEquals("CharacterBody2D", writer.importType("physic2d.CharacterBody2D"));
    }

    @Test
    public void importType_skipsJavaLang() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        assertEquals("String", writer.importType("java.lang.String"));
        assertEquals("package scripts;\n", writer.render());
    }

    @Test
    public void importType_skipsSamePackage() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        assertEquals("Other", writer.importType("scripts.Other"));
        assertEquals("package scripts;\n", writer.render());
    }

    @Test
    public void importBlock_sortedDeduped() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        writer.importType("c.Gamma");
        writer.importType("a.Alpha");
        writer.importType("b.Beta");
        writer.importType("a.Alpha");
        String expected = """
                package scripts;

                import a.Alpha;
                import b.Beta;
                import c.Gamma;
                """;
        assertEquals(expected, writer.render());
    }

    @Test
    public void importType_collisionThrows() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        writer.importType("a.Foo");
        assertThrows(IllegalStateException.class, () -> writer.importType("b.Foo"));
    }

    @Test
    public void indentation_nesting() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        writer.line("a");
        writer.indent();
        writer.line("b");
        writer.indent();
        writer.line("c");
        writer.dedent();
        writer.line("d");
        writer.dedent();
        writer.line("e");
        String expected = """
                package scripts;

                a
                    b
                        c
                    d
                e
                """;
        assertEquals(expected, writer.render());
    }

    @Test
    public void skeleton_simpleClass() {
        JavaSourceWriter writer = createSyntheticSource();
        String expected = """
                package scripts;

                import physic2d.CharacterBody2D;
                import scripting.Export;
                import scripting.RegisterGameObject;

                @RegisterGameObject
                public class MainPlayer extends CharacterBody2D {
                    @Export
                    public int speed = 200;
                    @Override
                    protected void onPhysicUpdate(float dt) {
                    }
                }
                """;
        assertEquals(expected, writer.render());
    }

    private static JavaSourceWriter createSyntheticSource() {
        JavaSourceWriter writer = new JavaSourceWriter("scripts");
        writer.importType("physic2d.CharacterBody2D");
        writer.importType("scripting.Export");
        writer.importType("scripting.RegisterGameObject");
        writer.annotation("@RegisterGameObject");
        writer.openType("public class MainPlayer extends CharacterBody2D");
        writer.annotation("@Export");
        writer.field("public int speed = 200");
        writer.annotation("@Override");
        writer.openMethod("protected void onPhysicUpdate(float dt)");
        writer.closeMethod();
        writer.closeType();
        return writer;
    }
}
