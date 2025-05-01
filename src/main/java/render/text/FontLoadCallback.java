package render.text;

public interface FontLoadCallback {
    void onFontLoaded(TCBFont loadedFont, FontRequest request);
}
