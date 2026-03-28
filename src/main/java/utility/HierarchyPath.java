package utility;

import TheCellBeyond.GameObject;
import components.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HierarchyPath represent a pth to a {@link TheCellBeyond.GameObject} or {@link components.Component} of an object in the scene tree hierarchy.
 * <p>
 * A hierarchy path is represented as a string composed of object name or class name,
 * separated by {@code /} with {@code ::} as component delimiter.
 * Similar to a filesystem path, {@code ..} and {@code .} are special symbols.
 * They refer to the parent object and the current object respectively.
 */
public final class HierarchyPath {
    public static final String Separator = "/";
    public static final String Current = ".";
    public static final String Parent = "..";
    public static final String Root = "root";
    public static final String ComponentDelimiter = "::";

    private final List<String> segments = new ArrayList<>();
    public final String originPath;
    public final boolean absolute;

    /**
     * Create a new {@link HierarchyPath} with the given path string.
     * @param path the path string that has hierarchy path format
     */
    public HierarchyPath(String path) {
        if (path == null) path = "";
        originPath = path.trim();
        absolute = originPath.startsWith(Separator);
        String[] args = originPath.split(Separator);
        for (String s : args) {
            if (s.isEmpty()) continue;
            segments.add(s);
        }
    }

    private HierarchyPath(String originPath, List<String> segments, boolean absolute) {
        this.originPath = originPath;
        this.segments.addAll(segments);
        this.absolute = absolute;
    }

    /**
     * Get the number of segment separated by the {@link #Separator} in this hierarchy path.
     * <p>
     * For example, the path {@code /root/Niko::Lightbulb} has 2 segments.
     * @return the segment count of the path
     */
    public int segmentCount() {
        return segments.size();
    }

    /**
     * Get the segment string at a specified index of the hierarchy path.
     * If the provided index is out of bound ({index &lt; 0 or index &gt;= {@link #segmentCount()}), this will return {@code null}.
     * <p>
     * The string return by this method could be the name or the class of the game object,
     * special symbols like relative, parent or root.
     * It could also be the name or class of the component in case of relative component path,
     * for example {@code ::SpriteRenderer} is the current object's sprite renderer component.
     * @param index the index of the wanted segment
     * @return the string of the wanted segment, without the {@link #Separator}
     */
    public String segment(int index) {
        if (index < 0 || index >= segments.size()) return null;
        return segments.get(index);
    }

    /**
     * Get the segment string at the first index of the hierarchy path.
     * <p>
     * The string return by this method could be the name or the class of the game object,
     * special symbols like relative, parent or root.
     * It could also be the name or class of the component in case of relative component path,
     * for example {@code ::SpriteRenderer} is the current object's sprite renderer component.
     * @return the string of the wanted segment, without the {@link #Separator}
     */
    public String firstSegment() {
        return segments.getFirst();
    }

    /**
     * Get the segment string at the last index of the hierarchy path.
     * <p>
     * The string return by this method could be the name or the class of the game object,
     * special symbols like relative, parent or root.
     * It could also be the name or class of the component in case of relative component path,
     * for example {@code ::SpriteRenderer} is the current object's sprite renderer component.
     * @return the string of the wanted segment, without the {@link #Separator}
     */
    public String lastSegment() {
        return segments.getLast();
    }

    public HierarchyPath slice(int starIndex, int endIndex) {
        int size = segments.size();
        starIndex = Math.max(0, Math.min(starIndex, size));
        endIndex = Math.max(0, Math.min(endIndex, size));
        if (starIndex > endIndex) starIndex = endIndex;
        if (starIndex == endIndex) return new HierarchyPath("");
        List<String> subSegments = segments.subList(starIndex, endIndex);
        boolean sliceAbs = starIndex == 0 && absolute;
        StringBuilder builder = new StringBuilder();
        if (sliceAbs) builder.append(Separator);
        for (int i = 0; i < subSegments.size(); i++) {
            if (i > 0) builder.append(Separator);
            builder.append(subSegments.get(i));
        }
        return new HierarchyPath(builder.toString(), subSegments, sliceAbs);
    }

    /**
     * Check if the hierarchy path is empty or not. If the path is empty,
     * this mean the path that used to create it was improperly formated, blank or null.
     * @return true if there is no segment
     */
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public boolean fromRoot() {
        return absolute && !segments.isEmpty() && segments.getFirst().equals(Root);
    }

    public boolean fromParent() {
        return !segments.isEmpty() && segments.getFirst().equals(Parent);
    }

    public boolean fromCurrent() {
        return !segments.isEmpty() && segments.getFirst().equals(Current);
    }

    public boolean targetComponent() {
        if (segments.isEmpty()) return false;
        return segments.getLast().contains(ComponentDelimiter);
    }

    public List<String> segments() {
        return Collections.unmodifiableList(segments);
    }

    public static String toComponentName(String segment) {
        if (segment == null || !segment.contains(ComponentDelimiter)) return null;
        int delimiterIndex = segment.indexOf(ComponentDelimiter);
        String component = segment.substring(delimiterIndex + ComponentDelimiter.length());
        return component.isEmpty() ? null : component;
    }

    public static String toObjectName(String segment) {
        if (segment == null || !segment.contains(ComponentDelimiter)) return segment;
        int delimiterIndex = segment.indexOf(ComponentDelimiter);
        String object = segment.substring(0, delimiterIndex);
        return object.isEmpty() ? null : object;
    }

    public static HierarchyPath of(GameObject object) {
        return HierarchyPaths.of(object);
    }

    public static HierarchyPath of(Component component) {
        return HierarchyPaths.of(component);
    }

    @Override
    public String toString() {
        return originPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HierarchyPath other)) return false;
        return originPath.equals(other.originPath);
    }

    @Override
    public int hashCode() {
        if (originPath == null) return 0;
        return originPath.hashCode();
    }
}