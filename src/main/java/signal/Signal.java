package signal;

import utility.log.EngineLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Signal is a data type represents a signal on a {@link TheCellBeyond.GameObject} or {@link components.Component} instance.
 * Signal can be store as variable and pass to functions. Signals are excluded from serialization by default.
 * <p>
 * Signal allow all connected {@link Callable}s and by extension, the objects that own them,
 * to listen and react to events, without referencing one another.
 * </p>
 * Signals are often declared as {@code public final} on the emitter object, with the signal's constructor accept
 * type parameters. Pass no type for signals that don't require data on emit.
 * The declared type(s) will be used to define the contract of the signal and validate on emit.
 * <p>
 * On emitter side, where the signal originated from:
 * {@snippet lang = "java":
 * public class Weapon extends Area2D {
 *     // Declare weapon cooldown signal
 *     public final Signal weaponCooldown = new Signal(Float.class);
 *     public float cooldownSeconds = 2.0f
 *
 *     // Do attack logic then emit the signal when attack finished to inform weapon is on cooldown
 *     public void attack() {
 *         weaponCooldown.emit(coolDownSeconds);
 *     }
 * }
 * }
 * </p>
 * On listener side, where the signal need to be received and react to:
 * {@snippet lang = "java":
 * public class Player extends CharacterBoby2D {
 *     public final String weaponPath = "./Weapon";
 *
 *     @Override
 *     public void onReady() {
 *         // Get the weapon object via relative hierarchy path, can be replaced with instanceof check if needed.
 *         Weapon weapon = (Weapon) HierarchyPaths.toGameObject(weaponPath, this);
 *         if (weapon != null) weapon.weaponCooldown.connect(Callable.get(this, "onWeaponCooldown"));
 *     }
 *
 *     public void onWeaponCooldown(float cooldownTime) {
 *         // Handle cooldown logic
 *     }
 * }
 * }
 */
public class Signal {
    private static final EngineLog Logger = new EngineLog(Signal.class);
    private final Class<?>[] types;
    private final List<Callable> callables = new CopyOnWriteArrayList<>();

    /**
     * Create a new {@link Signal} with the given parameter types.
     * An empty parameter list will define a signal with no argument.
     * <p>
     * When {@link #emit(Object...)} is called, the args are validated against the types.
     * @param types the expected parameter types
     */
    public Signal(Class<?>... types) {
        this.types = types;
    }

    /**
     * Connect a {@link Callable} to this signal, which will be invoked when this signal emits.
     * If the callable is null, invalid or already existed, connection is rejected.
     * @param callable the callable to connect with this signal
     */
    public void connect(Callable callable) {
        if (callable == null || !callable.valid()) return;
        if (callables.contains(callable)) return;
        callables.add(callable);
    }

    /**
     * Disconnect a {@link Callable} from this signal.
     * @param callable the callable to disconnect
     */
    public void disconnect(Callable callable) {
        callables.remove(callable);
    }

    /**
     * Remove all connection from this signal.
     */
    public void disconnectAll() {
        callables.clear();
    }

    /**
     * Emit this signal, invoking all connected callables with the given args.
     * <p>
     * The arguments passed to this method will be validated against the declared types during the signal construction.
     * If there is a mismatch in type or number of arguments, no callable will be invoked.
     * @param args the arguments to emit to connected callables
     */
    public void emit(Object... args) {
        if (invalidArgs(args)) return;
        callables.forEach(c -> c.call(args));
    }

    private boolean invalidArgs(Object[] args) {
        if (args.length != types.length) {
            Logger.warning(String.format("Signal emit expected %d argument(s), got %d instead", types.length, args.length));
            return true;
        }
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            if (types[i].isInstance(args[i])) continue;
            Logger.warning(String.format("Signal emit type mismatch at arg %d: expected %s but found %s", i, types[i].getSimpleName(), args[i].getClass().getSimpleName()));
            return true;
        }
        return false;
    }
}
