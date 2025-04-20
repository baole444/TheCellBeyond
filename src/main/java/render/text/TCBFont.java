package render.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import utility.PathResolver;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class TCBFont {
    private String filepath;
    private int fontSize;
    private int textureId;
    private int bitmapWidth;
    private int bitmapHeight;
    private Map<Character, CharInfo> characters = new HashMap<>();

    public TCBFont(String filepath, int fontSize, boolean isProjectAsset) throws IOException{
        if (isProjectAsset && CurrentProject != null && ProjectRoot != null) {
            this.filepath = PathResolver.resolveToAbsolute(ProjectRoot, filepath);
        }
        // Assumed that it is a default font that we have in our assets. Or user wants to use an absolute path.
        else this.filepath = new File(filepath).getAbsolutePath();

        verifyFontFile();

        this.fontSize = fontSize;

        // Load font file
        byte[] fontData = Files.readAllBytes(Paths.get(filepath));
        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
        fontBuffer.put(fontData);
        fontBuffer.flip();

        // Create bitmap
        bitmapWidth = 512;
        bitmapHeight = 512;
        ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(96);

        // Gen OpenGL texture.
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Gen char info
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < 96; i++) {
                STBTTBakedChar bakedChar = charData.get(i);
                CharInfo charInfo = new CharInfo(bakedChar.x0(), bakedChar.y0(),
                        bakedChar.x1(), bakedChar.y1(),
                        bakedChar.xoff(), bakedChar.yoff(),
                        bakedChar.xadvance()
                );

                characters.put((char)(i+32), charInfo);
            }
        }

        // Free char data
        charData.free();
    }

    private void verifyFontFile() throws IOException {
        File toVerify = new File(this.filepath);

        if (!toVerify.exists()) throw new IOException("Font file does not exist at: '" + this.filepath + "'");
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

    public void cleanup() {
        glDeleteTextures(textureId);
    }
}
