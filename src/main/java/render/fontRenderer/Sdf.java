package render.fontRenderer;

import com.mlomb.freetypejni.Face;
import com.mlomb.freetypejni.FreeType;
import com.mlomb.freetypejni.Library;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.mlomb.freetypejni.FreeType.*;
import static com.mlomb.freetypejni.FreeTypeConstants.FT_LOAD_RENDER;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

public class Sdf {
    public static int textureId = -1;

    private static float mapRange(float val, float in_min, float in_max, float out_min, float out_max) {
        return (val - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private static int getPixel(int x, int y, byte[] bitmap, int width, int height) {
        if (x >= 0 && x < width && y > 0 && y < height) {
            return (bitmap[x + y * width] & 0xFF) == 0 ? 0 : 1;
        }

        return 0;
    }

    private static float findNearestPixel(int pixelX, int pixelY, byte[] bitmap, int width, int height, int spread) {
        int state = getPixel(pixelX, pixelY, bitmap, width, height);
        int minX = pixelX - spread;
        int maxX = pixelX + spread;
        int minY = pixelY - spread;
        int maxY = pixelY + spread;

        float minDistance = spread * spread;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                int pixelState = getPixel(x, y, bitmap, width, height);
                float dxSquared = (x - pixelX) * (x - pixelX);
                float dySquared = (y - pixelY) * (y - pixelY);
                float distanceSquared = dxSquared + dySquared;
                if (pixelState != state) {
                    minDistance = Math.min(distanceSquared, minDistance);
                }
            }
        }

        minDistance = (float) Math.sqrt(minDistance);
        float output = (minDistance - 0.5f) / (spread - 0.5f);
        output *= state == 0 ? -1 : 1;

        return (output + 1) * 0.5f;
    }

    public static void generateCodepointBitmap(int codepoint, String fontFile, int fontSize) {
        int padding = 15;
        int upscaleResolution = 1080;
        int spread = upscaleResolution / 2;

        Library library = FreeType.newLibrary();
        if (library == null) {
            System.err.println("Cannot initiate FreeType library.");
            return;
        }

        Face font = library.newFace(fontFile, 0);

        FT_Set_Pixel_Sizes(font.getPointer(), 0, upscaleResolution);

        if (FT_Load_Char(font.getPointer(), (char) codepoint, FT_LOAD_RENDER)) {
            System.out.println("FreeType could not generate character");
            free(library, font);
            return;
        }

        int glyphWidth = font.getGlyphSlot().getBitmap().getWidth();
        int glyphHeight = font.getGlyphSlot().getBitmap().getRows();
        byte[] glyphBitmap = new byte[glyphHeight * glyphWidth];

        font.getGlyphSlot().getBitmap().getBuffer().get(glyphBitmap, 0, glyphWidth * glyphHeight);

        System.out.println("Glyph width " + glyphWidth);
        System.out.println("Glyph width " + glyphHeight);

        float widthScale = (float) glyphWidth / (float) upscaleResolution;
        float heightScale = (float) glyphHeight / (float) upscaleResolution;

        int charWidth = (int) ((float) fontSize * widthScale);
        int charHeight = (int) ((float) fontSize * heightScale);

        int bitmapWidth = charWidth + padding * 2;
        int bitmapHeight = charHeight + padding * 2;

        float bitmapScaleX = (float) glyphWidth / (float) charWidth;
        float bitmapScaleY = (float) glyphHeight / (float) charHeight;

        int[] bitmap = new int[bitmapWidth * bitmapHeight];

        for (int y = -padding; y < charHeight + padding; y++) {
            for (int x = -padding; x < charWidth + padding; x++) {
                int pixelX = (int) mapRange(x, -padding,
                        charWidth + padding, -padding * bitmapScaleX,
                        (charWidth + padding) * bitmapScaleX);

                int pixelY = (int) mapRange(y, -padding,
                        charHeight + padding, -padding * bitmapScaleY,
                        (charHeight + padding) * bitmapScaleY);

                float val = findNearestPixel(pixelX, pixelY, glyphBitmap, glyphWidth, glyphHeight, spread);

                bitmap[(x + padding) + ((y + padding) * bitmapWidth)] = (int) (val * 255.0f);
            }
        }

        BufferedImage testImage = new BufferedImage(bitmapWidth, bitmapHeight, BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;

        for (int byteAsInt : bitmap) {
            int argb = (255 << 24) | (byteAsInt << 16) | (byteAsInt << 8) | byteAsInt;

            testImage.setRGB(x, y, argb);

            x++;

            if (x >= bitmapWidth) {
                x = 0;
                y++;
            }

            if (y >= bitmapHeight) break;
        }

        try {
            File out = new File("test.png");
            ImageIO.write(testImage, "png", out);
        } catch (IOException e) {
            System.err.println("Failed to generate SDF test image.");
        }

        uploadTexture(testImage);
        free(library, font);
    }

    private static void free(Library library, Face font) {
        FT_Done_Face(font.getPointer());
        FT_Done_FreeType(library.getPointer());
    }

    private static void uploadTexture(BufferedImage image) {
        int[] pixels = new int[image.getHeight() * image.getWidth()];

        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                byte alphaComponent = (byte) ((pixel >> 16) & 0xFF);

                for (int i = 0; i < 4; i++) byteBuffer.put(alphaComponent);
            }
        }

        byteBuffer.flip();

        textureId = glGenTextures();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);

        byteBuffer.clear();
    }
}
