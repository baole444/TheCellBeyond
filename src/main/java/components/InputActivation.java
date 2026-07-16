package components;

import scripting.API;

/**
 * InputActivation evaluate whether an input press signal meets activate conditions base on {@link ActivationMode}.
 * It tracks internal state like hold progress and tap consumption across calls.
 * Upon releasing the input, all states are reset.
 */
@API
public class InputActivation {
    /**
     * Input activation mode, support for immediate, hold and tap activation.
     */
    public enum ActivationMode {
        /**
         * Input is activated as long as it is pressed.
         */
        Immediate,
        /**
         * Input is activated after the required hold time is meet.
         * Interruption while holding will reset the activation process.
         */
        Hold,
        /**
         * Input is activated within the frame when it was pressed.
         */
        Tap
    }

    private ActivationMode activationMode = ActivationMode.Immediate;
    private float requiredHoldTime = 0.0f;
    private transient float currentHoldTime = 0.0f;
    private transient boolean inputConsumed = false;

    /**
     * Create a new {@link InputActivation} with default {@link ActivationMode#Immediate} mode.
     */
    public InputActivation() {}

    /**
     * Create a new {@link InputActivation} with the given mode.
     * If the given mode is null, it will default to immediate mode.
     * @param mode the mode to operate with
     */
    public InputActivation(ActivationMode mode) {
        if (mode == null) mode = ActivationMode.Immediate;
        activationMode = mode;
    }

    /**
     * Create a new {@link InputActivation} from the given input activation.
     * @param inputActivation the input activation to copy from.
     */
    public InputActivation(InputActivation inputActivation) {
        activationMode = inputActivation.activationMode == null ? ActivationMode.Immediate : inputActivation.activationMode;
        requiredHoldTime = inputActivation.requiredHoldTime;
    }

    /**
     * Get the current activation mode of this input activation.
     * @return the current operating mode
     */
    public ActivationMode activationMode() {
        return activationMode;
    }

    /**
     * Set the activation mode for this input activation.
     * <p>
     * This will trigger reset if changed to a different mode.
     * @param mode the mode to operate with
     */
    public void activationMode(ActivationMode mode) {
        if (mode == null) return;
        if (activationMode != mode) reset();
        activationMode = mode;
    }

    /**
     * Get the time that is required to activate the input.
     * @return required hold time, in seconds
     */
    public float requiredHoldTime() {
        return requiredHoldTime;
    }

    /**
     * Set the required time to hold the input before it is activated.
     * @param holdTime the time to hold, in seconds
     */
    public void requiredHoldTime(float holdTime) {
        requiredHoldTime = Math.max(0.0f, holdTime);
    }

    /**
     * Check if the input met activation conditions or not.
     * @param isPressed the press state of the input
     * @param dt delta time
     * @return true if the conditions are met
     */
    public boolean isActivated(boolean isPressed, float dt) {
        if (!isPressed) {
            reset();
            return false;
        }
        switch (activationMode) {
            case Immediate -> {
                return true;
            }
            case Hold -> {
                currentHoldTime += dt;
                return currentHoldTime >= requiredHoldTime;
            }
            case Tap -> {
                if (inputConsumed) return false;
                inputConsumed = true;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Get the progress until input activation (0.0 -> 1.0).
     * <p>
     * Immediate activation mode will return 1.0 on pressed
     * @param isPressed the press state of the input
     * @return progress value normalized to range of 0.0 and 1.0
     */
    public float activationProgress(boolean isPressed) {
        if (!isPressed) return 0.0f;
        switch (activationMode) {
            case Immediate -> {
                return 1.0f;
            }
            case Hold -> {
                if (requiredHoldTime <= 0.0f) return 1.0f;
                return Math.min(1.0f, currentHoldTime / requiredHoldTime);
            }
            case Tap -> {
                return inputConsumed ? 0.0f : 1.0f;
            }
            default -> {
                return 0.0f;
            }
        }
    }

    /**
     * Reset tracking state for activation conditions.
     */
    public void reset() {
        currentHoldTime = 0.0f;
        inputConsumed = false;
    }
}
