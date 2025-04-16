package utility;

import org.joml.Vector3f;

public class ColorConverter {
    /**
     * Convert vector3 representation of the color to hexadecimal int.
     * @param rgbVector vector representation of the color to convert.
     * @return integer of the color in hexadecimal form.
     */
    public static int fromVectorToHexInt(Vector3f rgbVector) {
        float colorRange;

        float[] in = {rgbVector.x, rgbVector.y, rgbVector.z};

        int[] out = {0, 0, 0};

        // Assume they use 0-255 scale.
        if (rgbVector.x > 1f || rgbVector.y > 1f || rgbVector.z > 1f)
            colorRange = 255f;

        // Assume they use 0-1 scale.
        else colorRange = 1f;

        for (int i = 0; i < 3; i++) {
            if (colorRange == 255f) {
                out[i] = (int) Math.min(colorRange, Math.max(0, in[i]));
            } else {
                out[i] = (int) (Math.min(colorRange, Math.max(0, in[i])) * 255);
            }
        }

        return (out[0] << 16) | (out[1] << 8) | out[2];
    }

    /**
     * Convert hexadecimal integer representation of the color to vector3.
     * @param rgb integer of the color in hexadecimal form.
     * @param clam_to_scale_zero_one set to true if the output vector value should be in scale of 0f -> 1f.
     * @return vector3 representation of the color.
     */
    public static Vector3f fromHexIntToVector(int rgb, boolean clam_to_scale_zero_one) {
        float clamValue = 255f;

        if (!clam_to_scale_zero_one) clamValue = 1f;

        float r = (float) ((rgb >> 16) & 0xFF) / clamValue;
        float g = (float) ((rgb >> 8) & 0xFF) / clamValue;
        float b = (float) ((rgb) & 0xFF) / clamValue;

        return new Vector3f(r, g, b);
    }
}
