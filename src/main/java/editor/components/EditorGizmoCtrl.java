package editor.components;

import components.Component;
import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;

/**
 * EditorGizmoCtrl manage {@link EditorGizmo}'s appearance and how the gizmo function.
 */
public class EditorGizmoCtrl extends Component {
    private static final String PATH = Settings.TexturePath.Gizmo;
    private static EditorGizmoType gizmoType = EditorGizmoType.Translate;

    private transient SpriteSheet gizmo;
    private transient boolean isInitialized = false;

    public static EditorGizmoType getGizmoType() {
        return gizmoType;
    }

    public static void setGizmoType(EditorGizmoType gizmoType) {
        if (gizmoType == null) return;
        EditorGizmoCtrl.gizmoType = gizmoType;
    }

    @Override
    protected void onEditorStartLogic() {
        initGizmoSprite();
    }

    private void initGizmoSprite() {
        if (isInitialized || gameObject == null) return;

        try {
            if (!AssetsPool.hasSpriteSheet(PATH)) {
                int w = 16;
                int h = 48;
                int count = 3;
                AssetsPool.addSpriteSheet(PATH,
                        new SpriteSheet(AssetsPool.loadTexture(PATH), w, h, count, 0)
                );
            }

            gizmo = AssetsPool.loadSpriteSheet(PATH);
            completeInit();
        } catch (Exception e) {
            System.err.println("Failed to initialize EditorGizmo texture: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (gizmo == null) return;

        Sprite gizmoMove = gizmo.spriteIndex(1);
        Sprite gizmoScale = gizmo.spriteIndex(2);

        if (gizmoMove == null || gizmoScale == null) return;

        gameObject.addComponent(new EditorGizmoMove(gizmoMove));
        gameObject.addComponent(new EditorGizmoScale(gizmoScale));

        isInitialized = true;
    }

    @Override
    public void onEditorUpdate(float dt) {
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
