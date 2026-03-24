package signal;

import TheCellBeyond.internal.LogicServer;
import scene.Scene;
import utility.log.EngineLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Callable represent an instance method that can be invoked with arbitrary arguments.
 * The target method should have void return type.
 * <p>
 * Method resolution happened at construction time. If resolved with exceptions, an invalid Callable is created,
 * all call to this invalid callable will be skipped.
 * </p>
 * This is mainly use with {@link Signal} to connect handler methods to signals.
 * {@snippet lang = "java":
 * public Player extends CharacterBody2D {
 *     private int health = 20;
 *
 *     @Override
 *     protected void onStart() {
 *         Callable callable = Callable.get(this, "onHit");
 *         // Other logic
 *     }
 *
 *     public void onHit(int damage) {
 *         health -= Math.abs(damage);
 *     }
 * }
 * }
 */
public class Callable {
    private static final EngineLog Logger = new EngineLog(Callable.class);
    private static final WeakHashMap<Object, Map<String, Callable>> cache = new WeakHashMap<>();
    private final Object target;
    private final String methodName;
    private final Method method;
    private final boolean valid;

    private Callable(Object target, String methodName, Method method) {
        this.target = target;
        this.methodName = methodName;
        this.method = method;
        valid = method != null;
        if (valid) this.method.setAccessible(true);
    }

    /**
     * Check if this callable has bound to a method or not.
     * Only valid callable can invoke the target method.
     * @return true if bound to target method
     */
    public boolean valid() {
        return valid;
    }

    /**
     * Invoke the bound method with the given arguments if this callable is valid.
     * The order of the args must match the method's declared order.
     * @param args the parameters to pass to the method
     * @see #deferredCall(Object...) call with deferring
     */
    public void call(Object... args) {
        if (!valid) return;
        try {
            method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Logger.warning(String.format("Exception in callable '%s' on '%s': %s", methodName, target.getClass().getSimpleName(), e.getCause()));
        } catch (IllegalArgumentException e) {
            Logger.warning(String.format("Argument mismatch when calling '%s' on '%s': %s", methodName, target.getClass().getSimpleName(), e.getMessage()));
        } catch (IllegalAccessException e) {
            Logger.warning(String.format("Cannot access '%s' on '%s': %s", methodName, target.getClass().getSimpleName(), e.getMessage()));
        }
    }

    /**
     * Queue this callable to be invoked at the end of the current frame during idle process (logic update.)
     * If there is no active scene, this will invoke immediately.
     * @param args the parameters to pass to the method
     */
    public void deferredCall(Object... args) {
        if (!valid) return;
        Scene scene = LogicServer.currentScene();
        if (scene == null) {
            Logger.warning("No active scene for deferred call, invoking immediately");
            call(args);
            return;
        }
        scene.queueDeferredCallable(this, args);
    }

    /**
     * Get the target instance that this callable is bound to.
     * @return the target instance, or null if there is none
     */
    public Object target() {
        return target;
    }

    /**
     * Get the name of the method this callable is bound to.
     * @return the method name, blank or empty if method resolving failed or there is none
     */
    public  String methodName() {
        return methodName;
    }

    /**
     * Get from cache or create a new {@link Callable} that bind to the specified method on the target instance.
     * If the target is null or the specified method cannot be found,
     * The return callable will be mark as invalid and not cached.
     * @param target the object that owns the method
     * @param methodName the name of the method to bind
     * @return a new {@link Callable} bound to the target method
     * @apiNote
     * The caller is responsible for method name uniqueness,
     * this will bind to the first encounter of the method with the same name.
     */
    public static Callable get(Object target, String methodName) {
        if (target == null) {
            Logger.warning(String.format("Cannot create callable '%s': target is null", methodName));
            return new Callable(null, methodName != null ? methodName : "", null);
        }
        if (methodName == null || methodName.isBlank()) {
            Logger.warning(String.format("Cannot create callable on '%s': method name is null or blank", target.getClass().getSimpleName()));
            return new Callable(target, "", null);
        }
        Map<String, Callable> methods = cache.get(target);
        if (methods != null) {
            Callable cached = methods.get(methodName);
            if (cached != null) return cached;
        }
        Method resolved = resolveMethod(target, methodName);
        if (resolved == null) {
            Logger.warning(String.format("Cannot create callable on '%s' for '%s': method not found", target.getClass().getSimpleName(), methodName));
            return new Callable(target, methodName, null);
        }
        Callable callable = new Callable(target, methodName, resolved);
        cache.computeIfAbsent(target, _ -> new HashMap<>()).put(methodName, callable);
        return callable;
    }

    private static Method resolveMethod(Object target, String methodName) {
        Class<?> c = target.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof  Callable callable)) return false;
        return target == callable.target && Objects.equals(methodName, callable.methodName);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(target) * 31 + methodName.hashCode();
    }
}
