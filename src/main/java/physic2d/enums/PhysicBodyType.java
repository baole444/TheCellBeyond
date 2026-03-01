package physic2d.enums;

/**
 * Types of physic body.
 */
public enum PhysicBodyType {
    /**
     * Static body that cannot be moved.
     */
    Static,
    /**
     * Body with full physic simulation.
     */
    Dynamic,
    /**
     * Body that can only be moved by velocity and affect other physic body on its way.
     */
    Kinematic
}
