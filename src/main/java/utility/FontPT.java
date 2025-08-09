package utility;

import java.awt.*;

public class FontPT {
    // Traditional typography point size
    private static final float PT_PER_INCH = 72.0f;

    private static int screenDPI;

    private static float pixelRatio;

    private static void calculateRatio() {
        if (pixelRatio != 0.0f && screenDPI != 0) return;

        int dpi;

        if (!GraphicsEnvironment.isHeadless()) {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        } else {
            dpi = 96; // common dpi value
        }

        screenDPI = dpi;

        pixelRatio = dpi / PT_PER_INCH;
    }

    public static int pointToPixel(float point) {
        calculateRatio();
        return Math.round(point * pixelRatio);
    }

    public static float pixelToPoint(float pixel) {
        calculateRatio();
        return pixel / pixelRatio;
    }
}
