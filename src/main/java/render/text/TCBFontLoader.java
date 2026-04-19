package render.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenBounds;
import org.lwjgl.util.msdfgen.MSDFGenTransform;
import utility.AssetReference;
import utility.log.EngineLog;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.msdfgen.MSDFGen.*;
import static org.lwjgl.util.msdfgen.MSDFGenExt.*;

class TCBFontLoader {
    private static final EngineLog Logger = new EngineLog(TCBFontLoader.class);
    private static final double Translation = 0.125d;
    record LoadResult(Map<Character, CharUV> charUVs, Map<Character, CharMetric> charMetrics, ByteBuffer atlasData) {}
    private long ftFace, ftLib, msdfFTHandle, fontHandle;
    private int colorChannelCount;
    private boolean msdfReady = false;
    private final Map<Character, MSDFGlyphData> glyphData = new HashMap<>();
    private ByteBuffer atlasData;

    static LoadResult generate(ByteBuffer fontData, GlyphRange glyphRange, int fontSizePixels) {
        TCBFontLoader loader = new TCBFontLoader();
        try {
            loader.initializeMSDF(fontData, fontSizePixels);
            if (!glyphRange.hasUnicodeRanges()) {
                int startChar = glyphRange.getStartChar();
                for (int codePoint = startChar; codePoint < startChar + glyphRange.getNumGlyphs(); codePoint++) {
                    char ch = (char) codePoint;
                    loader.generateGlyph(ch);
                }
                return loader.buildAtlas(fontSizePixels);
            }
            for (int[] range : glyphRange.getUnicodeRange()) {
                for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                    char ch = (char)codePoint;
                    loader.generateGlyph(ch);
                }
            }
            return loader.buildAtlas(fontSizePixels);
        } finally {
            loader.cleanup();
        }
    }

    static Map<Character, CharMetric> computeMetrics(ByteBuffer fontData, Map<Character, CharUV> charUVs, int fontSizePixels, AssetReference assetReference) {
        Map<Character, CharMetric> charMetrics = new HashMap<>();
        long ftLib =0, ftFace = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp =stack.mallocPointer(1);
            check(FT_Init_FreeType(pp));
            ftLib = pp.get(0);
            check(FT_New_Memory_Face(ftLib, fontData, 0, pp));
            ftFace = pp.get(0);
            check(FT_Set_Pixel_Sizes(FT_Face.create(ftFace), 0, fontSizePixels));
            FT_Face face = FT_Face.create(ftFace);
            for (char ch : charUVs.keySet()) {
                try {
                    int glyphIndex = FT_Get_Char_Index(face, ch);
                    FT_Load_Glyph(face, glyphIndex, FT_LOAD_DEFAULT);
                    FT_GlyphSlot slot = face.glyph();
                    if (slot == null) continue;
                    float ft_float_factor = 64.0f;
                    float advance = slot.advance().x() / ft_float_factor;
                    charMetrics.put(ch, new CharMetric(advance, fontSizePixels * FontAtlasLayout.TextureSizeMultiplier));
                } catch (Exception e) {
                    Logger.warning(String.format("Failed to compute size metric for char '%s': %s", ch, e.getMessage()));
                }
            }
        } catch (Exception e) {
            Logger.warning(String.format("Size metric computation failed for '%s': %s", assetReference.canonicalPath(), e.getMessage()));
        } finally {
            if (ftFace != 0) FT_Done_Face(FT_Face.create(ftFace));
            if (ftLib != 0) FT_Done_FreeType(ftLib);
        }
        return charMetrics;
    }

    private void initializeMSDF(ByteBuffer fontData, int fontSizePixels) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            check(msdf_ft_set_load_callback(name -> getLibrary().getFunctionAddress(memByteBuffer(name, memByteBufferNT1(name).capacity() + 1))));
            check(msdf_ft_init(pp));
            msdfFTHandle = pp.get(0);
            check(FT_Init_FreeType(pp));
            ftLib = pp.get(0);
            check(msdf_ft_load_font_data(msdfFTHandle, fontData, pp));
            fontHandle = pp.get(0);
            check(FT_New_Memory_Face(ftLib, fontData, 0 , pp));
            ftFace = pp.get(0);
            check(FT_Set_Pixel_Sizes(FT_Face.create(ftFace), 0, fontSizePixels));
            msdfReady = true;
        }
    }

    private void generateGlyph(char ch) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            FT_Face face = FT_Face.create(ftFace);
            int glyphIndex = FT_Get_Char_Index(face, ch);
            boolean glyphOk = FT_Load_Glyph(face, glyphIndex, FT_LOAD_DEFAULT) == 0;
            boolean shapeOK = msdf_ft_font_load_glyph(fontHandle, ch, MSDF_FONT_SCALING_EM_NORMALIZED, pp) == MSDF_SUCCESS;
            if (!shapeOK || !glyphOk) {
                glyphData.put(ch, new MSDFGlyphData(
                        BufferUtils.createByteBuffer(FontAtlasLayout.BitmapSize * FontAtlasLayout.BitmapSize * 3),
                        0.0f, 0.0d, 0.0d
                        )
                );
                return;
            }
            long shape = pp.get(0);
            FT_GlyphSlot slot = face.glyph();
            if (slot == null) {
                msdf_shape_free(shape);
                glyphData.put(ch,
                        new MSDFGlyphData(BufferUtils.createByteBuffer(FontAtlasLayout.BitmapSize * FontAtlasLayout.BitmapSize * 3),
                                0.0f, 0.0d, 0.0d));
                return;
            }
            float ft_float_factor = 64.0f;
            float advance = slot.advance().x() / ft_float_factor;
            check(msdf_shape_normalize(shape));
            check(msdf_shape_edge_colors_simple(shape, 3.0));
            MSDFGenBitmap bitmap = MSDFGenBitmap.calloc(stack);
            check(msdf_bitmap_alloc(MSDF_BITMAP_TYPE_MSDF, FontAtlasLayout.BitmapSize, FontAtlasLayout.BitmapSize, bitmap));
            MSDFGenBounds bounds = MSDFGenBounds.calloc(stack);
            check(msdf_shape_bound(shape, bounds));
            double left = bounds.l();
            int marginPixel = FontAtlasLayout.MSDFSize / 2;
            double margin = (double) marginPixel / FontAtlasLayout.MSDFSize;
            check(msdf_generate_msdf(bitmap, shape, MSDFGenTransform.calloc(stack)
                    .scale(it -> it.x(FontAtlasLayout.MSDFSize).y(FontAtlasLayout.MSDFSize))
                    .translation(it -> it.x(- left + margin).y(margin))
                    .distance_mapping(it -> it.lower(-0.5 * Translation).upper(0.5 * Translation))
            ));
            ByteBuffer pixels = buildBitmapU8(stack, bitmap);
            double leftOffset = left * FontAtlasLayout.MSDFSize + (double) marginPixel / 2;
            double bottomOffset = (double) marginPixel / 2;
            glyphData.put(ch, new MSDFGlyphData(pixels, advance, leftOffset, bottomOffset));
            msdf_bitmap_free(bitmap);
            msdf_shape_free(shape);
        }
    }

    private ByteBuffer buildBitmapU8(MemoryStack stack, MSDFGenBitmap bitmap) {
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
            float v = Math.max(0.0f, Math.min(1.0f, pixels.get(i)));
            // half-down rounding according to msdfgen
            data.put(i, (byte)(~(int)(255.5f -  255.0f * v)));
        }
        return data;
    }

    private LoadResult buildAtlas(int fontSizePixels) {
        if (glyphData.isEmpty()) return new LoadResult(Map.of(), Map.of(), BufferUtils.createByteBuffer(0));
        int count = glyphData.size();
        int glyphsPerRow = (int) Math.ceil(Math.sqrt(count));
        int atlasWidth = glyphsPerRow * FontAtlasLayout.PaddedBitmapSize;
        int atlasHeight = ((count + glyphsPerRow - 1) / glyphsPerRow) * FontAtlasLayout.PaddedBitmapSize;
        atlasData = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * colorChannelCount);
        Map<Character, CharUV> charUVs = new HashMap<>();
        Map<Character, CharMetric> charMetrics = new HashMap<>();
        int gIndex = 0;
        for (Map.Entry<Character, MSDFGlyphData> entry : glyphData.entrySet()) {
            char ch = entry.getKey();
            MSDFGlyphData data = entry.getValue();
            int row = gIndex / glyphsPerRow;
            int col = gIndex % glyphsPerRow;
            int X = col * FontAtlasLayout.PaddedBitmapSize + FontAtlasLayout.GlyphPadding;
            int Y = row * FontAtlasLayout.PaddedBitmapSize + FontAtlasLayout.GlyphPadding;
            copyGlyphToAtlas(data.pixelData(), X, Y, atlasWidth);
            memFree(data.pixelData());
            charUVs.put(ch, new CharUV(X, Y, X + FontAtlasLayout.BitmapSize, Y + FontAtlasLayout.BitmapSize, data.leftOffset(), data.bottomOffset()));
            charMetrics.put(ch, new CharMetric(data.advance(), fontSizePixels * FontAtlasLayout.TextureSizeMultiplier));
            gIndex++;
        }
        glyphData.clear();
        return new LoadResult(charUVs, charMetrics, atlasData);
    }

    private void copyGlyphToAtlas(ByteBuffer glyphData, int X, int Y, int atlasWidth) {
        for (int y = 0; y < FontAtlasLayout.BitmapSize; y++) {
            for (int x = 0; x < FontAtlasLayout.BitmapSize; x++) {
                int flipY = FontAtlasLayout.BitmapSize - 1 - y;
                int glyphIdx = (flipY * FontAtlasLayout.BitmapSize + x) * colorChannelCount;
                int atlasIdx = ((Y + y) * atlasWidth + (X + x)) * colorChannelCount;
                if (atlasIdx + 2 >= atlasData.capacity() || glyphIdx + 2 >= glyphData.capacity()) continue;
                atlasData.put(atlasIdx, glyphData.get(glyphIdx));
                atlasData.put(atlasIdx + 1, glyphData.get(glyphIdx + 1));
                atlasData.put(atlasIdx + 2, glyphData.get(glyphIdx + 2));
            }
        }
    }

    private void cleanup() {
        if (!msdfReady) return;
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

    private static void check(int result) {
        if (result != MSDF_SUCCESS) throw new IllegalStateException("MSDF failed with error code: " + result);
    }
}
