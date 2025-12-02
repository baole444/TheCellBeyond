package render.fontRenderer;

import org.junit.jupiter.api.Test;
import render.text.GlyphRange;
import render.text.TCBFont;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FontTest {

    @Test
    public void runSuccess() throws IOException {
        String correctPath = "assets/fonts/Caudex.ttf";
        TCBFont font = new TCBFont(correctPath, 16, GlyphRange.ASCII);

        assertNotNull(font);
    }

    @Test
    public void runFail() {
        try {
            String incorrectPath = "assets/fonts/Consolas.ttf";
            TCBFont font = new TCBFont(incorrectPath, 16, GlyphRange.ASCII);
        } catch (IOException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void runSuccessASCII_EXTENDED() throws IOException {
        String correctPath = "assets/fonts/NotoSansMono_Regular.ttf";
        TCBFont font = new TCBFont(correctPath, 16, GlyphRange.ASCII_EXTENDED);

        assertNotNull(font);
    }
}
