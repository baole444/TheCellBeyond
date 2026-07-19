package TheCellBeyond.internal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ResourceIDTest {
    private static final int Iterations = 1000;

    @Test
    public void idsStrictlyIncreaseAndNeverRepeat() {
        Set<Integer> seen = new HashSet<>();
        int previous = new ResourceID(ResourceType.Undefined).id;
        seen.add(previous);
        for (int i = 0; i < Iterations; i++) {
            ResourceID RID = new ResourceID(ResourceType.Undefined);
            assertTrue(RID.id > previous, "id " + RID.id + " did not increase past " + previous);
            assertTrue(seen.add(RID.id), "id " + RID.id + " was dispatched twice");
            previous = RID.id;
        }
    }

    @Test
    public void disposingAndReloadingNeverReissueAnId() {
        Set<Integer> seen = new HashSet<>();
        ResourceRegistry<Object> registry = new ResourceRegistry<>();
        for (int i = 0; i < Iterations; i++) {
            ResourceID RID = new ResourceID(ResourceType.Undefined);
            registry.register(RID, new Object());
            registry.unregister(RID);
            assertTrue(seen.add(RID.id), "id " + RID.id + " was reissued after its resource was unregistered");
        }
    }

    @Test
    public void staleRIDResolveToNullRatherThanTheNextResource() {
        ResourceRegistry<String> registry = new ResourceRegistry<>();
        ResourceID first = new ResourceID(ResourceType.Undefined);
        registry.register(first, "first");
        registry.unregister(first);
        ResourceID second = new ResourceID(ResourceType.Undefined);
        registry.register(second, "second");
        assertNull(registry.get(first), "a stale RID resolved to a resource it does not name");
        assertEquals("second", registry.get(second));
    }

    @Test
    public void distinctRIDsAreNotEqualAndHashDifferently() {
        ResourceID first = new ResourceID(ResourceType.Undefined);
        ResourceID second = new ResourceID(ResourceType.Undefined);
        assertNotEquals(first, second, "RIDs with different ids must not be equal");
        assertNotEquals(first.hashCode(), second.hashCode(), "an id-based hashCode should distinguish distinct ids");
    }

    @Test
    public void aDisposedRIDKeepsItsIdentityAsAMapKey() {
        ResourceRegistry<String> registry = new ResourceRegistry<>();
        Map<ResourceID, String> keyedByRID = new HashMap<>();
        ResourceID RID = new ResourceID(ResourceType.Undefined);
        registry.register(RID, "resource");
        keyedByRID.put(RID, "value");
        int hashBefore = RID.hashCode();
        registry.unregister(RID);
        assertEquals(hashBefore, RID.hashCode(), "a RID's hash changed when its resource went away");
        assertEquals("value", keyedByRID.get(RID), "a RID must stay findable as a map key after its resource is disposed");
    }

    @Test
    public void aNullTypeFallsBackToUndefined() {
        assertSame(ResourceType.Undefined, new ResourceID(null).type);
    }
}
