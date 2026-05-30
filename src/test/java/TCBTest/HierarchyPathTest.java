package TCBTest;

import org.junit.jupiter.api.Test;
import utility.HierarchyPath;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HierarchyPathTest {
    @Test
    public void parseSegmentsIgnoreBlankAndSeparator() {
        HierarchyPath path = new HierarchyPath("/root/Parent/Child");
        assertEquals(3, path.segmentCount(), "Leading separator should not produce an empty segment");
        assertEquals("root", path.firstSegment());
        assertEquals("Child", path.lastSegment());
        assertEquals("Parent", path.segment(1));
        assertNull(path.segment(3), "Out of bound index should return null");
        assertNull(path.segment(-1), "Negative index should return null");
    }

    @Test
    public void absoluteAndEmptyDetection() {
        assertTrue(new HierarchyPath("/root/Child").absolute);
        assertFalse(new HierarchyPath("Child").absolute);
        assertTrue(new HierarchyPath("").isEmpty(), "Blank path should be empty");
        assertTrue(new HierarchyPath(null).isEmpty(), "Null path should be empty");
        assertTrue(new HierarchyPath("   ").isEmpty(), "Whitespace path should be empty");
    }

    @Test
    public void toObjectNameStripsComponent() {
        assertEquals("Transition", HierarchyPath.toObjectName("Transition::TransitionController"));
        assertEquals("Transition", HierarchyPath.toObjectName("Transition"), "Segment without component should return as is");
        assertNull(HierarchyPath.toObjectName("::TransitionController"), "Empty object part should return null");
        assertNull(HierarchyPath.toObjectName(null), "Null segment should return null");
    }

    @Test
    public void toComponentNameStripsObject() {
        assertEquals("TransitionController", HierarchyPath.toComponentName("Transition::TransitionController"));
        assertNull(HierarchyPath.toComponentName("Transition"), "Segment without delimiter has no component");
        assertNull(HierarchyPath.toComponentName("Transition::"), "Empty component part should return null");
        assertNull(HierarchyPath.toComponentName(null), "Null segment should return null");
    }

    @Test
    public void targetComponentDetection() {
        assertTrue(new HierarchyPath("/root/Transition::TransitionController").targetComponent());
        assertTrue(new HierarchyPath("::TransitionController").targetComponent());
        assertFalse(new HierarchyPath("/root/Transition").targetComponent());
        assertFalse(new HierarchyPath("").targetComponent(), "Empty path cannot target component");
    }

    @Test
    public void fromRootIgnoresAppendedComponent() {
        assertTrue(new HierarchyPath("/root").fromRoot());
        assertTrue(new HierarchyPath("/root/Transition::TransitionController").fromRoot());
        assertTrue(new HierarchyPath("/root::TransitionController").fromRoot(),
                "Root with appended component should still be from root");
        assertFalse(new HierarchyPath("root").fromRoot(), "Relative path is never from root");
        assertFalse(new HierarchyPath("/Transition::TransitionController").fromRoot(),
                "Non root first segment should not be from root");
    }

    @Test
    public void fromCurrentIgnoresAppendedComponent() {
        assertTrue(new HierarchyPath("./Child").fromCurrent());
        assertTrue(new HierarchyPath(".::TransitionController").fromCurrent(),
                "Current with appended component should still be from current");
        assertFalse(new HierarchyPath("Child").fromCurrent(), "Implicit relative path is not flagged as current");
        assertFalse(new HierarchyPath("/root").fromCurrent());
    }

    @Test
    public void fromParentIgnoresAppendedComponent() {
        assertTrue(new HierarchyPath("../Sibling").fromParent());
        assertTrue(new HierarchyPath("..::TransitionController").fromParent(),
                "Parent with appended component should still be from parent");
        assertFalse(new HierarchyPath("Child").fromParent());
    }

    @Test
    public void sliceProducesSubPath() {
        HierarchyPath path = new HierarchyPath("/root/Parent/Transition::TransitionController");
        assertEquals(3, path.segmentCount(), "Component delimiter does not split a segment");
        HierarchyPath objectPath = path.slice(0, path.segmentCount() - 1);
        assertEquals(2, objectPath.segmentCount());
        assertTrue(objectPath.absolute, "Slice starting at index 0 of an absolute path stays absolute");
        assertEquals("Parent", objectPath.lastSegment());
        assertFalse(objectPath.targetComponent(), "Component segment should be sliced away");

        HierarchyPath mid = path.slice(1, 3);
        assertFalse(mid.absolute, "Slice not starting at index 0 is relative");
        assertEquals("Parent", mid.firstSegment());
        assertEquals("Transition::TransitionController", mid.lastSegment());
    }

    @Test
    public void sliceEmptyAndClampedRange() {
        HierarchyPath path = new HierarchyPath("/root/Child");
        assertTrue(path.slice(1, 1).isEmpty(), "Equal start and end should produce empty path");
        assertEquals(2, path.slice(-5, 99).segmentCount(), "Out of range indices should clamp to bounds");
    }

    @Test
    public void equalityAndOriginPath() {
        HierarchyPath path = new HierarchyPath("  /root/Child  ");
        assertEquals("/root/Child", path.originPath, "Origin path should be trimmed");
        assertEquals(new HierarchyPath("/root/Child"), path);
        assertEquals(new HierarchyPath("/root/Child").hashCode(), path.hashCode());
        assertNotEquals(new HierarchyPath("/root/Other"), path);
    }
}
