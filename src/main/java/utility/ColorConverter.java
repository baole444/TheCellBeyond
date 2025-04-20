package utility;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class ColorConverter {
    /**
     * Convert vector3 representation of the color to hexadecimal int.
     * @param rgbVector vector representation of the color to convert.
     * @return integer of the color in hexadecimal form.
     */
    public static int fromVector3ToHexInt(Vector3f rgbVector) {
        float colorRange;

        float[] in = {rgbVector.x, rgbVector.y, rgbVector.z};

        int[] out = {0, 0, 0};

        // Assume they use 0-255 scale.
        if (rgbVector.x > 1f || rgbVector.y > 1f || rgbVector.z > 1f)
            colorRange = 255f;

        // Assume they use 0-1 scale.
        else colorRange = 1f;

        for (int i = 0; i < 3; i++) {
            float v = Math.min(colorRange, Math.max(0, in[i]));
            if (colorRange == 255f) {
                out[i] = (int) v;
            } else {
                out[i] = (int) (v * 255);
            }
        }

        return (out[0] << 16) | (out[1] << 8) | out[2];
    }

    /**
     * Convert vector4 representation of the color to hexadecimal int.
     * @param rgbaVector vector representation of the color to convert.
     * @return integer of the color in hexadecimal form.
     */
    public static int fromVector4ToHexInt(Vector4f rgbaVector) {
        float colorRange;

        float[] in = {rgbaVector.x, rgbaVector.y, rgbaVector.z, rgbaVector.w};

        int[] out = {0, 0, 0, 0};

        // Assume they use 0-255 scale.
        if (rgbaVector.x > 1f || rgbaVector.y > 1f || rgbaVector.z > 1f || rgbaVector.w > 1f)
            colorRange = 255f;

            // Assume they use 0-1 scale.
        else colorRange = 1f;

        for (int i = 0; i < 4; i++) {
            float v = Math.min(colorRange, Math.max(0, in[i]));
            if (colorRange == 255f) {
                out[i] = (int) v;
            } else {
                out[i] = (int) (v * 255);
            }
        }

        return (out[0] << 16) | (out[1] << 8) | out[2] | out[3] << 24;
    }

    /**
     * Convert hexadecimal integer representation of the color to vector3.
     * @param rgb integer of the color in hexadecimal form.
     * @param clam_to_scale_zero_one set to true if the output vector value should be in scale of 0f -> 1f.
     * @return vector3 representation of the color.
     */
    public static Vector3f fromHexIntToVector3(int rgb, boolean clam_to_scale_zero_one) {
        float clamValue = 255f;

        if (!clam_to_scale_zero_one) clamValue = 1f;

        float r = (float) ((rgb >> 16) & 0xFF) / clamValue;
        float g = (float) ((rgb >> 8) & 0xFF) / clamValue;
        float b = (float) ((rgb) & 0xFF) / clamValue;

        return new Vector3f(r, g, b);
    }

    /**
     * Convert hexadecimal integer representation of the color to vector4.
     * @param rgba integer of the color in hexadecimal form.
     * @param clam_to_scale_zero_one set to true if the output vector value should be in scale of 0f -> 1f.
     * @return vector4 representation of the color.
     */
    public static Vector4f fromHexIntToVector4(int rgba, boolean clam_to_scale_zero_one) {
        float clamValue = 255f;

        if (!clam_to_scale_zero_one) clamValue = 1f;

        float r = (float) ((rgba >> 16) & 0xFF) / clamValue;
        float g = (float) ((rgba >> 8) & 0xFF) / clamValue;
        float b = (float) ((rgba) & 0xFF) / clamValue;
        float a = (float) ((rgba >> 24) & 0xFF) / clamValue;

        return new Vector4f(r, g, b, a);
    }
}
