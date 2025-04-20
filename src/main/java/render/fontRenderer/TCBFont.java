package render.fontRenderer;

import org.lwjgl.BufferUtils;
import utility.PathResolver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TCBFont {
    private final String filePath;
    private final int fontSize;
    private final String fontName;

    private int width;
    private int height;
    private int lineHeight;

    private static final float PRESUMED_BASELINE = 1.4f;

    public int textureId;

    private Map<Integer, CharInfo> charMap;
    /**
     * Generate a bitmap for given font.
     * @param filePath path to the font file.
     *                 If you specified the font is from your project's file
     *                 by setting {@code isProjectAsset} to @{code true},
     *                 make sure it is a relative path within your project's directory.
     * @param fontSize size of the font to generate.
     * @param isProjectAsset true to use custom font from your project.
     */
    public TCBFont(String filePath, int fontSize, boolean isProjectAsset) throws IOException {
        if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
            this.filePath = PathResolver.resolveToAbsolute(ProjectRoot, filePath);
        }
        // Assumed that it is a default font that we have in our assets. Or user wants to use an absolute path.
        else this.filePath = new File(filePath).getAbsolutePath();

        verifyFontFile();

        this.fontSize = fontSize;
        this.charMap = new HashMap<>();

        String nameWithExtension = new File(this.filePath).getName();

        this.fontName = nameWithExtension.substring(0, nameWithExtension.lastIndexOf('.'));

        generateBitMap();
    }

    private Font registerFont(String fontFile) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(filePath));
            ge.registerFont(font);
            return font;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void verifyFontFile() throws IOException {
        File toVerify = new File(this.filePath);

        if (!toVerify.exists()) throw new IOException("Font file does not exist at: '" + this.filePath + "'");
    }

    private void generateBitMap() {
        Font font = registerFont(filePath);

        if (font == null) {
            System.err.println("Cannot register font.");
            return;
        }

        font = new Font(font.getName(), Font.PLAIN, fontSize);

        /*
        Create a fake image to get
        graphic context -> font information.

        Will be disposed after proper height calculation.
         */
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = image.createGraphics();

        graphics2D.setFont(font);

        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        int estimateWidth = (int) Math.sqrt(font.getNumGlyphs()) * font.getSize() + 1;

        width = 0;
        height = fontMetrics.getHeight();
        lineHeight = fontMetrics.getHeight();

        int x = 0;
        int y = (int) (fontMetrics.getHeight() * PRESUMED_BASELINE);

        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                // Get sizes of current glyph and update image's w and h.
                CharInfo charInfo = new CharInfo(x, y, fontMetrics.charWidth(i), fontMetrics.getHeight());

                charMap.put(i, charInfo);
                width = Math.max(x + fontMetrics.charWidth(i), width);

                x += charInfo.width();

                if (x > estimateWidth) {
                    x = 0;
                    y += (int) (fontMetrics.getHeight() * PRESUMED_BASELINE);
                    height += (int) (fontMetrics.getHeight() * PRESUMED_BASELINE);
                }
            }
        }
        // Add 1 more line to prevent final line cutoff.
        height += (int) (fontMetrics.getHeight() * PRESUMED_BASELINE);

        graphics2D.dispose();

        // Create the real texture image.
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        graphics2D = image.createGraphics();

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setFont(font);
        graphics2D.setColor(Color.WHITE);

        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                CharInfo charInfo = charMap.get(i);
                charMap.get(i).setTextureCoordinates(width, height);
                graphics2D.drawString("" + (char) i, charInfo.sourceX(), charInfo.sourceY());
            }
        }

        // Clear graphic context.
        graphics2D.dispose();

        // Used in test, not important.
        /*
        try {
            File file = new File("tempFont_" + fontName + ".png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            System.err.println("Failed to generate bitmap: " + e.getMessage());
            e.printStackTrace();
        }
         */

        uploadTexture(image);
    }

    private void uploadTexture(BufferedImage image) {
        int[] pixels = new int[image.getHeight() * image.getWidth()];

        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                byte alphaComponent = (byte) ((pixel >> 24) & 0xFF);

                byteBuffer.put((byte) 255);
                byteBuffer.put((byte) 255);
                byteBuffer.put((byte) 255);
                byteBuffer.put(alphaComponent);
            }
        }

        byteBuffer.flip();

        textureId = glGenTextures();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);

        this.width = image.getWidth();
        this.height = image.getHeight();

        byteBuffer.clear();
    }

    public CharInfo getCharacter(int codePoint) {
        return charMap.getOrDefault(codePoint, new CharInfo(0, 0, 0, 0));
    }
}
