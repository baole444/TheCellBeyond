package render.text;

public enum GlyphRange {
    /**
     * ASCII printable chars.
     */
    ASCII(32, 96, "ASCII"),

    /**
     * ASCII and commonly used symbols.
     */
    ASCII_EXTENDED(32, 256, "ASCII and Symbols"),

    /**
     * Japanese Hiragana and Katakana.
     */
    JAPANESE_BASIC(0x3400, 192, "Japanese Basic"),

    /**
     * Extended ASCII including basic Latin accented chars.
     */
    LATIN_EXTENDED(32, 244, "Latin Extended"),

    /**
     * Latin and Cyrillic chars.
     */
    LATIN_CYRILLIC(32, 400, "Latin and Cyrillic"),

    /**
     * Latin and Greek chars.
     */
    LATIN_GREEK(32, 352, "Latin and Greek"),

    /**
     * Vietnamese chars.
     */
    VIETNAMESE(new int[][] {
            {0x0020, 0x007F},  // Basic Latin
            {0x00A0, 0x00FF},  // Latin-1 Supplement
            {0x0100, 0x017F},  // Latin Extended-A
            {0x0180, 0x024F},  // Latin Extended-B
            {0x0300, 0x036F},  // Combining Diacritical Marks
            {0x1EA0, 0x1EFF}   // Latin Extended Additional
    }, "Vietnamese"),

    /**
     * This will attempt to dynamically calculate and get all glyphs supported by the font.
     */
    ALL(0, 0, "All Glyphs");

    private final int startChar;

    private final int numGlyphs;

    private final String description;

    private final int [][] unicodeRange;

    GlyphRange(int startChar, int numGlyphs, String description) {
        this.startChar = startChar;
        this.numGlyphs = numGlyphs;
        this.description = description;
        this.unicodeRange = null;
    }

    GlyphRange(int[][] unicodeRange, String description) {
        this.startChar = -1;
        this.numGlyphs = -1;
        this.description = description;
        this.unicodeRange = unicodeRange;
    }

    public int getStartChar() {
        return startChar;
    }

    public int getNumGlyphs() {
        return numGlyphs;
    }

    public String getDescription() {
        return description;
    }

    public int[][] getUnicodeRange() {
        return unicodeRange;
    }

    public boolean hasUnicodeRanges() {
        return unicodeRange != null;
    }
}
