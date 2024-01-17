package render;

import org.joml.*;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;

public class Shader {

    private int shaderProgramID;

    private boolean inUse = false;
    private String vertexSrc;

    private String fragmentSrc;

    private String filepath;
    public Shader(String filepath) {
        this.filepath = filepath;
        try {
            String src = new String(Files.readAllBytes(Paths.get(filepath)));
            String[] splitString = src.split("(#type)( )+([a-zA-Z]+)");

            //Find first pattern of #type
            int index = src.indexOf("#type") + 6;
            int eol = src.indexOf("\r\n", index);
            String firstPattern = src.substring(index, eol).trim();

            //Find second
            index = src.indexOf("#type", eol) + 6;
            eol = src.indexOf("\r\n", index);
            String secondPattern = src.substring(index, eol).trim();

            if (firstPattern.equals("vertex")) {
                vertexSrc = splitString[1];
            } else if (firstPattern.equals("fragment")) {
                fragmentSrc = splitString[1];
            } else {
                throw new IOException("Unexpected token '" + firstPattern + "'");
            }

            if (secondPattern.equals("fragment")) {
                fragmentSrc = splitString[2];
            } else if (secondPattern.equals("vertex")) {
                vertexSrc = splitString[2];
            } else {
                throw new IOException("Unexpected token '" + secondPattern + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Error: Cannot open file or shader: '" + filepath + "'";
        }


    }

    public void compile() {
        int vertexID, fragmentID;
        // compile and link shader

        //First load and compile the vertex shader
        vertexID = glCreateShader(GL_VERTEX_SHADER);

        //Pass shader to GPU
        glShaderSource(vertexID, vertexSrc);
        glCompileShader(vertexID);

        //Catch errors
        int success = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: 'default.glsl'\n\tVertex shader failed to compiled.");
            System.out.println(glGetShaderInfoLog(vertexID, len));
            assert false: "";
        }

        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);

        //Pass shader to GPU
        glShaderSource(fragmentID, fragmentSrc);
        glCompileShader(fragmentID);

        //Catch errors
        success = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: '" + filepath + " '\n\tFragment shader failed to compiled.");
            System.out.println(glGetShaderInfoLog(fragmentID, len));
            assert false: "";
        }

        //Link shader
        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        glLinkProgram(shaderProgramID);

        // linking error

        success = glGetProgrami(shaderProgramID, GL_LINK_STATUS);
        if (success == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: '" + filepath + " '\n\tLink shader failed.");
            System.out.println(glGetProgramInfoLog(shaderProgramID, len));
            assert false: "";
        }
    }

    public void use() {
        // Bind shader
        if (!inUse) {
            glUseProgram(shaderProgramID);
        }
    }

    public void detach() {
        glUseProgram(0);
        inUse = false;
    }

    public void loadMat4f(String var, Matrix4f mat4) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        FloatBuffer matBuff = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuff);
        glUniformMatrix4fv(varLocate, false, matBuff);
    }

    public void loadMat3f(String var, Matrix3f mat3) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        FloatBuffer matBuff = BufferUtils.createFloatBuffer(9);
        mat3.get(matBuff);
        glUniformMatrix3fv(varLocate, false, matBuff);
    }

    public void loadVec4f(String var, Vector4f vec) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform4f(varLocate, vec.x, vec.y, vec.z, vec.w);
    }

    public void loadVec3f(String var, Vector3f vec) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform3f(varLocate, vec.x, vec.y, vec.z);
    }

    public void loadVec2f(String var, Vector2f vec) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform2f(varLocate, vec.x, vec.y);
    }

    public void loadFloat(String var, float val) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1f(varLocate, val);
    }

    public void loadInt(String var, int val) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1i(varLocate, val);
    }

    public void loadTexture(String var, int slot) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1i(varLocate, slot);
    }
}
