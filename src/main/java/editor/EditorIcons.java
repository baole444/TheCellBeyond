package editor;

import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;

public class EditorIcons {
    private static final String PATH = Settings.TexturePath.EditorControls;
    private static final int width = 28;
    private static final int height = 28;
    private static final int iconCount = 20;
    private static SpriteSheet icons;
    private static boolean isInitialized = false;

    public interface EditorIconSprite {
        Sprite getIcon();
    }

    public enum Icons implements EditorIconSprite {
        New(0),
        Copy(1),
        Edit(2),
        Delete(3),
        Open(4),
        Select(5),
        Search(6),
        Eraser(7);

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
        PlayBackward(8),
        Stop(9),
        Play(10),
        Pause(11),
        PreviousFrame(12),
        NextFrame(13),
        MoveFrameLeft(14),
        MoveFrameRight(15);

        final int index;

        SpriteFrameIcons(int index) {
            this.index = index;
        }

        @Override
        public Sprite getIcon() {
            return icons == null ? null : icons.spriteIndex(index);
        }
    }

    public enum LogLevelIcons implements EditorIconSprite {
        Debug(16),
        Info(17),
        Warning(18),
        Error(19);

        final int index;

        LogLevelIcons(int index) {
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
