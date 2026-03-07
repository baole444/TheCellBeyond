package editor.components;

import components.Component;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetManager;
import utility.Settings;

/**
 * EditorGizmoCtrl manage {@link EditorGizmo}'s appearance and how the gizmo function.
 */
public class EditorGizmoCtrl extends Component {
    private static final String Path = Settings.TexturePath.Gizmo;
    private static EditorGizmoType gizmoType = EditorGizmoType.Translate;
    private transient SpriteSheet gizmo;
    private transient boolean isInitialized = false;

    /**
     * Create a new {@link EditorGizmoCtrl} component.
     */
    public EditorGizmoCtrl() {
        String name = EditorGizmoCtrl.class.getSimpleName();
        super(name);
    }

    /**
     * Get the current type of Gizmo.
     * @return the Gizmo type
     */
    public static EditorGizmoType getGizmoType() {
        return gizmoType;
    }

    /**
     * Set the current type of Gizmo.
     * @param gizmoType the gizmo type to switch to
     */
    public static void setGizmoType(EditorGizmoType gizmoType) {
        if (gizmoType == null) return;
        EditorGizmoCtrl.gizmoType = gizmoType;
    }

    @Override
    protected void onEditorStart() {
        initGizmoSprite();
    }

    private void initGizmoSprite() {
        if (isInitialized || gameObject == null) return;
        try {
            AssetManager manager = AssetManager.get();
            if (!manager.hasSpriteSheet(Path)) {
                int w = 16;
                int h = 48;
                int count = 3;
                manager.addSpriteSheet(Path,
                        new SpriteSheet(manager.getTexture(manager.loadTexture(Path)), w, h, count, 0)
                );
            }
            gizmo = manager.getSpriteSheet(Path);
            completeInit();
        } catch (Exception e) {
            System.err.println("Failed to initialize EditorGizmo texture: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (gizmo == null || gameObject == null) return;
        Sprite gizmoMove = gizmo.spriteIndex(1);
        Sprite gizmoScale = gizmo.spriteIndex(2);
        if (gizmoMove == null || gizmoScale == null) return;
        gameObject.addComponent(new EditorGizmoMove(gizmoMove));
        gameObject.addComponent(new EditorGizmoScale(gizmoScale));
        isInitialized = true;
    }

    @Override
    public void onEditorUpdate(float dt) {
        if (gameObject == null) return;
        if (gizmo == null) {
            initGizmoSprite();
            return;
        }
        if (!isInitialized) {
            completeInit();
            return;
        }
        if (gizmoType == EditorGizmoType.Translate) {
            gameObject.getFirstComponent(EditorGizmoMove.class).use();
            gameObject.getFirstComponent(EditorGizmoScale.class).stopUse();
            return;
        }
        if (gizmoType == EditorGizmoType.Scale) {
            gameObject.getFirstComponent(EditorGizmoMove.class).stopUse();
            gameObject.getFirstComponent(EditorGizmoScale.class).use();
        }
    }
}
