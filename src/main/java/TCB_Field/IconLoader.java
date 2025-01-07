package TCB_Field;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

public class IconLoader {
    private ByteBuffer icon;
    private int width, height;

    public ByteBuffer getIcon() {
        return icon;
    }

    IconLoader(int w, int h, ByteBuffer icon) {
        this.icon = icon;
        this.height = h;
        this.width = w;
    }

    public int loadIconW() {
        return width;
    }

    public int loadIconH() {
        return height;

    }
    public static IconLoader loadIcon(String filepath) {
        ByteBuffer icon;
        int width, height;
        //  Image repeater

        //stbi_set_flip_vertically_on_load(true);

        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer w = s.mallocInt(1);
            IntBuffer h = s.mallocInt(1);
            IntBuffer channels = s.mallocInt(1);

            icon = stbi_load(filepath, w, h, channels, 4);

            if (icon == null ) {
                assert false : "FATAL: Texture failed to load! '" + filepath + " '";
            }

            width = w.get();
            height = h.get();

        }
        //stbi_image_free(icon); //Free memory and prevent memory leak

        return new IconLoader(width, height, icon);
    }
}
