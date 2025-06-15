package render.texture;

interface TextureStatusListener {
    void onTextureStatusChange(int handleId, TextureHandle.Status status);
}
