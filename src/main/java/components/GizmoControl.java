package components;

import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;

/**
 * A class dedicated to handling Editor's gizmo system.
 * Handle gizmo's type and keybindings.
 */
public class GizmoControl extends Component {
    private static final String PATH = Settings.TexturePath.Gizmo;
    private transient SpriteSheet gizmo;
    private transient boolean isInitialized = false;
    private static int isGizUse = 0;

    @Override
    protected void additionalStartLogic() {
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
            System.err.println("Failed to initialize Gizmo texture: " + e.getMessage());
        }
    }

    private void completeInit() {
        if (gizmo == null) return;

        Sprite gizmoMove = gizmo.spriteIndex(1);
        Sprite gizmoScale = gizmo.spriteIndex(2);

        if (gizmoMove == null || gizmoScale == null) return;

        gameObject.addComponent(new GizmoMove(gizmoMove));
        gameObject.addComponent(new GizmoScale(gizmoScale));

        isInitialized = true;
    }

    public static int getIsGizUse() {
        return isGizUse;
    }

    public static void setIsGizUse(int val) {
        isGizUse = val;
    }

    @Override
    public void editorUpdate(float dt) {
        if (gizmo == null) {
            initGizmoSprite();
            return;
        }

        if (!isInitialized) {
            completeInit();
            return;
        }

        if (isGizUse == 0) {
            gameObject.getFirstComponent(GizmoMove.class).use();

            gameObject.getFirstComponent(GizmoScale.class).stopUse();

        } else if (isGizUse == 1) {
            gameObject.getFirstComponent(GizmoMove.class).stopUse();

            gameObject.getFirstComponent(GizmoScale.class).use();
        }
    }
}
