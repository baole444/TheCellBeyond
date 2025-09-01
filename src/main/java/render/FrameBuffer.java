package render;

import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {
    private int width;
    private int height;

    private long windowPtr = 0;
    private int renderBufferObjID = 0;
    private int frameBufferObjID = 0;
    private Texture texture = null;

    private boolean waitingForTexture = false;

    public FrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    public FrameBuffer(int width, int height, long windowPointer) {
        this.width = width;
        this.height = height;
        this.windowPtr = windowPointer;
        init();
    }

    private void init() {
        // Make frame buffer
        this.frameBufferObjID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObjID);

        // Generate texture to frame buffer
        this.texture = Texture.createFrameBufferTexture(width, height);
        this.waitingForTexture = true;

        setupRenderBuffer();

        completeFrameBufferSetup();

        // Send frame back to window
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    private void setupRenderBuffer() {
        this.renderBufferObjID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderBufferObjID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBufferObjID);
    }

    private void completeFrameBufferSetup() {
        if (waitingForTexture && texture != null && texture.isReady()) {
            glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObjID);

            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    this.texture.getID(),
                    0
            );

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Error: Framebuffer is not complete");
            }

            waitingForTexture = false;
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public void setWindow(long windowPtr) {
        this.windowPtr = windowPtr;
    }

    public void use() {
        if (waitingForTexture) completeFrameBufferSetup();

        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObjID);
    }

    public void detach() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getFrameBufferObjID() {
        return frameBufferObjID;
    }

    public int getTextureID() {
        return texture != null ? texture.getID() : -1;
    }

    public boolean isReady() {
        return !waitingForTexture && texture != null && texture.isReady();
    }

    /**
     * Render the framebuffer contents to the screen.
     * Call this after {@link FrameBuffer#detach()} to render the framebuffer's content.
     */
    public void renderToScreen() {
        if (!isReady()) return;

        if (windowPtr == 0) {
            renderToScreen(width, height);
        } else {
            IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);

            glfwGetFramebufferSize(windowPtr, widthBuffer, heightBuffer);
            int winWidth = widthBuffer.get();
            int winHeight = heightBuffer.get();

            renderToScreen(winWidth, winHeight);
        }
    }

    private void renderToScreen(int targetWidth, int targetHeight) {
        // Bind framebuffer as read buffer
        glBindFramebuffer(GL_READ_FRAMEBUFFER, frameBufferObjID);

        // Bind default framebuffer as draw buffer
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        // Copy framebuffer to screen
        glBlitFramebuffer(0, 0, width, height, // Source rectangle
                0 ,0, targetWidth, targetHeight,  // Destination
                GL_COLOR_BUFFER_BIT, GL_NEAREST // Mask and filter
        );

        // Unbind
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Wrapper to bind framebuffer, execute rendering codes, then detach framebuffer and render it to screen.
     * @param renderAction rendering tasks that create frame
     *                     (texture rendering, text rendering, effects, etc.)
     *                     for the framebuffer to draw.
     */
    public void captureAndRender(Runnable renderAction) {
        if (!isReady()) {
            renderAction.run();
            return;
        }

        use();

        renderAction.run(); // execute render codes

        detach();

        renderToScreen();
    }

    public void clear(float r, float g, float b, float a) {
        if (!isReady()) return;

        use();
        glClearColor(r, g, b, a);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        detach();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void resize (int width, int height) {
        if (width <= 0 || height <= 0) return;

        dispose();

        this.width = width;
        this.height = height;

        init();
    }

    public void dispose() {
        if (frameBufferObjID != 0) {
            glDeleteFramebuffers(frameBufferObjID);
            frameBufferObjID = 0;
        }

        if (renderBufferObjID != 0) {
            glDeleteRenderbuffers(renderBufferObjID);
            renderBufferObjID = 0;
        }

        if (texture != null) {
            texture.dispose();
            texture = null;
        }

        waitingForTexture = false;
    }

}
