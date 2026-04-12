package utility;

import java.awt.*;

/**
 * FontPT is a static utility class providing conversion between screen pixels and typographic points.
 * This dynamically calculate the pixel ratio base on the current display DPI and {@link #PointPerInch} value.
 */
public final class FontPT {
    /**
     * Traditional typography measured 72 points per inch.
     */
    public static final float PointPerInch = 72.0f;
    private static int screenDPI;
    private static float pixelRatio;

    private FontPT() {}

    private static void calculateRatio() {
        if (pixelRatio != 0.0f && screenDPI != 0) return;
        int dpi = GraphicsEnvironment.isHeadless() ? 96 : Toolkit.getDefaultToolkit().getScreenResolution();
        screenDPI = dpi;
        pixelRatio = dpi / PointPerInch;
    }

    /**
     * Convert points to pixels.
     * @param point the amount of point
     * @return the equivalent pixel amount
     */
    public static int pointToPixel(float point) {
        calculateRatio();
        return Math.round(point * pixelRatio);
    }

    /**
     * Convert pixels to points.
     * @param pixel the amount of pixel
     * @return the equivalent point amount
     */
    public static float pixelToPoint(float pixel) {
        calculateRatio();
        return pixel / pixelRatio;
    }
}
