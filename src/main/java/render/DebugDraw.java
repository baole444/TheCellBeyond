package render;

import TCB_Field.Window;
import org.joml.Vector2f;
import org.joml.Vector3f;
import utility.AssetsPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class DebugDraw {
    private static int MAX_LINE = 500;
    private static List<Line2D> Lines = new ArrayList<>();

    // 6 float vertex, 2 vertices/line
    private static float[] vertexA = new float[MAX_LINE * 6 * 2];

    private static Shader shader = AssetsPool.loadShader("assets/shaders/DBLine2.glsl");

    private static int vaoID;
    private static int vboID;
    private static boolean isLoaded = false;

    public static void start(){
        //VAO
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //Memory buffer
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertexA.length * Float.BYTES, GL_DYNAMIC_DRAW);

        //Attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Line width
        glLineWidth(2.0f);
    }

    public static void startFrame() {
        if (!isLoaded) {
            start();
            isLoaded = true;
        }

        // Remove leftover
        for (int i = 0; i < Lines.size(); i++) {
            if (Lines.get(i).startFrame() < 0) {
                Lines.remove(i);
                i--;
            }
        }
    }

    public static void draw() {
        if (Lines.size() <= 0) return;

        int index = 0;
        for (Line2D line: Lines) {
            for (int i = 0; i < 2; i++) {
                Vector2f pos = i == 0 ? line.loadStart() : line.loadEnd();
                Vector3f color = line.loadColor();

                // Position
                vertexA[index] = pos.x;
                vertexA[index + 1] = pos.y;
                vertexA[index + 2] = -10.0f; //Object depth, can be within defined view range

                // Color
                vertexA[index + 3] = color.x;
                vertexA[index + 4] = color.y;
                vertexA[index + 5] = color.z;

                index += 6;

            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0,
                Arrays.copyOfRange(vertexA, 0, Lines.size() * 6 * 2)
        );

        // Use shader
        shader.use();
        shader.loadMat4f("uProject", Window.getScene().viewport().getProjectMatrix());
        shader.loadMat4f("uView", Window.getScene().viewport().getViewMatrix());

        // Bind VAO
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        // Batching
        glDrawArrays(GL_LINES, 0, Lines.size() * 6 * 2);

        // Bresenham line (maybe later)


        // End
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        shader.detach();
    }

    // Line2D methods

    public static void addLine2(Vector2f start, Vector2f end) {
        // TODO: Add constants for common color
        addLine2(start, end, new Vector3f(1, 1, 0), 1);
    }

    public static void addLine2(Vector2f start, Vector2f end, Vector3f color) {
        addLine2(start, end, color, 1);
    }

    public static void addLine2(Vector2f start, Vector2f end, Vector3f color, int alive) {
        if (Lines.size() >= MAX_LINE) return;
        DebugDraw.Lines.add(new Line2D(start, end, color, alive));
    }

    // Box2D methods

//   public static void addBox2(Vector2f centre, Vector2f dimension,
//                              float rotate, Vector3f color, int alive) {
//       Vector2f min = new Vector2f(centre).sub(new Vector2f(dimension).mul(0.5f));
//       Vector2f max = new Vector2f(centre).add(new Vector2f(dimension).mul(0.5f));

//       Vector2f[] vertices = {
//               new Vector2f(min.x, min.y), new Vector2f(min.x, max.y),
//               new Vector2f(max.x, max.y), new Vector2f(max.x, min.y)
//       };

//       //if (rotate != 0.0f) {
//       //    for (Vector2f v : vertices) {
//       //        JMath.rotate(v, rotate, centre);
//       //    }
//       //}
//   }
}
