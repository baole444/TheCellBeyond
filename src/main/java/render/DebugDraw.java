package render;

import TheCellBeyond.Window;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.TCBMath;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class DebugDraw {
    private static final int MAX_LINE = 4096;
    private static final List<Line2D> Lines = new ArrayList<>();
    private static Shader shader;

    private static final float[] vertices = new float[MAX_LINE * 7 * 2];
    private static int vaoID;
    private static int vboID;
    private static boolean init = false;

    public static void init(Shader s) {
        shader = s;
        start();
        init = true;
    }

    public static void start() {
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

    public static void startFrame() {
        for (int i = 0; i < Lines.size(); i++) {
            if (Lines.get(i).startFrame() < 0) {
                Lines.remove(i);
                i--;
            }
        }
    }

    public static void draw() {
        if (Lines.isEmpty() || !init || shader == null) return;

        int index = 0;
        for (Line2D line: Lines) {
            for (int i = 0; i < 2; i++) {
                Vector2f pos = i == 0 ? line.getStart() : line.getEnd();
                Vector4f color = line.color();

                // Position
                vertices[index] = pos.x;
                vertices[index + 1] = pos.y;
                vertices[index + 2] = -10.0f;

                // Color
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
        shader.loadMat4f("uProject", Window.getScene().viewport().getProjectionMatrix());
        shader.loadMat4f("uView", Window.getScene().viewport().getViewMatrix());

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_LINES, 0, Lines.size() * 7 * 2);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        shader.detach();
    }

    public static void addLine2(Vector2f start, Vector2f end) {
        addLine2(start, end, new Vector4f(1, 1, 1, 1), 1);
    }

    public static void addLine2(Vector2f start, Vector2f end, Vector4f color) {
        addLine2(start, end, color, 1);
    }

    public static void addLine2(Vector2f start, Vector2f end, Vector4f color, int alive) {
        if (Lines.size() >= MAX_LINE) return;
        DebugDraw.Lines.add(new Line2D(start, end, color, alive));
    }

    public static void addBox2(Vector2f centre, Vector2f dimension, float rotate) {
        addBox2(centre, dimension, rotate, new Vector4f(1, 1, 1, 1), 1);
    }

    public static void addBox2(Vector2f centre, Vector2f dimension, float rotate, Vector4f color) {
        addBox2(centre, dimension, rotate, color, 1);
    }

    public static void addBox2(Vector2f centre, Vector2f dimension, float rotate, Vector4f color, int alive) {
        Vector2f min = new Vector2f(centre).sub(new Vector2f(dimension).mul(0.5f));
        Vector2f max = new Vector2f(centre).add(new Vector2f(dimension).mul(0.5f));
        Vector2f[] vertices = {new Vector2f(min.x, min.y), new Vector2f(min.x, max.y), new Vector2f(max.x, max.y), new Vector2f(max.x, min.y)};

        if (rotate != 0.0f) for (Vector2f v : vertices) TCBMath.rotate(v, rotate, centre);

        addLine2(vertices[0], vertices[1], color, alive);
        addLine2(vertices[0], vertices[3], color, alive);
        addLine2(vertices[1], vertices[2], color, alive);
        addLine2(vertices[2], vertices[3], color, alive);
    }

    public static void addCircle(Vector2f centre, float radius) {
        addCircle(centre, radius, new Vector4f(1, 1, 1, 1), 1);
    }

    public static void addCircle(Vector2f centre, float radius, Vector4f color) {
        addCircle(centre, radius, color, 1);
    }

    public static void addCircle(Vector2f centre, float radius, Vector4f color, int alive) {
        Vector2f[] pt = new Vector2f[36];
        int step = 360 / pt.length;
        int startAngle = 0;

        for (int i = 0; i < pt.length; i++) {
            Vector2f tmp = new Vector2f(radius, 0);
            TCBMath.rotate(tmp, startAngle, new Vector2f());
            pt[i] = new Vector2f(tmp).add(centre);

            if (i > 0) {
                addLine2(pt[i - 1], pt[i], color, alive);
            }

            startAngle += step;
        }
        addLine2(pt[pt.length - 1], pt[0], color, alive);
    }
}
