package scene;

import project.Project;

public class SceneRuntime extends SceneLoader {
    @Override
    public void loadResource(Scene scene) {
        Project.loadProjectData();
    }

    @Override
    public void onSceneStarted(Scene scene) {}

    @Override
    public void onSceneEnd() {}
}
