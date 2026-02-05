package editor;

import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;

public final class EditorIcons {
    private static final String PATH = Settings.TexturePath.EditorControls;
    private static final int width = 28;
    private static final int height = 28;
    private static final int iconCount = 24;
    private static SpriteSheet icons;
    private static boolean isInitialized = false;

    public interface EditorIconSprite {
        Sprite getIcon();
    }

    public enum Icons implements EditorIconSprite {
        /**
         * File with plush sign.
         */
        New(0),

        /**
         * Two overlap files.
         */
        Copy(1),

        /**
         * File with pen writing on it.
         */
        Edit(2),

        /**
         * Trashcan.
         */
        Delete(3),

        /**
         * Minus sign
         */
        Remove(4),

        /**
         * Plus sign.
         */
        Add(5),

        /**
         * Writing Pen.
         */
        EditPen(6),

        /**
         * Circle arrow.
         */
        Reset(7),

        /**
         * Folder.
         */
        Open(8),

        /**
         * Mouse pointer.
         */
        Select(9),

        /**
         * Magnifier.
         */
        Search(10),

        /**
         * Eraser.
         */
        Eraser(11);

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
        PlayBackward(12),
        Stop(13),
        Play(14),
        Pause(15),
        PreviousFrame(16),
        NextFrame(17),
        MoveFrameLeft(18),
        MoveFrameRight(19);

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
        Debug(20),
        Info(21),
        Warning(22),
        Error(23);

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
