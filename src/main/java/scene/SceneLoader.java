package scene;

public abstract class SceneLoader {
    public abstract void onSceneEntered(Scene scene);
    public abstract void loadResource(Scene scene);
    public void imgui() {}
    public abstract void onSceneEnd();
}
