package editor;

import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;

public class EditorIcons {
    private static final String PATH = "engine://assets/textures/EditorControls.png";
    private static final int width = 28;
    private static final int height = 28;
    private static final int iconCount = 12;
    private static SpriteSheet icons;
    private static boolean isInitialized = false;

    public interface EditorIconSprite {
        Sprite getIcon();
    }

    public enum Icons implements EditorIconSprite {
        New(0),
        Copy(1),
        Edit(2),
        Delete(3);

        final int index;

        Icons(int index) {
            this.index = index;
        }

        @Override
        public Sprite getIcon() {
            return icons == null ? null : icons.spriteIndex(index);
        }
    }

    public enum SpriteFrameIcons implements EditorIconSprite {
        PlayBackward(4),
        Stop(5),
        Play(6),
        Pause(7),
        PreviousFrame(8),
        NextFrame(9),
        MoveFrameLeft(10),
        MoveFrameRight(11);

        final int index;

        SpriteFrameIcons(int index) {
            this.index = index;
        }

        @Override
        public Sprite getIcon() {
            return icons == null ? null : icons.spriteIndex(index);
        }
    }

    public static void init() {
        if (isInitialized) return;

        try {
            if (!AssetsPool.hasSpriteSheet(PATH)) {
                AssetsPool.addSpriteSheet(PATH,
                        new SpriteSheet(AssetsPool.loadTexture(PATH), width, height, iconCount, 0)
                );
            }

            icons = AssetsPool.loadSpriteSheet(PATH);
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize editor icons: " + e.getMessage());
        }
    }

    public boolean isIsInitialized() {
        return isInitialized;
    }
}
