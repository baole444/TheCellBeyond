package render;

import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {
    private int frameBufferObjID = 0;
    private Texture texture = null;


    public FrameBuffer(int width, int height) {
        // Make frame buffer

        frameBufferObjID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObjID);

        // Generate texture to frame buffer
        this.texture = new Texture(width, height);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                this.texture.loadID(),
                0
        );

        // Store depth info with render buffer

        int renderBufferObjID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderBufferObjID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBufferObjID);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            assert false : "WARNING: FrameBuffer did not finished";
        }
        // Send frame back to window
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public void use() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObjID);
    }

    public void detach() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int loadFrameBufferObjID() {
        return frameBufferObjID;
    }

    public int loadTexID() {
        return texture.loadID();
    }

}
