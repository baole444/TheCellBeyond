package render.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenBounds;
import org.lwjgl.util.msdfgen.MSDFGenTransform;
import render.FontAtlasTexture;
import utility.AssetReference;
import utility.AssetsPool;
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

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.msdfgen.MSDFGen.*;
import static org.lwjgl.util.msdfgen.MSDFGenExt.*;

public class TCBFont {
    private static final int MSDF_SIZE = 32;
    private static final int TEXTURE_SIZE_MULTIPLIER = 2;
    private static final int bitmapSize = MSDF_SIZE * TEXTURE_SIZE_MULTIPLIER;
    private static final double TRANSLATION = 0.125d;
    private int colorChannelCount;
    private long ftFace, ftLib, msdfFTHandle, fontHandle;
    private final Map<Character, MSDFGlyphData> glyphData = new HashMap<>();
    private volatile ByteBuffer atlasData;
    private final ByteBuffer fontData;
    private boolean msdfReady = false;
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);

    private final AssetReference assetReference;
    private final GlyphRange glyphRange;
    private final int fontSizePixel;
    private int startChar;
    private int numGlyphs;
    private int atlasWidth, atlasHeight;
    private final Map<Character, CharInfo> characters = new HashMap<>();

    public TCBFont(ByteBuffer fontBuffer, AssetReference assetReference, int fontSizePixel, GlyphRange glyphRange) {
        this.assetReference = assetReference;
        this.fontSizePixel = fontSizePixel;
        this.glyphRange = glyphRange;
        fontData = fontBuffer;

        try {
            initializeMSDF();
            loadFontData();

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
     * @param fontSizePixel size to render the text at in pixel.
     * @param glyphRange The Unicode range to support.
     * @throws IOException File does not exist.
     */
    public TCBFont(String filepath, int fontSizePixel, GlyphRange glyphRange) throws IOException {
        PathResolver resolver;
        if (!PathResolver.isInitialized()) PathResolver.initialize(null);
        resolver = PathResolver.get();

        assetReference = new AssetReference(filepath);
        verifyFontFile();

        this.fontSizePixel = fontSizePixel;
        this.glyphRange = glyphRange;

        // Load font file
        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(data.length);
            fontBuffer.put(data);
            fontBuffer.flip();
            fontData = fontBuffer;

            try {
                initializeMSDF();
                loadFontData();

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

            check(msdf_ft_set_load_callback(name -> getLibrary().getFunctionAddress(memByteBuffer(name, memByteBufferNT1(name).capacity() + 1))));
            check(msdf_ft_init(pp));
            msdfFTHandle = pp.get(0);

            check(FT_Init_FreeType(pp));
            ftLib = pp.get(0);

            msdfReady = true;
        }
    }

    private void loadFontData() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            check(msdf_ft_load_font_data(msdfFTHandle, fontData, pp));
            fontHandle = pp.get(0);

            check(FT_New_Memory_Face(ftLib, fontData, 0 , pp));
            ftFace = pp.get(0);

            check(FT_Set_Pixel_Sizes(FT_Face.create(ftFace), 0, fontSizePixel));
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
                        BufferUtils.createByteBuffer(bitmapSize * bitmapSize * 3),
                        0.0f, 0.0d, 0.0d
                );
                glyphData.put(ch, data);
                return;
            }
            long shape = pp.get(0);

            FT_GlyphSlot slot = face.glyph();

            float ft_float_factor = 64.0f;
            float advance, bearingX, bearingY, height;
            advance = slot.advance().x() / ft_float_factor;
            bearingX = slot.metrics().horiBearingX() / ft_float_factor;
            bearingY = slot.metrics().horiBearingY() / ft_float_factor;
            height = slot.metrics().height() / ft_float_factor;

            check(msdf_shape_normalize(shape));
            check(msdf_shape_edge_colors_simple(shape, 3.0));

            MSDFGenBitmap bitmap = MSDFGenBitmap.calloc(stack);
            check(msdf_bitmap_alloc(MSDF_BITMAP_TYPE_MSDF, bitmapSize, bitmapSize, bitmap));

            MSDFGenBounds bounds = MSDFGenBounds.calloc(stack);
            check(msdf_shape_bound(shape, bounds));
            double left = bounds.l();
            double bottom = bounds.b();
            int marginPixel = 2 * TEXTURE_SIZE_MULTIPLIER;
            double margin = (double) marginPixel / bitmapSize;
            check(msdf_generate_msdf(bitmap, shape, MSDFGenTransform.calloc(stack)
                    .scale(it -> it
                            .x(MSDF_SIZE)
                            .y(MSDF_SIZE))
                    .translation(it -> it
                            .x(-(left - margin))
                            .y(-(bottom - margin)))
                    .distance_mapping(it -> it
                            .lower(-0.5 * TRANSLATION)
                            .upper(0.5 * TRANSLATION))
            ));
            ByteBuffer pixels = getBitmapU8(stack, bitmap);

            double leftOffset = (bearingX - marginPixel) / TEXTURE_SIZE_MULTIPLIER;
            double bottomOffset = (height - bearingY - marginPixel) / TEXTURE_SIZE_MULTIPLIER;

            MSDFGlyphData data = new MSDFGlyphData(
                    pixels, advance / TEXTURE_SIZE_MULTIPLIER,
                    leftOffset, bottomOffset
            );
            glyphData.put(ch, data);

            msdf_bitmap_free(bitmap);
            msdf_shape_free(shape);
        }
    }

    private void createAtlas() {
        if (glyphData.isEmpty()) return;
        int glyphPadding = 2;
        int paddedBitmapSize = bitmapSize + glyphPadding * 2;
        int glyphCount = glyphData.size();
        int glyphsPerRow = (int) Math.ceil(Math.sqrt(glyphCount));
        atlasWidth = glyphsPerRow * paddedBitmapSize;
        atlasHeight = ((glyphCount + glyphsPerRow - 1) / glyphsPerRow) * paddedBitmapSize;
        atlasData = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * colorChannelCount);

        int gIndex = 0;
        for (Map.Entry<Character, MSDFGlyphData> entry : glyphData.entrySet()) {
            char ch = entry.getKey();
            MSDFGlyphData data = entry.getValue();

            int row = gIndex / glyphsPerRow;
            int col = gIndex % glyphsPerRow;
            int X = col * paddedBitmapSize + glyphPadding;
            int Y = row * paddedBitmapSize + glyphPadding;

            copyGlyphToAtlas(data.pixelData(), X, Y);
            memFree(data.pixelData());

            float x0 = (float) X;
            float y0 = (float) Y;
            float x1 = x0 + bitmapSize;
            float y1 = y0 + bitmapSize;
            double xOffset = data.leftOffset();
            double yOffset = data.bottomOffset();

            CharInfo charInfo = new CharInfo(x0, y0, x1, y1,
                    xOffset, yOffset, data.advance(), fontSizePixel);
            characters.put(ch, charInfo);

            gIndex++;
        }

        glyphData.clear();
        FontManager.get().cacheFontAtlas(assetReference, glyphRange, atlasData);
    }

    private void copyGlyphToAtlas(ByteBuffer glyphData, int X, int Y) {
        for (int y = 0; y < bitmapSize; y++) {
            for (int x = 0; x < bitmapSize; x++) {
                int flipY = bitmapSize - 1 - y;
                int glyphIdx = (flipY * bitmapSize + x) * colorChannelCount;
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

    public int getFontSizePixel() {
        return fontSizePixel;
    }

    public int getTextureID() {
        if (assetReference == null || assetReference.getCanonicalPath() == null) return -1;

        FontAtlasTexture texture = AssetsPool.loadFontAtlasTexture(assetReference.getCanonicalPath(), glyphRange, atlasWidth, atlasHeight, colorChannelCount);
        return texture.getID();
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public ByteBuffer getFontData() {
        return fontData;
    }

    public CharInfo getCharInfo(char c) {
        return characters.getOrDefault(c, characters.get(' '));
    }

    public String getCanonicalPath() {
        return assetReference != null ? assetReference.getCanonicalPath() : null;
    }

    public GlyphRange getGlyphRange() {
        return glyphRange;
    }

    public boolean isLoaded() {
        return isLoaded.get();
    }
}
