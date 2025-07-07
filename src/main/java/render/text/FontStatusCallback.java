package render.text;

public interface FontStatusCallback {
    void onFontReady(TCBFont loadedFont, FontRequest request);
}
