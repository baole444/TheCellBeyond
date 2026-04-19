package scene;

import TheCellBeyond.GameObject;
import TheCellBeyond.Transform2D;
import components.*;
import editor.EditorIcons;
import editor.components.*;
import editor.EditorWidget;
import editor.dialog.AddSpriteSheetDialog;
import editor.dialog.AddTextureUnitDialog;
import editor.payload.SpriteDragDropPayload;
import eventviewer.event.Event;
import imgui.flag.*;
import project.Project;
import project.ProjectAssetMap;
import project.ProjectData;
import project.ProjectSheetMap;
import eventviewer.EngineEventCallback;
import eventviewer.EngineEventListener;
import eventviewer.event.EditorEvent;
import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import render.texture.TextureUnit;
import utility.AssetManager;
import utility.UnifiedPaths;
import utility.TextureScale;
import utility.prefabrication.PrefabData;
import utility.prefabrication.PrefabManager;

import java.util.*;
import java.util.stream.Collectors;

public class SceneEditor extends SceneLoader {
    private static GameObject levelEditorObject;

    public SceneEditor() {}

    public static void pickUpObject(GameObject pickingObject) {
        if (levelEditorObject == null || levelEditorObject.isDestroyed() || pickingObject == null || pickingObject.isDestroyed()) return;
        levelEditorObject.getFirstComponent(EditorMouseCtrl.class).pickObject(pickingObject);
    }

    @Override
    public void onSceneStarted(Scene scene) {}

    @Override
    public void loadResource(Scene scene) {
        Project.loadProjectData();
        levelEditorObject = new GameObject("EditorObject");
        levelEditorObject.setNotSerialize();
        levelEditorObject.addComponents(new IsNotSelectable(), new Transform2D(),
                new EditorMouseCtrl(), new EditorKeyCtrl(), new EditorGrid(),
                new EditorTileMapGrid(), new EditorTileMapCtrl(),
                new EditorSceneCtrl(scene.viewport()), new EditorGizmoCtrl()
        );
        scene.queueForObjectAddition(levelEditorObject, null);
    }

    @Override
    public void onSceneEnd() {
        levelEditorObject = null;
    }
}
