package render.text;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenTransform;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImageWrite.stbi_flip_vertically_on_write;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.msdfgen.MSDFGen.*;
import static org.lwjgl.util.msdfgen.MSDFGenExt.*;

/**
 * <a href="https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/util/msdfgen/HelloMSDFGen.java">Will be replaced with lwjgl msdfgen binding</a>
 */
public class TCBFont {
    private static final int MSDF_SIZE = 32;
    private static final double TRANSLATION = 0.125d;
    private int colorChannelCount;
    private long ftFace;
    private long ftLib;
    private long msdfFTHandle;
    private long fontHandle;
    private final Map<Character, MSDFGlyphData> glyphData = new HashMap<>();
    private boolean msdfReady = false;

    private final AssetReference assetReference;
    private final int fontSize;
    private final GlyphRange glyphRange;
    private int startChar;
    private int numGlyphs;

    private transient int textureId;
    private int atlasWidth;
    private int atlasHeight;

    private final Map<Character, CharInfo> characters = new HashMap<>();

    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final AtomicBoolean hasTexture = new AtomicBoolean(false);

    private volatile ByteBuffer atlasData;

    public TCBFont(ByteBuffer fontBuffer, AssetReference assetReference, int fontSize, GlyphRange glyphRange) {
        this.assetReference = assetReference;
        this.fontSize = fontSize;
        this.glyphRange = glyphRange;

        try {
            initializeMSDF();
            loadFontData(fontBuffer);

            if (glyphRange.hasUnicodeRanges()) {
                generateCombinedRangeAtlas();
            } else {
                startChar = glyphRange.getStartChar();
                numGlyphs = glyphRange.getNumGlyphs();
                generateSingleRangeAtlas();
            }

            isLoaded.set(true);
        } finally {
            cleanupMSDF();
        }
    }

    /**
     * Create a font.
     * @param filepath path to the font file.
     * @param fontSize size to render the text at.
     * @param glyphRange The Unicode range to support.
     * @throws IOException File does not exist.
     */
    public TCBFont(String filepath, int fontSize, GlyphRange glyphRange) throws IOException {
        PathResolver resolver;
        if (!PathResolver.isInitialized()) PathResolver.initialize(null);
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

            try {
                initializeMSDF();
                loadFontData(fontBuffer);

                if (glyphRange.hasUnicodeRanges()) {
                    generateCombinedRangeAtlas();
                } else {
                    startChar = glyphRange.getStartChar();
                    numGlyphs = glyphRange.getNumGlyphs();
                    generateSingleRangeAtlas();
                }

                isLoaded.set(true);
            } finally {
                cleanupMSDF();
            }
        }
    }

    private void verifyFontFile() throws IOException {
        PathResolver resolver = PathResolver.get();

        if (!resolver.exists(assetReference.getResolvedPath())) throw new IOException("Font file does not exist at: '" + assetReference.getCanonicalPath() + "'");
    }

    private void generateSingleRangeAtlas() {
        for (int i = 0; i < numGlyphs; i++) {
            char ch = (char)(startChar + i);
            generateGlyph(ch);
        }

        createAtlas();
    }

    private void generateCombinedRangeAtlas() {
        for (int[] range : glyphRange.getUnicodeRange()) {
            for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                char ch = (char)codePoint;
                generateGlyph(ch);
            }
        }

        createAtlas();
    }

    private void initializeMSDF() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);

            check(msdf_ft_set_load_callback(name -> FreeType.getLibrary().getFunctionAddress(memByteBuffer(name, memByteBufferNT1(name).capacity() + 1))));
            check(msdf_ft_init(pp));
            msdfFTHandle = pp.get(0);

            check(FT_Init_FreeType(pp));
            ftLib = pp.get(0);

            msdfReady = true;
        }
    }

    private void loadFontData(ByteBuffer fontData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            check(msdf_ft_load_font_data(msdfFTHandle, fontData, pp));
            fontHandle = pp.get(0);

            check(FT_New_Memory_Face(ftLib, fontData, 0 , pp));
            ftFace = pp.get(0);

            check(FT_Set_Pixel_Sizes(FT_Face.create(ftFace), 0, fontSize));
        }
    }

    private void cleanupMSDF() {
        if (msdfReady) {
            if (fontHandle != 0) {
                msdf_ft_font_destroy(fontHandle);
                fontHandle = 0;
            }
            if (msdfFTHandle != 0) {
                msdf_ft_deinit(msdfFTHandle);
                msdfFTHandle = 0;
            }
            if (ftFace != 0) {
                FT_Done_Face(FT_Face.create(ftFace));
                ftFace = 0;
            }
            if (ftLib != 0) {
                FT_Done_FreeType(ftLib);
                ftLib = 0;
            }
            Objects.requireNonNull(msdf_ft_get_load_callback()).free();
            msdfReady = false;
        }
    }

    private ByteBuffer getBitmapU8(MemoryStack stack, MSDFGenBitmap bitmap) {
        PointerBuffer pp = stack.mallocPointer(1);

        check(msdf_bitmap_get_byte_size(bitmap, pp));
        long byteSize = pp.get(0);

        check(msdf_bitmap_get_pixels(bitmap, pp));
        FloatBuffer pixels = memFloatBuffer(pp.get(0), (int)byteSize >> 2);

        IntBuffer pi = stack.mallocInt(1);
        msdf_bitmap_get_channel_count(bitmap, pi);
        colorChannelCount = pi.get(0);

        ByteBuffer data = memAlloc(pixels.capacity());
        for (int i = 0; i < pixels.capacity(); i++) {
            // clamp to [0, 1] range
            float v = Math.max(0.0f, Math.min(1.0f, pixels.get(i)));
            // half-down rounding according to msdfgen
            data.put(i, (byte)(~(int)(255.5f -  255.0f * v)));
        }

        return data;
    }

    private void generateGlyph(char ch) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);

            FT_Face face = FT_Face.create(ftFace);

            int glyphIndex = FT_Get_Char_Index(face, ch);
            boolean glyphOk = FT_Load_Glyph(face, glyphIndex, FT_LOAD_DEFAULT) == 0;
            boolean shapeOK = msdf_ft_font_load_glyph(fontHandle, ch, MSDF_FONT_SCALING_EM_NORMALIZED, pp) == MSDF_SUCCESS;
            if (!shapeOK || !glyphOk) {
                MSDFGlyphData data = new MSDFGlyphData(
                        BufferUtils.createByteBuffer(MSDF_SIZE * MSDF_SIZE  * 3),
                        0.0f, new Vector2f(), new Vector2f()
                );
                glyphData.put(ch, data);
                return;
            }
            long shape = pp.get(0);

            check(msdf_shape_normalize(shape));
            check(msdf_shape_edge_colors_simple(shape, 3.0));

            MSDFGenBitmap bitmap = MSDFGenBitmap.calloc(stack);
            check(msdf_bitmap_alloc(MSDF_BITMAP_TYPE_MSDF, MSDF_SIZE, MSDF_SIZE, bitmap));

            check(msdf_generate_msdf(bitmap, shape, MSDFGenTransform.calloc(stack)
                    .scale(it -> it
                            .x(MSDF_SIZE)
                            .y(MSDF_SIZE))
                    .translation(it -> it
                            .x(TRANSLATION)
                            .y(TRANSLATION))
                    .distance_mapping(it -> it
                            .lower(-0.5 * TRANSLATION)
                            .upper(0.5 * TRANSLATION))
            ));

            MSDFGenBitmap output = bitmap;

            ByteBuffer pixels = getBitmapU8(stack, output);

            FT_GlyphSlot slot = face.glyph();
            float advance = 0.0f;
            float bearingX = 0.0f;
            float bearingY = MSDF_SIZE * 0.75f;
            float width =  MSDF_SIZE;
            float height = MSDF_SIZE;
            if (slot != null) {
                advance = slot.advance().x() >> 6;
                bearingX = slot.bitmap_left();
                bearingY = slot.bitmap_top();
                width = slot.bitmap().width();
                height = slot.bitmap().rows();
            }

            MSDFGlyphData data = new MSDFGlyphData(
                    pixels, advance,
                    new Vector2f(bearingX, bearingY),
                    new Vector2f(width, height)
            );
            
            glyphData.put(ch, data);

            msdf_bitmap_free(bitmap);
            msdf_shape_free(shape);
        }
    }

    private void createAtlas() {
        if (glyphData.isEmpty()) return;

        int glyphCount = glyphData.size();

        int glyphsPerRow = (int) Math.ceil(Math.sqrt(glyphCount));
        atlasWidth = glyphsPerRow * MSDF_SIZE;
        atlasHeight = ((glyphCount + glyphsPerRow - 1) / glyphsPerRow) * MSDF_SIZE;
        atlasData = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * colorChannelCount);

        int gIndex = 0;
        for (Map.Entry<Character, MSDFGlyphData> entry : glyphData.entrySet()) {
            char ch = entry.getKey();
            MSDFGlyphData data = entry.getValue();

            int row = gIndex / glyphsPerRow;
            int col = gIndex % glyphsPerRow;
            int X = col * MSDF_SIZE;
            int Y = row * MSDF_SIZE;

            copyGlyphToAtlas(data.pixelData(), X, Y);
            memFree(data.pixelData());

            float x0 = (float) X;
            float y0 = (float) Y;
            float x1 = x0 + MSDF_SIZE;
            float y1 = y0 + MSDF_SIZE;
            float xOffset = data.bearing().x;
            float yOffset = data.bearing().y;
            Vector2f size = data.size();

            CharInfo charInfo = new CharInfo(x0, y0, x1, y1,
                    xOffset, yOffset, data.advance(),
                    size.x, size.y);
            characters.put(ch, charInfo);

            gIndex++;
        }

        glyphData.clear();
    }

    private void copyGlyphToAtlas(ByteBuffer glyphData, int X, int Y) {
        for (int y = 0; y < MSDF_SIZE; y++) {
            for (int x = 0; x < MSDF_SIZE; x++) {
                int glyphIdx = (y * MSDF_SIZE + x) * colorChannelCount;
                int atlasIdx = ((Y + y) * atlasWidth + (X + x)) * colorChannelCount;
                if (atlasIdx + 2 >= atlasData.capacity() || glyphIdx + 2 >= glyphData.capacity()) continue;

                atlasData.put(atlasIdx, glyphData.get(glyphIdx));
                atlasData.put(atlasIdx + 1, glyphData.get(glyphIdx + 1));
                atlasData.put(atlasIdx + 2, glyphData.get(glyphIdx + 2));
            }
        }
    }

    private void check(int result) {
        if (result != MSDF_SUCCESS) {
            throw new IllegalStateException("MSDF failed with error code: " + result);
        }
    }

    public void createTexture() {
        if (hasTexture.get() || atlasData == null) return;

        int newTextureId = 0;

        try {
            newTextureId = glGenTextures();

            IntBuffer previousTexture = BufferUtils.createIntBuffer(1);

            glGetIntegerv(GL_TEXTURE_BINDING_2D, previousTexture);
            glBindTexture(GL_TEXTURE_2D, newTextureId);

            int format = colorChannelCount == 3 ? GL_RGB : GL_RED;
            glTexImage2D(GL_TEXTURE_2D, 0, format, atlasWidth, atlasHeight, 0, format, GL_UNSIGNED_BYTE, atlasData);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, previousTexture.get(0));

            if (textureId != 0) glDeleteTextures(textureId);

            textureId = newTextureId;
            hasTexture.set(true);

            atlasData = null;
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

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
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

        atlasData = null;
    }
}
