package physic2d;

import java.util.List;

public class PhysicLayer {
    public static int layerToBit(int layerIndex) {
        return isLayerIndexValid(layerIndex) ? 1 << layerIndex : 1;
    }

    public static int layersToMask(int... layerIndices) {
        int mask = 0;
        for (int index : layerIndices) {
            if (!isLayerIndexValid(index)) continue;
            mask |= 1 << index;
        }

        return mask;
    }

    public static boolean isLayerInMask(int mask, int layerIndex) {
        if (!isLayerIndexValid(layerIndex)) return false;

        int layerBit = 1 << layerIndex;
        int val = mask & layerBit;

        return val != 0;
    }

    public static int addLayerToMask(int mask, int layerIndex) {
        if (!isLayerIndexValid(layerIndex)) return mask;

        int layerBit = 1 << layerIndex;
        return mask | layerBit;
    }

    public static int removeLayerFromMask(int mask, int layerIndex) {
        if (!isLayerIndexValid(layerIndex)) return mask;

        int layerBit = 1 << layerIndex;
        return mask & ~layerBit;
    }

    public static int toggleLayerInMask(int mask, int layerIndex) {
        if (!isLayerIndexValid(layerIndex)) return mask;

        int layerBit = 1 << layerIndex;
        return mask ^ layerBit;
    }

    public static int getFullMask() {
        return 0xFFFF;
    }

    public static int getEmptyMask() {
        return 0;
    }

    public static boolean canCollide(int layerMaskA, int collisionMaskA, int layerMaskB, int collisionMaskB) {
        int AExistForB = layerMaskA & collisionMaskB;
        int BExistForA = layerMaskB & collisionMaskA;

        return AExistForB != 0 && BExistForA != 0;
    }

    public static int[] maskToLayerIndices(int mask) {
        int count = Integer.bitCount(mask);
        int[] indices = new int[count];
        int index = 0;
        int layerBit, val;
        for (int i = 0; i < Physic2D.MaxLayer && index < count; i++) {
            layerBit = 1 << i;
            val = mask & layerBit;
            if (val != 0) indices[index++] = i;
        }

        return indices;
    }

    public static boolean isLayerIndexValid(int index) {
        return index >= 0 && index < Physic2D.MaxLayer;
    }

    public static boolean isMaskValid(int mask) {
        return mask >= getEmptyMask() && mask <= getFullMask();
    }
}
