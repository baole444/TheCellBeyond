package scripting.transpiler.manifest;

public final class SnakeCaseConverter {
    private static final char Separator = '_';
    private static final int SeparatorBuffer = 5;
    private SnakeCaseConverter() {}

    public static String toSnake(String origin) {
        if (origin == null || origin.isBlank()) return origin;
        StringBuilder result = new StringBuilder(origin.length() + SeparatorBuffer);
        result.append(Character.toLowerCase(origin.charAt(0)));
        for (int i = 1; i < origin.length(); i++) {
            char current = origin.charAt(i);
            if (atWordBoundary(origin, i)) result.append(Separator);
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

    private static boolean atWordBoundary(String text, int index) {
        char current = text.charAt(index);
        if (!Character.isUpperCase(current)) return false;
        char previous = text.charAt(index - 1);
        if (Character.isLowerCase(previous) || Character.isDigit(previous)) return true;
        boolean nextIsLowercase = index + 1 < text.length() && Character.isLowerCase(text.charAt(index + 1));
        return Character.isUpperCase(previous) && nextIsLowercase;
    }
}
