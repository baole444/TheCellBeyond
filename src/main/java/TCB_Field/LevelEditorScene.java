package TCB_Field;

import components.FontRender;
import components.SpriteRender;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import render.Shader;
import render.Texture;
import utility.Time;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class LevelEditorScene extends Scene {
    private float[] vertexA = {
            //position                     //color                           // UV coordinate
             50.0f, -50.0f, 0.0f,          0.0f, 0.0f, 0.0f, 1.0f,           1, 1,   //bottom right  0
            -50.0f,  50.0f, 0.0f,          0.0f, 0.0f, 0.0f, 1.0f,           0, 0,   //top left      1
             50.0f,  50.0f, 0.0f,          0.0f, 0.0f, 0.0f, 1.0f,           1, 0,   //top right     2
            -50.0f, -50.0f, 0.0f,          0.0f, 0.0f, 0.0f, 1.0f,           0, 1    //bottom left   3
    };

    private int[] elementA = {
            //MUST BE COUNTER-CLOCK-WISE
            /*

                x       x


                x       x

            */
            2, 1, 0,    //top right tris
            0, 1, 3     //bottom right tris
    };
    private int vaoID, vboID, eboID;

    private Shader defaultShader;
    private Texture place_holder_texture;

    GameObject place_holder_object;
    private boolean isFirst = false;

    public LevelEditorScene() {


    }

    @Override
    public void init() {

        System.out.println("Loading placeholder object.");
        this.place_holder_object = new GameObject("place_holder_object");
        this.place_holder_object.addComponent(new SpriteRender());
        this.place_holder_object.addComponent(new FontRender());
        this.addObjToScene(this.place_holder_object);

        this.viewport = new Viewport(new Vector2f(-320, -240));
        defaultShader = new Shader(("assets/shaders/default.glsl"));
        defaultShader.compile();
        this.place_holder_texture = new Texture("assets/texture/Just_a_placeholder.png");
        // Vertex buff object to GPU
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Vertices float buffer

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexA.length);
        vertexBuffer.put(vertexA).flip();

        // upload VBO

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // indices and upload
        IntBuffer elementBuffer = BufferUtils.createIntBuffer(elementA.length);
        elementBuffer.put(elementA).flip();

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);

        // vertex attribute pointer

        int positionSize = 3;
        int colorSize = 4;
        int uvSize = 2;
        //int floatSizeBytes = 4; //In case Float.BYTES fail
        int vertexSizeBytes = (positionSize + colorSize + uvSize) * Float.BYTES;

        glVertexAttribPointer(0, positionSize, GL_FLOAT, false, vertexSizeBytes, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexSizeBytes, positionSize * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, uvSize, GL_FLOAT, false, vertexSizeBytes, (positionSize + colorSize) *  Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    @Override
    public void update(float dt) {
        //viewport.position.x -= dt * 25.0f; //some cool moving stuff
        //viewport.position.y -= dt * 18.75f;

        defaultShader.use();

        // Load texture
        defaultShader.loadTexture("texSampler", 0);
        glActiveTexture(GL_TEXTURE0);
        place_holder_texture.bind();

        defaultShader.loadMat4f("uProject", viewport.getProjectMatrix());
        defaultShader.loadMat4f("uView", viewport.getViewMatrix());
        defaultShader.loadFloat("uTime", Time.getTime());
        // Bind VAO
        glBindVertexArray(vaoID);
        // Enable vertex attribute pointers
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES, elementA.length, GL_UNSIGNED_INT, 0);

        // Unbind all
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glBindVertexArray(0);
        defaultShader.detach();

        if (!isFirst) {
            System.out.println("Loading game object.");
            GameObject go = new GameObject("Test 2");
            go.addComponent(new SpriteRender());
            this.addObjToScene(go);
            isFirst = true;
        }

        for (GameObject go : this.gObjects) {
            go.update(dt);
        }
    }

}
