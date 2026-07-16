package physic2d.enums;

import scripting.API;

/**
 * Types of physic body.
 */
@API
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
