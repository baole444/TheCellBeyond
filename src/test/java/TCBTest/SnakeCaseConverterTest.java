package TCBTest;

import org.junit.jupiter.api.Test;
import scripting.transpiler.manifest.SnakeCaseConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SnakeCaseConverterTest {
    @Test
    public void convertsCamelCaseMembers() {
        assertEquals("move_and_slide", SnakeCaseConverter.toSnake("moveAndSlide"));
        assertEquals("linear_velocity", SnakeCaseConverter.toSnake("linearVelocity"));
        assertEquals("is_on_floor", SnakeCaseConverter.toSnake("isOnFloor"));
        assertEquals("position", SnakeCaseConverter.toSnake("position"));
    }

    @Test
    public void groupsAcronymRunsUntilLowercaseFollows() {
        assertEquals("parse_html5_element", SnakeCaseConverter.toSnake("parseHTML5Element"));
        assertEquals("html_parser", SnakeCaseConverter.toSnake("HTMLParser"));
    }

    @Test
    public void absorbsDigitsIntoTheCurrentToken() {
        assertEquals("vec2", SnakeCaseConverter.toSnake("vec2"));
        assertEquals("to_v2f", SnakeCaseConverter.toSnake("toV2f"));
        assertEquals("utf8_decode", SnakeCaseConverter.toSnake("utf8Decode"));
    }

    @Test
    public void handlesEmptyAndNull() {
        assertEquals("", SnakeCaseConverter.toSnake(""));
        assertNull(SnakeCaseConverter.toSnake(null));
    }
}
