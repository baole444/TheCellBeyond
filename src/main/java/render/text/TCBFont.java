package render.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import utility.AssetReference;
import utility.PathResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBTruetype.*;

public class TCBFont {
    private AssetReference assetReference;
    private final int fontSize;
    private final GlyphRange glyphRange;
    private int startChar;
    private int numGlyphs;

    private transient int textureId;
    private int bitmapWidth;
    private int bitmapHeight;

    private final Map<Character, CharInfo> characters = new HashMap<>();

    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final AtomicBoolean hasTexture = new AtomicBoolean(false);

    private volatile ByteBuffer bitmap;

    public TCBFont(ByteBuffer fontBuffer, String filepath, int fontSize, GlyphRange glyphRange) throws IOException {
        assetReference = new AssetReference(filepath);
        this.fontSize = fontSize;
        this.glyphRange = glyphRange;

        if (glyphRange.hasUnicodeRanges()) {
            loadCombinedRangeFont(fontBuffer);
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
        PathResolver resolver;

        if (!PathResolver.isInitialized()) {
            PathResolver.initialize(null);
        }
        resolver = PathResolver.get();

        assetReference = new AssetReference(filepath);

        verifyFontFile();

        this.fontSize = fontSize;
        this.glyphRange = glyphRange;

        // Load font file
        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] fontData = stream.readAllBytes();
            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
            fontBuffer.put(fontData);
            fontBuffer.flip();

            if (glyphRange.hasUnicodeRanges()) {
                loadCombinedRangeFont(fontBuffer);
            } else {
                this.startChar = glyphRange.getStartChar();
                this.numGlyphs = glyphRange.getNumGlyphs();
                loadSingleRangeFont(fontBuffer);
            }

            isLoaded.set(true);
        }
    }

    private void verifyFontFile() throws IOException {
        File toVerify = new File(assetReference.getAbsolutePath());

        if (!toVerify.exists()) throw new IOException("Font file does not exist at: '" + assetReference.getCanonicalPath() + "'");
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

        int newTextureId = 0;

        try {
            newTextureId = glGenTextures();

            IntBuffer previousTexture = BufferUtils.createIntBuffer(1);

            glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture);

            glBindTexture(GL_TEXTURE_2D, newTextureId);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, previousTexture.get(0));

            if (textureId != 0) glDeleteTextures(textureId);

            textureId = newTextureId;
            hasTexture.set(true);

            bitmap = null;
        } catch (Exception e) {
            if (newTextureId != 0) {
                glDeleteTextures(newTextureId);
            }

            throw e;
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
        return assetReference != null ? assetReference.getCanonicalPath() : null;
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
        if (hasTexture.get() && textureId != 0) {
            glDeleteTextures(textureId);

            textureId = 0;
        }

        bitmap = null;
    }
}
