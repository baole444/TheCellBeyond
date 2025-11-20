package project;

public record ProjectSheetMap(
        String path, int numberOfSprite,
        int spriteSizeX, int spriteSizeY,
        int spriteSpacingX, int spriteSpacingY,
        int spriteStartPosX, int spriteStartPosY
) {}
