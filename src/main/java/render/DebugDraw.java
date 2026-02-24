package render;

import TheCellBeyond.Viewport;
import TheCellBeyond.internal.LogicServer;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.TCBMath;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * DebugDraw gives visual aid for shape and boundary that is not visible.
 */
public final class DebugDraw {
    private static final int MAX_LINE = 4096;
    private static final List<Line2D> Lines = new ArrayList<>();
    private static Shader shader;

    private static final float[] vertices = new float[MAX_LINE * 7 * 2];
    private static int vaoID;
    private static int vboID;
    private static boolean init = false;

    /**
     * Create debug draw pipeline.
     */
    private DebugDraw() {}

    /**
     * Initialize debug draw batch.
     * @param s the shader program to draw debug lines
     */
    public static void init(Shader s) {
        shader = s;
        start();
        init = true;
    }

    /**
     * Setup vertex attribute pointers.
     */
    private static void start() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glLineWidth(2.0f);
    }

    /**
     * Start the frame and capture debug draw request.
     * This also remove line that ran out of alive tick.
     */
    public static void startFrame() {
        for (int i = 0; i < Lines.size(); i++) {
            if (Lines.get(i).startFrame() >= 0) continue;
            Lines.remove(i);
            i--;
        }
    }

    /**
     * Draw the request in the frame.
     */
    public static void draw() {
        if (LogicServer.currentScene() == null) {
            Lines.clear();
            return;
        }
        if (Lines.isEmpty() || !init || shader == null) return;
        Viewport viewport = LogicServer.currentSceneViewport();
        if (viewport == null) {
            Lines.clear();
            return;
        }
        int index = 0;
        for (Line2D line: Lines) {
            for (int i = 0; i < 2; i++) {
                Vector2f pos = i == 0 ? line.getStart() : line.getEnd();
                Vector4f color = line.color();
                vertices[index] = pos.x;
                vertices[index + 1] = pos.y;
                vertices[index + 2] = -10.0f;
                vertices[index + 3] = color.x;
                vertices[index + 4] = color.y;
                vertices[index + 5] = color.z;
                vertices[index + 6] = color.w;
                index += 7;
            }
        }
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        shader.use();
        shader.loadMat4f("uProject", viewport.getProjectionMatrix());
        shader.loadMat4f("uView", viewport.getViewMatrix());
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_LINES, 0, Lines.size() * 2);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
        shader.detach();
    }

    /**
     * Add a line draw request in white and alive for 1 frame.
     * @param start start position in world units
     * @param end end position in world units
     */
    public static void addLine2(Vector2f start, Vector2f end) {
        addLine2(start, end, new Vector4f(1, 1, 1, 1), 1);
    }

    /**
     * Add a line draw request alive for 1 frame.
     * @param start start position in world units
     * @param end end position in world units
     * @param color the colour vector for the line
     */
    public static void addLine2(Vector2f start, Vector2f end, Vector4f color) {
        addLine2(start, end, color, 1);
    }

    /**
     * Add a line draw request.
     * @param start start position in world units
     * @param end end position in world units
     * @param color the colour vector for the line
     * @param alive the number of frames that this line will last for
     */
    public static void addLine2(Vector2f start, Vector2f end, Vector4f color, int alive) {
        if (Lines.size() >= MAX_LINE) return;
        DebugDraw.Lines.add(new Line2D(start, end, color, alive));
    }

    /**
     * Add a box draw request in white and alive for 1 frame.
     * @param centre the centre of the box in world units
     * @param dimension the dimension of the box in world units
     * @param rotationDegrees the rotation angle in degrees
     */
    public static void addBox2(Vector2f centre, Vector2f dimension, float rotationDegrees) {
        addBox2(centre, dimension, rotationDegrees, new Vector4f(1, 1, 1, 1), 1);
    }

    /**
     * Add a box draw request alive for 1 frame.
     * @param centre the centre of the box in world units
     * @param dimension the dimension of the box in world units
     * @param rotationDegrees the rotation angle in degrees
     * @param color the colour vector for the box's lines
     */
    public static void addBox2(Vector2f centre, Vector2f dimension, float rotationDegrees, Vector4f color) {
        addBox2(centre, dimension, rotationDegrees, color, 1);
    }

    /**
     * Add a box draw request.
     * @param centre the centre of the box in world units
     * @param dimension the dimension of the box in world units
     * @param rotationDegrees the rotation angle in degrees
     * @param color the colour vector for the box's lines
     * @param alive the number of frames that this box will last for
     */
    public static void addBox2(Vector2f centre, Vector2f dimension, float rotationDegrees, Vector4f color, int alive) {
        Vector2f min = new Vector2f(centre).sub(new Vector2f(dimension).mul(0.5f));
        Vector2f max = new Vector2f(centre).add(new Vector2f(dimension).mul(0.5f));
        Vector2f[] vertices = {new Vector2f(min.x, min.y), new Vector2f(min.x, max.y), new Vector2f(max.x, max.y), new Vector2f(max.x, min.y)};
        if (rotationDegrees != 0.0f) for (Vector2f v : vertices) TCBMath.rotate(v, rotationDegrees, centre);
        addLine2(vertices[0], vertices[1], color, alive);
        addLine2(vertices[0], vertices[3], color, alive);
        addLine2(vertices[1], vertices[2], color, alive);
        addLine2(vertices[2], vertices[3], color, alive);
    }

    /**
     * Add a circle draw request in white and alive for 1 frame.
     * @param centre the centre of the circle in world units
     * @param radius the radius of the circle in world units
     */
    public static void addCircle(Vector2f centre, float radius) {
        addCircle(centre, radius, new Vector4f(1, 1, 1, 1), 1);
    }

    /**
     * Add a circle draw request alive for 1 frame.
     * @param centre the centre of the circle in world units
     * @param radius the radius of the circle in world units
     * @param color the colour vector for the circle's lines
     */
    public static void addCircle(Vector2f centre, float radius, Vector4f color) {
        addCircle(centre, radius, color, 1);
    }

    /**
     * Add a circle draw request.
     * @param centre the centre of the circle in world units
     * @param radius the radius of the circle in world units
     * @param color the colour vector for the circle's lines
     * @param alive the number of frames that this circle will last for
     */
    public static void addCircle(Vector2f centre, float radius, Vector4f color, int alive) {
        Vector2f[] pt = new Vector2f[36];
        int step = 360 / pt.length;
        int startAngle = 0;
        for (int i = 0; i < pt.length; i++) {
            Vector2f tmp = new Vector2f(radius, 0);
            TCBMath.rotate(tmp, startAngle, new Vector2f());
            pt[i] = new Vector2f(tmp).add(centre);
            if (i > 0) addLine2(pt[i - 1], pt[i], color, alive);
            startAngle += step;
        }
        addLine2(pt[pt.length - 1], pt[0], color, alive);
    }
}
