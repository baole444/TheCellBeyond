package editor;

import render.texture.Sprite;
import render.texture.SpriteSheet;
import utility.AssetsPool;
import utility.Settings;

/**
 * EditorIcons is a collections of icon sprites used by the Editor UI.
 */
public final class EditorIcons {
    private static final String Path = Settings.TexturePath.EditorControls;
    private static final int width = 28;
    private static final int height = 28;
    private static final int iconCount = 24;
    private static SpriteSheet icons;
    private static boolean isInitialized = false;

    /**
     * The EditorIconSprite interface provide common method {@link #getIcon()},
     * of which can be implemented to return specific sprite.
     */
    public interface EditorIconSprite {
        /**
         * Get the sprite of an editor icon.
         * @return the sprite or null if there is none
         */
        Sprite getIcon();
    }

    /**
     * Common icons of Editor UI.
     */
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

    /**
     * Icons used by {@link SpriteFrameEditor}.
     */
    public enum SpriteFrameIcons implements EditorIconSprite {
        /**
         * Left pointing triangle.
         */
        PlayBackward(12),
        /**
         * Square with rounded edge.
         */
        Stop(13),
        /**
         * Right pointing triangle.
         */
        Play(14),
        /**
         * Two vertical bar.
         */
        Pause(15),
        /**
         * Left triangle with vertical bar behind it.
         */
        PreviousFrame(16),
        /**
         * Right pointing triangle with vertical bar behind it.
         */
        NextFrame(17),
        /**
         * Left pointing arrow.
         */
        MoveFrameLeft(18),
        /**
         * Right pointing arrow.
         */
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

    /**
     * Icons used by {@link ConsoleOutput}.
     */
    public enum LogLevelIcons implements EditorIconSprite {
        /**
         * Swooned bug.
         */
        Debug(20),
        /**
         * "i" in white circle.
         */
        Info(21),
        /**
         * Exclamation mark in inverted yellow triangle.
         */
        Warning(22),
        /**
         * "X" in red circle.
         */
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

    private EditorIcons() {}

    /**
     * Initialize and load editor icons.
     */
    public static void init() {
        if (isInitialized) return;
        try {
            if (!AssetsPool.hasSpriteSheet(Path)) {
                AssetsPool.addSpriteSheet(Path,
                        new SpriteSheet(AssetsPool.loadTexture(Path), width, height, iconCount, 0)
                );
            }
            icons = AssetsPool.getSpriteSheet(Path);
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize editor icons: " + e.getMessage());
        }
    }
}
