package render;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.*;

public class ObjectSelection {
    private int objSelectID;
    private int frameBufferObj;
    private int textureDepth;

    public ObjectSelection(int width, int height) {
        if (!init(width, height)) {
            assert false : "Failed to initialize object selection within working space";
        }
    }

    public boolean init(int width, int height) {
        // Make frame buffer

        frameBufferObj = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObj);

        // Generate texture to frame buffer
        objSelectID = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, objSelectID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F,
                width, height, 0,
                GL_RGB, GL_FLOAT, 0
        );

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, this.objSelectID, 0
        );

        // Generate texture object for depth buffer
        glEnable(GL_TEXTURE_2D);
        textureDepth = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureDepth);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT,
                width, height, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, 0
        );
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_2D, textureDepth, 0);

        // Backward compatible for unsupported GPU

        glReadBuffer(GL_NONE);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            assert false : "WARNING: FrameBuffer did not finished";
            return false;
        }
        // Send frame back to window
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return true;
    }

    public void useWrite() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, frameBufferObj);
    }

    public void detachWrite() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }
    //TODO: need to check on glReadPixels
    public int pixelCheck(int x, int y) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, frameBufferObj);
        glReadBuffer(GL_COLOR_ATTACHMENT0);

        float pixel[] = new float[3];
        glReadPixels(x, y, 1, 1, GL_RGB, GL_FLOAT, pixel);

        return (int)pixel[0] - 1;
    }

}
