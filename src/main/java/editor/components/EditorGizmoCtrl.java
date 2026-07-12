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
    private static EditorGizmoMode gizmoType = EditorGizmoMode.Move;
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
     * Switch gizmo control to scale mode.
     */
    public static void useScaleGizmo() {
        gizmoType = EditorGizmoMode.Scale;
    }

    /**
     * Switch gizmo control to move mode.
     */
    public static void useMoveGizmo() {
        gizmoType = EditorGizmoMode.Move;
    }

    /**
     * Check if gizmo control is in scale mode.
     * @return true if is in scale mode
     */
    public static boolean inScaleMode() {
        return gizmoType == EditorGizmoMode.Scale;
    }

    /**
     * Check if gizmo control is in move mode.
     * @return true if is in move mode
     */
    public static boolean inMoveMode() {
        return gizmoType == EditorGizmoMode.Move;
    }

    @Override
    protected void internalEditorStart() {
        initGizmoSprite();
    }

    private void initGizmoSprite() {
        if (isInitialized || gameObject == null) return;
        try {
            if (!AssetManager.hasSpriteSheet(Path)) {
                int w = 16;
                int h = 48;
                int count = 3;
                AssetManager.addSpriteSheet(Path,
                        new SpriteSheet(AssetManager.getTexture(AssetManager.loadTexture(Path)), w, h, count, 0)
                );
            }
            gizmo = AssetManager.getSpriteSheet(Path);
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
    public void internalEditorUpdate(float dt) {
        if (gameObject == null) return;
        if (gizmo == null) {
            initGizmoSprite();
            return;
        }
        if (!isInitialized) {
            completeInit();
            return;
        }
        if (gizmoType == EditorGizmoMode.Move) {
            gameObject.getFirstComponent(EditorGizmoMove.class).use();
            gameObject.getFirstComponent(EditorGizmoScale.class).stopUse();
            return;
        }
        if (gizmoType == EditorGizmoMode.Scale) {
            gameObject.getFirstComponent(EditorGizmoMove.class).stopUse();
            gameObject.getFirstComponent(EditorGizmoScale.class).use();
        }
    }
}
