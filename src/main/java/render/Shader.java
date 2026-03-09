package render;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import org.joml.*;
import org.lwjgl.BufferUtils;
import utility.AssetReference;
import utility.AssetResourceType;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;

public class Shader {
    private static final Pattern ShaderRegex = Pattern.compile("#type\\s+(\\w+)\\s*\\n");
    private static final EngineLog Logger = new EngineLog(Shader.class);
    public final ResourceID RID = new ResourceID(AssetResourceType.Shader);
    private int shaderProgramID;
    private String vertexSrc;
    private String fragmentSrc;
    private final AssetReference assetReference;
    private volatile boolean compiled = false;
    private volatile boolean inUsed = false;
    private volatile boolean failed = false;

    public Shader(String filepath) {
        assetReference = new AssetReference(filepath);
        loadShaderSource();
    }

    private void loadShaderSource() {
        UnifiedPaths resolver = UnifiedPaths.get();
        try (InputStream stream = resolver.getAssetStream(assetReference.resolvedPath())) {
            String src = new String(stream.readAllBytes());
            parseShaderSource(src);
        } catch (IOException e) {
            Logger.error(String.format("Failed to open shader at '%s': %s", getFilePath(), e.getMessage()));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
        }
    }

    private void parseShaderSource(String src) {
        if (src == null || src.isBlank()) {
            Logger.error(String.format("Cannot parse shader source from '%s': empty or null source", getFilePath()));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            return;
        }
        Matcher matcher = ShaderRegex.matcher(src);
        int previousEnd = -1;
        String previousType = null;
        while (matcher.find()) {
            if (previousType != null) {
                assignSection(previousType, src.substring(previousEnd, matcher.start()));
                if (failed) return;
            }
            previousType = matcher.group(1).toLowerCase();
            previousEnd = matcher.end();
        }
        if (previousType != null) {
            assignSection(previousType, src.substring(previousEnd));
            if (failed) return;
        }
        if (vertexSrc == null) {
            Logger.error(String.format("Cannot parse shader '%s': missing a #type vertex section", getFilePath()));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            return;
        }
        if (fragmentSrc == null) {
            Logger.error(String.format("Cannot parse shader '%s': missing a #type fragment section", getFilePath()));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
        }
    }

    public void compile() {
        if (compiled || failed) return;
        int vertexID, fragmentID;
        vertexID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexID, vertexSrc);
        glCompileShader(vertexID);
        int success = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);
            Logger.error(String.format("Failed to compile vertex shader from '%s': %s", getFilePath(), glGetShaderInfoLog(vertexID, len)));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            return;
        }
        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentID, fragmentSrc);
        glCompileShader(fragmentID);
        success = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH);
            Logger.error(String.format("Failed to compile fragment shader from '%s': %s", getFilePath(), glGetShaderInfoLog(fragmentID, len)));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            return;
        }
        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        glLinkProgram(shaderProgramID);
        success = glGetProgrami(shaderProgramID, GL_LINK_STATUS);
        if (success == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            Logger.error(String.format("Failed to link shader from '%s': %s", getFilePath(), glGetProgramInfoLog(shaderProgramID, len)));
            failed = true;
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            return;
        }
        compiled = true;
        ResourceStatusCallback.emit(RID, ResourceStatus.READY);
    }

    /**
     * Bind this shader to the GPU if it is not in used.
     * A shader cannot bind if it had failed to parse or not compiled.
     */
    public void use() {
        if (failed) return;
        if (!compiled) compile();
        if (!inUsed) glUseProgram(shaderProgramID);
    }

    /**
     * Unbind this shader from the GPU. This will set the shader program ptr in GPU to {@code 0},
     * and mark this shader as not in used.
     */
    public void detach() {
        glUseProgram(0);
        inUsed = false;
    }

    public String getFilePath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    /**
     * Copy this shader in a thread-safe manner.
     * Note: OpenGL shader program ID is not reusable as it is context-specific.
     */
    public Shader copy() {
        return new Shader(assetReference.canonicalPath());
    }

    public void reload() {
        if (compiled && shaderProgramID != 0) {
            glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
            compiled = false;
            inUsed = false;
        }
        failed = false;
        loadShaderSource();
        compile();
    }

    /**
     * Check if the shader source file is at the given file location.
     * @return true if file exist
     */
    public boolean exists() {
        UnifiedPaths resolver = UnifiedPaths.get();
        return resolver.exists(assetReference.resolvedPath());
    }

    public void loadMat4f(String var, Matrix4f mat4) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        FloatBuffer matBuff = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuff);
        glUniformMatrix4fv(varLocate, false, matBuff);
    }

    public void loadMat3f(String var, Matrix3f mat3) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        FloatBuffer matBuff = BufferUtils.createFloatBuffer(9);
        mat3.get(matBuff);
        glUniformMatrix3fv(varLocate, false, matBuff);
    }

    public void loadVec4f(String var, Vector4f vec) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform4f(varLocate, vec.x, vec.y, vec.z, vec.w);
    }

    public void loadVec3f(String var, Vector3f vec) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform3f(varLocate, vec.x, vec.y, vec.z);
    }

    public void loadVec2f(String var, Vector2f vec) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform2f(varLocate, vec.x, vec.y);
    }

    public void loadFloat(String var, float val) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1f(varLocate, val);
    }

    public void loadInt(String var, int val) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1i(varLocate, val);
    }

    public void loadTexture(String var, int slot) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1i(varLocate, slot);
    }

    public void loadIntA(String var, int[] array) {
        if (failed) return;
        int varLocate = glGetUniformLocation(shaderProgramID, var);
        use();
        glUniform1iv(varLocate, array);
    }

    public void dispose() {
        if (compiled && shaderProgramID != 0) {
            glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
            compiled = false;
            inUsed = false;
        }
        ResourceStatusCallback.emit(RID, ResourceStatus.DISPOSED);
        RID.release();
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
        return String.format("Shader{source='%s'}", getFilePath());
    }

    private void assignSection(String type, String content) {
        switch (type) {
            case "vertex" -> vertexSrc = content;
            case "fragment" -> fragmentSrc = content;
            default -> {
                Logger.error(String.format("Shader '%s' contains unknown #type '%s'", getFilePath(), type));
                failed = true;
                ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
            }
        }
    }
}
