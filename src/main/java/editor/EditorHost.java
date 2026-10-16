package editor;

import TheCellBeyond.internal.EngineHost;
import editor.dialogs.ExitConfirmDialog;
import editor.log.LogFileWriter;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import scene.Scene;
import scene.SceneLoader;

import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Init;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit;

/**
 * The EditorHost drive the engine with addition of editor's UI and tooling, used for developing scene and control play test.
 */
public final class EditorHost implements EngineHost {
    private static final String GlslVersion = "#version 330 core";
    public final String projectPath;
    private EditorLayer editorLayer;

    /**
     * Create a new {@link EditorHost} for the project at the given path.
     * @param projectPath the path to the project YAML file
     */
    public EditorHost(String projectPath) {
        this.projectPath = projectPath;
    }

    @Override
    public void initialize(long windowPtr) {
        NFD_Init();
        editorLayer = new EditorLayer(windowPtr);
        editorLayer.init(GlslVersion);
    }

    @Override
    public void boot() {
        EngineEventCallback.emit(projectPath, new EditorEvent(EditorEvent.Type.LoadProjectFromDisk));
    }

    @Override
    public void frame(float dt, Scene scene) {
        editorLayer.update(dt, scene);
    }

    @Override
    public boolean closeRequested() {
        return ExitConfirmDialog.exitDialog();
    }

    @Override
    public void dispose() {
        editorLayer.dispose();
        NFD_Quit();
        LogFileWriter.stop();
    }

    @Override
    public SceneLoader editSceneLoader() {
        return new SceneEditor();
    }
}
