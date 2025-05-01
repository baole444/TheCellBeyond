package render.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import utility.PathResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBTruetype.*;

public class TCBFont {
    private final String filepath;
    private final int fontSize;
    private final GlyphRange glyphRange;
    private int startChar;
    private int numGlyphs;

    private int textureId;
    private int bitmapWidth;
    private int bitmapHeight;

    private final Map<Character, CharInfo> characters = new HashMap<>();

    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final AtomicBoolean hasTexture = new AtomicBoolean(false);

    private volatile ByteBuffer bitmap;

    public TCBFont(ByteBuffer fontBuffer, String filepath, int fontSize, GlyphRange glyphRange) throws IOException {
        this.filepath = filepath;
        this.fontSize = fontSize;
        this.glyphRange = glyphRange;

        if (glyphRange.hasUnicodeRanges()) {
            loadCombinedRangeFont(fontBuffer);
        } else if (glyphRange == GlyphRange.ALL) {
            this.startChar = 0;
            this.numGlyphs = getFontAvailableGlyphs(fontBuffer);
            loadSingleRangeFont(fontBuffer);
        } else {
            this.startChar = glyphRange.getStartChar();
            this.numGlyphs = glyphRange.getNumGlyphs();
            loadSingleRangeFont(fontBuffer);
        }

        isLoaded.set(true);
    }

    /**
     * Create a font with ASCII glyph range.
     * @param filepath path to the font file.
     * @param fontSize size to render the text at.
     * @param isProjectAsset is the file path relative to the project's root directory?
     * @throws IOException File does not exist.
     */
    public TCBFont(String filepath, int fontSize, boolean isProjectAsset) throws IOException {
        this(filepath, fontSize, isProjectAsset, GlyphRange.ASCII);
    }

    /**
     * Create a font.
     * @param filepath path to the font file.
     * @param fontSize size to render the text at.
     * @param isProjectAsset is the file path relative to the project's root directory?
     * @param glyphRange The Unicode range to support.
     * @throws IOException File does not exist.
     */
    public TCBFont(String filepath, int fontSize, boolean isProjectAsset, GlyphRange glyphRange) throws IOException {
        if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
            this.filepath = PathResolver.resolveToAbsolute(ProjectRoot, filepath);
        }
        // Assumed that it is a default font that we have in our assets. Or user wants to use an absolute path.
        else this.filepath = new File(filepath).getAbsolutePath();

        verifyFontFile();

        this.fontSize = fontSize;
        this.glyphRange = glyphRange;

        // Load font file
        byte[] fontData = Files.readAllBytes(Paths.get(filepath));
        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
        fontBuffer.put(fontData);
        fontBuffer.flip();

        if (glyphRange.hasUnicodeRanges()) {
            loadCombinedRangeFont(fontBuffer);
        } else if (glyphRange == GlyphRange.ALL) {
            this.startChar = 0;
            this.numGlyphs = getFontAvailableGlyphs(fontBuffer);
            loadSingleRangeFont(fontBuffer);
        } else {
            this.startChar = glyphRange.getStartChar();
            this.numGlyphs = glyphRange.getNumGlyphs();
            loadSingleRangeFont(fontBuffer);
        }

        isLoaded.set(true);

        createTexture();
    }

    private void verifyFontFile() throws IOException {
        File toVerify = new File(this.filepath);

        if (!toVerify.exists()) throw new IOException("Font file does not exist at: '" + this.filepath + "'");
    }

    private int calculateBitmapScale(int fontSize, int numGlyphs) {
        int defaultSize = 512;

        // Apply generous padding to the glyphs;
        float additionalPadding = 1.1f;

        float glyphBoundBox = fontSize * additionalPadding;

        int glyphsPerRow = (int) (defaultSize / glyphBoundBox);

        int rows = (int) Math.ceil((float) numGlyphs / glyphsPerRow);

        float requiredHeight = rows * glyphBoundBox;

        float scaleFactor = requiredHeight / defaultSize;

        if (scaleFactor <= 1.0f) {
            return 1;
        }

        return (int) Math.ceil(scaleFactor);
    }

    private int getFontAvailableGlyphs(ByteBuffer fontData) {
        // create temporary font info
        STBTTFontinfo fontInfo = STBTTFontinfo.calloc();
        if (!stbtt_InitFont(fontInfo, fontData)) {
            fontInfo.free();
            return 1024; // return a generous number of glyphs when font init failed.
        }

        int glyphCount = 0;

        try {
            int[][] unicodeRanges = {
                    {0x0020, 0x007F},   // Basic Latin
                    {0x00A0, 0x00FF},   // Latin-1 Supplement
                    {0x0100, 0x017F},   // Latin Extended-A
                    {0x0180, 0x024F},   // Latin Extended-B
                    {0x0250, 0x02AF},   // IPA Extensions
                    {0x02B0, 0x02FF},   // Spacing Modifier Letters
                    {0x0300, 0x036F},   // Combining Diacritical Marks
                    {0x0370, 0x03FF},   // Greek and Coptic
                    {0x0400, 0x04FF},   // Cyrillic
                    {0x0500, 0x052F},   // Cyrillic Supplement
                    {0x0530, 0x058F},   // Armenian
                    {0x0590, 0x05FF},   // Hebrew
                    {0x0600, 0x06FF},   // Arabic
                    {0x0900, 0x097F},   // Devanagari
                    {0x0980, 0x09FF},   // Bengali
                    {0x0A00, 0x0A7F},   // Gurmukhi
                    {0x0E00, 0x0E7F},   // Thai
                    {0x1100, 0x11FF},   // Hangul Jamo
                    {0x2000, 0x206F},   // General Punctuation
                    {0x2070, 0x209F},   // Superscripts and Subscripts
                    {0x20A0, 0x20CF},   // Currency Symbols
                    {0x2100, 0x214F},   // Letterlike Symbols
                    {0x2150, 0x218F},   // Number Forms
                    {0x2200, 0x22FF},   // Mathematical Operators
                    {0x2600, 0x26FF},   // Miscellaneous Symbols
                    {0x3000, 0x303F},   // CJK Symbols and Punctuation
                    {0x3040, 0x309F},   // Hiragana
                    {0x30A0, 0x30FF},   // Katakana
                    {0x3100, 0x312F},   // Bopomofo
                    {0x4E00, 0x9FFF},   // CJK Unified Ideographs (sample only 1/16 for performance)
                    {0xAC00, 0xD7AF}    // Hangul Syllables (sample only 1/16 for performance)
            };

            // Sampling Unicode points in the font
            for (int[] range: unicodeRanges) {
                int start = range[0];
                int end = range[1];

                int step = 1;

                // Sample 1/16 on large ranges
                if (end - start > 1000) {
                    step = 16;
                }

                for (int codePoint = start; codePoint <= end; codePoint += step) {
                    int glyphIndex = stbtt_FindGlyphIndex(fontInfo, codePoint);
                    if (glyphIndex > 0) {
                        glyphCount += step;
                    }
                }
            }

            // Find highest valid glyph index
            int maxGlyphIndex = findMaxGlyphIndex(fontInfo);

            // Check if count match max glyph index
            glyphCount = Math.max(glyphCount, maxGlyphIndex);

            // Min of 256 glyphs
            glyphCount = Math.max(glyphCount, 256);

            // Cap max glyph count in case it is excessively large.
            glyphCount = Math.min(glyphCount, 10000);

        } finally {
            fontInfo.free();
        }

        return glyphCount;
    }

    private int findMaxGlyphIndex(STBTTFontinfo fontInfo) {
        IntBuffer advanceWidth = BufferUtils.createIntBuffer(1);
        IntBuffer leftSideBearing = BufferUtils.createIntBuffer(1);
        IntBuffer x0 = BufferUtils.createIntBuffer(1);
        IntBuffer y0 = BufferUtils.createIntBuffer(1);
        IntBuffer x1 = BufferUtils.createIntBuffer(1);
        IntBuffer y1 = BufferUtils.createIntBuffer(1);

        int maxGlyphIndex = 0;
        int low = 0;
        int high = 65535;

        while (low <= high) {
            int mid = (low + high) /2;

            int glyphIndex = mid;

            stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, leftSideBearing);
            boolean validBox = stbtt_GetGlyphBox(fontInfo, glyphIndex, x0, y0, x1, y1);

            boolean isGlyphValid = advanceWidth.get(0) != 0 || validBox;

            if (isGlyphValid) {
                maxGlyphIndex = Math.max(maxGlyphIndex, glyphIndex);
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return maxGlyphIndex;
    }

    private void loadSingleRangeFont(ByteBuffer fontBuffer) {
        int scaleFactor = calculateBitmapScale(fontSize, numGlyphs);

        // Create bitmap
        bitmapWidth = 512 * scaleFactor;
        bitmapHeight = 512 * scaleFactor;
        bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

        // Create buffer for char data
        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(numGlyphs);

        // Bake the font to bitmap
        stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap, bitmapWidth, bitmapHeight, startChar, charData);


        // Save char info of each glyph
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < numGlyphs; i++) {
                STBTTBakedChar bakedChar = charData.get(i);
                CharInfo charInfo = new CharInfo(bakedChar.x0(), bakedChar.y0(),
                        bakedChar.x1(), bakedChar.y1(),
                        bakedChar.xoff(), bakedChar.yoff(),
                        bakedChar.xadvance()
                );

                characters.put((char)(startChar + i), charInfo);
            }
        }

        // Free char data
        charData.free();
    }

    private void loadCombinedRangeFont(ByteBuffer fontBuffer) {
        int totalGlyphs = 0;

        // Count total glyphs across all ranges
        for (int[] range : glyphRange.getUnicodeRange()) {
            totalGlyphs += (range[1] - range[0] + 1);
        }

        int scaleFactor = calculateBitmapScale(fontSize, totalGlyphs);

        bitmapWidth = 512 * scaleFactor;
        bitmapHeight = 512 * scaleFactor;
        bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

        // Prepare font for packing
        STBTTFontinfo fontInfo = STBTTFontinfo.calloc();
        stbtt_InitFont(fontInfo, fontBuffer);

        // Create packing context
        STBTTPackContext packContext = STBTTPackContext.malloc();
        stbtt_PackBegin(packContext, bitmap, bitmapWidth, bitmapHeight, 0 ,1);

        // Process unicode ranges
        for (int[] range : glyphRange.getUnicodeRange()) {
            int firstChar = range[0];
            int charCount = range[1] - range[0] + 1;

            // Create buffer of pack ranges and buffer for packed char data
            STBTTPackRange.Buffer rangeBuffer = STBTTPackRange.calloc(1);
            STBTTPackedchar.Buffer charBuffer = STBTTPackedchar.calloc(charCount);

            // Configure pack range
            rangeBuffer.get(0)
                .font_size(fontSize)
                .first_unicode_codepoint_in_range(firstChar)
                .num_chars(charCount)
                .chardata_for_range(charBuffer);

            // Pack this range of chars
            stbtt_PackFontRanges(packContext, fontBuffer, 0 , rangeBuffer);

            // Store char info
            for (int i = 0; i < charCount; i++) {
                STBTTPackedchar packedChar = charBuffer.get(i);

                CharInfo charInfo = new CharInfo(
                        packedChar.x0(), packedChar.y0(),
                        packedChar.x1(), packedChar.y1(),
                        packedChar.xoff(), packedChar.yoff(),
                        packedChar.xadvance()
                );

                characters.put((char) (firstChar + i), charInfo);
            }

            // Free resources, charBuffer is resued.
            rangeBuffer.free();
        }

        stbtt_PackEnd(packContext);
        packContext.free();
        fontInfo.free();
    }

    public void createTexture() {
        // Bitmap generated or has no bitmap to generate
        if (hasTexture.get() || bitmap == null) return;

        if (textureId != 0) glDeleteTextures(textureId);

        // Gen OpenGL texture.
        textureId = glGenTextures();

        System.out.println("Creating new font texture: " + textureId + " for font: " + filepath);

        IntBuffer previousTexture = BufferUtils.createIntBuffer(1);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture);

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, previousTexture.get(0));

        hasTexture.set(true);

        bitmap = null;
    }

    private void saveDebugImage(ByteBuffer bitmap) {
        try {
            // Create a BufferedImage and write the bitmap data to it
            BufferedImage image = new BufferedImage(bitmapWidth, bitmapHeight, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < bitmapHeight; y++) {
                for (int x = 0; x < bitmapWidth; x++) {
                    int i = y * bitmapWidth + x;
                    int value = bitmap.get(i) & 0xFF;
                    int color = (value << 24) | (value << 16) | (value << 8) | value;
                    image.setRGB(x, y, color);
                }
            }

            // Save the image to a file
            String fontName = new File(filepath).getName().replaceAll("\\.[^.]*$", "");
            File outputFile = new File("tempFont_" + fontName + ".png");
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            System.err.println("Failed to save debug image: " + e.getMessage());
        }
    }

    public int getFontSize() {
        return fontSize;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getBitmapWidth() {
        return bitmapWidth;
    }

    public int getBitmapHeight() {
        return bitmapHeight;
    }

    public CharInfo getCharInfo(char c) {
        return characters.getOrDefault(c, characters.get(' '));
    }

    public String getFilepath() {
        return filepath;
    }

    public GlyphRange getGlyphRange() {
        return glyphRange;
    }

    public boolean isLoaded() {
        return isLoaded.get();
    }

    public boolean waitingTexture() {
        return isLoaded.get() && !hasTexture.get();
    }

    public void cleanup() {
        if (hasTexture.get() && textureId != 0) glDeleteTextures(textureId);

        bitmap = null;
    }
}
