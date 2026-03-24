package signal;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Strategy to skill field of type {@link Signal}, reduce the needs for user to mark signal as transient.
 */
public class SignalExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return Signal.class.isAssignableFrom(f.getDeclaredClass());
    }

    @Override
    public boolean shouldSkipClass(Class<?> c) {
        return false;
    }
}
