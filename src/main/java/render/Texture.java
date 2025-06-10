package render;

import org.lwjgl.BufferUtils;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private AssetReference assetReference;
    private transient int textureID;
    private int width, height;

    public Texture() {
        // Intended to fail if parameter not set
        textureID = -1;
        width = -1;
        height = -1;
    }

    public Texture(int width, int height) {
        assetReference = null;

        // Generate texture on GPU
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Generate empty space
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                width, height,
                0, GL_RGB, GL_UNSIGNED_BYTE, 0
        );
    }

    public void init(String filepath) {
        assetReference = new AssetReference(filepath);

        loadTextureDate();
    }

    private void loadTextureDate() {
        PathResolver resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            byte[] data = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();

            loadFromBuffer(buffer);
        } catch (IOException e) {
            System.err.println("Failed to load texture: " + assetReference.getCanonicalPath());
            e.printStackTrace();
        }
    }

    private void loadFromBuffer(ByteBuffer buffer) {
        // Generate texture on GPU
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture parameters

        //  Image repeater

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Image style
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        // Downsize
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        stbi_set_flip_vertically_on_load(true);

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        ByteBuffer image = stbi_load_from_memory(buffer, width, height, channels, 0);

        if (image != null ) {
            this.width = width.get(0);
            this.height = height.get(0);
            if (channels.get(0) == 3) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0),
                        0, GL_RGB, GL_UNSIGNED_BYTE, image);
            } else if (channels.get(0) == 4) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                        0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            } else {
                assert false : "Error: Unknown texture channels '" + channels.get(0) + " '";
            }

        } else {
            assert false : "FATAL: Texture failed to load! '" + getFilePath() + " '";
        }

        stbi_image_free(image); //Free memory and prevent memory leak
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getID() {
        return textureID;
    }

    public String getFilePath() {
        return assetReference != null ? assetReference.getCanonicalPath() : null;
    }

    public void setFilePath(String path) {
        this.assetReference = new AssetReference(path);
    }

    public Texture copy() {
        Texture copy = new Texture();

        if (this.assetReference != null) {
            copy.init(assetReference.getCanonicalPath());
        }

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Texture objTex)) return false;

        return objTex.getWidth() == this.width &&
                objTex.getHeight() == this.height &&
                objTex.getID() == this.textureID &&
                Objects.equals(objTex.getFilePath(), this.getFilePath());
    }
}
