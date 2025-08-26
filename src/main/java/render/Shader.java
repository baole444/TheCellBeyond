package render;

import org.joml.*;
import org.lwjgl.BufferUtils;
import utility.AssetReference;
import utility.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;

public class Shader {
    private int shaderProgramID;

    private boolean isInUse = false;

    private String vertexSrc;

    private String fragmentSrc;

    private final AssetReference assetReference;

    private boolean isCompiled = false;

    public Shader(String filepath) {
        this.assetReference = new AssetReference(filepath);

        loadShaderSource();
    }

    private void loadShaderSource() {
        PathResolver resolver = PathResolver.get();

        try (InputStream stream = resolver.getAssetStream(assetReference.getResolvedPath())) {
            String src = new String(stream.readAllBytes());
            parseShaderSource(src);
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Error: Cannot open file or shader: '" + getFilePath() + "'";
        }
    }

    private void parseShaderSource(String src) {
        try {
            String[] splitString = src.split("(#type)( )+([a-zA-Z]+)");

            //Find the first pattern of #type
            int index = src.indexOf("#type") + 6;
            int eol = src.indexOf("\n", index);
            String firstPattern = src.substring(index, eol).trim();

            //Find second
            index = src.indexOf("#type", eol) + 6;
            eol = src.indexOf("\n", index);
            String secondPattern = src.substring(index, eol).trim();

            if (firstPattern.equals("vertex")) {
                vertexSrc = splitString[1];
            } else if (firstPattern.equals("fragment")) {
                fragmentSrc = splitString[1];
            } else {
                throw new IOException("Unexpected token '" + firstPattern + "'");
            }

            if (secondPattern.equals("vertex")) {
                vertexSrc = splitString[2];
            } else if (secondPattern.equals("fragment")) {
                fragmentSrc = splitString[2];
            } else {
                throw new IOException("Unexpected token '" + secondPattern + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Error: Failed to parse shader: '" + getFilePath() + "'";
        }
    }

    public void compile() {
        if (isCompiled) return;

        int vertexID, fragmentID;
        // Compile and link shader:

        // First load and compile the vertex shader.
        vertexID = glCreateShader(GL_VERTEX_SHADER);

        // Pass shader to GPU.
        glShaderSource(vertexID, vertexSrc);
        glCompileShader(vertexID);

        // Catch errors.
        int success = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: '" + getFilePath() + "' \n\tVertex shader failed to compiled.");
            System.out.println(glGetShaderInfoLog(vertexID, len));
            assert false: "";
        }

        // First load and compile the fragment shader.
        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);

        // Pass shader to GPU.
        glShaderSource(fragmentID, fragmentSrc);
        glCompileShader(fragmentID);

        // Catch errors.
        success = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: '" + getFilePath() + "' \n\tFragment shader failed to compiled.");
            System.out.println(glGetShaderInfoLog(fragmentID, len));
            assert false: "";
        }

        // Link shader.
        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        glLinkProgram(shaderProgramID);

        // Catch linking errors.
        success = glGetProgrami(shaderProgramID, GL_LINK_STATUS);
        if (success == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            System.out.println("FATAL: '" + getFilePath() + " '\n\tLink shader failed.");
            System.out.println(glGetProgramInfoLog(shaderProgramID, len));
            assert false: "";
        }

        isCompiled = true;
    }

    public void use() {
        if (!isCompiled) compile();

        // Bind shader
        if (!isInUse) glUseProgram(shaderProgramID);
    }

    public void detach() {
        glUseProgram(0);
        isInUse = false;
    }

    public String getFilePath() {
        return assetReference != null ? assetReference.getCanonicalPath() : null;
    }

    /**
     * Copy this shader in a thread-safe manner.
     * Note: OpenGL shader program ID is not reusable as it is context-specific.
     */
    public Shader copy() {
        return new Shader(assetReference.getCanonicalPath());
    }

    public void reload() {
        if (isCompiled && shaderProgramID != 0) {
            glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
            isCompiled = false;
            isInUse = false;
        }

        loadShaderSource();
        compile();
    }

    public boolean exists() {
        PathResolver resolver = PathResolver.get();
        return resolver.exists(assetReference.getResolvedPath());
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

    public void loadIntA(String var, int[] array) {
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1iv(varLocate, array);
    }

    public void dispose() {
        if (isCompiled && shaderProgramID != 0) {
            glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
            isCompiled = false;
            isInUse = false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Shader target)) return false;

        return Objects.equals(getFilePath(), target.getFilePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilePath());
    }

    @Override
    public String toString() {
        return "Shader{" + getFilePath() + "}";
    }
}
