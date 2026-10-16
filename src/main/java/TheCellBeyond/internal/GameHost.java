package TheCellBeyond.internal;

import TheCellBeyond.Window;
import project.Project;
import scene.Scene;
import utility.log.EngineLog;

/**
 * The GameHost drive the engine as a shipped game, excluding all editor's UI and tooling.
 */
public final class GameHost implements EngineHost {
    private static final EngineLog Logger = new EngineLog(GameHost.class);
    public final String projectPath;

    /**
     * Create a new {@link GameHost} for the project at the given path.
     * @param projectPath the path to the project YAML file
     */
    public GameHost(String projectPath) {
        this.projectPath = projectPath;
    }

    @Override
    public void boot() {
        Logger.info("Loading project file at " + projectPath);
        Project.loadFromYaml(projectPath);
        if (!Project.loaded()) return;
        Project.loadProjectData();
    }

    @Override
    public void frame(float dt, Scene scene) {
        Window.getFrameBuffer().renderToScreen();
    }
}
