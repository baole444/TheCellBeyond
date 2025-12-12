package TCBTest;

import org.junit.jupiter.api.Test;
import render.text.GlyphRange;
import render.text.TCBFont;
import utility.Settings;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TCBFontTest {
    @Test
    public void loadFontCorrectly() {
        String correctPath = Settings.FontPath.Caudex;
        TCBFont font = null;
        try {
            font = new TCBFont(correctPath, 16, GlyphRange.ASCII);
        } catch (IOException _) {}

        assertNotNull(font);
    }

    @Test
    public void loadNoneExistingFontThrowIOException() {
        try {
            String incorrectPath = "engine://assets/fonts/NoneExistenceFile.ttf";
            TCBFont font = new TCBFont(incorrectPath, 16, GlyphRange.ASCII);
        } catch (IOException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void loadFontWithGlyphASCII_EXTENDED() {
        String correctPath = Settings.FontPath.NotoSansMono;
        TCBFont font = null;
        try {
            font = new TCBFont(correctPath, 16, GlyphRange.ASCII_EXTENDED);
        } catch (IOException _) {}

        assertNotNull(font);
    }
}
