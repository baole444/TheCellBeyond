package utility;

import TheCellBeyond.GameObject;
import components.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HierarchyPath represent a pth to a {@link TheCellBeyond.GameObject} or {@link components.Component}
 * of an object in the scene tree hierarchy.
 * <p>
 * A hierarchy path is represented as a string composed of object name or class name,
 * separated by {@code /} with {@code ::} as component delimiter.
 * Similar to a filesystem path, {@code ..} and {@code .} are special symbols.
 * They refer to the parent object and the current object respectively.
 * <p>
 * <b>Hierarchy Path Formats:</b>
 * <ul>
 *     <li> <i><u>/root/object</u></i> - Absolute path starting from scene's root object.</li>
 *     <li> <i><u>/path/to/child</u></i> - Absolute path starting from a specific object.</li>
 *     <li> <i><u>./path/to/child</u></i> - Relative path starting from the context object.</li>
 *     <li> <i><u>/path/to/object::component</u></i> - Path appended with component, allow resolve to component if needed.</li>
 * </ul>
 * Relative path from current object can also be shortened to <i><u>/path/to/child</u></i>.
 * <p>
 * By appending {@code ::componentName} to the end of any path, it can be resolves to {@link Component}.
 * Component path also support shortcut for component of current object,
 * using {@code ::componentName} or {@code ./::componentName}.
 * </p>
 * When a path is appended with component delimiter and component name, it can still be resolved to object as normal.
 */
public final class HierarchyPath {
    /**
     * Path segment separator symbol.
     */
    public static final String Separator = "/";
    /**
     * Relative symbol for current object.
     */
    public static final String Current = ".";
    /**
     * Relative symbol for current object's parent.
     */
    public static final String Parent = "..";
    /**
     * Symbol for root object in scene.
     */
    public static final String Root = "root";
    /**
     * Symbol to separate component from its game object segment.
     */
    public static final String ComponentDelimiter = "::";
    /**
     * The original path string that was used to create this {@link HierarchyPath}.
     */
    public final String originPath;
    /**
     * Is this hierarchy path an absolute path or not.
     */
    public final boolean absolute;

    private final List<String> segments = new ArrayList<>();

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

    /**
     * Slide the current hierarchy path into a new path using the given the start and end index.
     * @param starIndex the starting segment's index to slice
     * @param endIndex the ending segment index's to slice
     * @return a new {@link HierarchyPath} sliced from the current path
     */
    public HierarchyPath slice(int starIndex, int endIndex) {
        int size = segments.size();
        starIndex = Math.clamp(starIndex, 0, size);
        endIndex = Math.clamp(endIndex, 0, size);
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

    /**
     * Check if the hierarchy path starts from root object or not.
     * A path is considered starting from root if it is absolute and the first segment is the {@link #Root} symbol.
     * <p>
     * This does not affect by the presence of {@link #ComponentDelimiter} in the segment.
     * @return true if the path is from root object
     */
    public boolean fromRoot() {
        if (!absolute || segments.isEmpty()) return false;
        return Root.equals(toObjectName(segments.getFirst()));
    }

    /**
     * Check if the hierarchy path is relative and starts from parent of context object or not.
     * A path is considered starting from parent if it's first segment is the {@link #Parent} symbol.
     * <p>
     * This does not affect by the presence of {@link #ComponentDelimiter} in the segment.
     * @return true if the path is from parent object
     */
    public boolean fromParent() {
        if (segments.isEmpty()) return false;
        return Parent.equals(toObjectName(segments.getFirst()));
    }

    /**
     * Check if the hierarchy path is relative and starts from current object or not.
     * A path is considered relative from the current object if its first segment is {@link #Current} symbol.
     * <p>
     * This does not affect by the presence of {@link #ComponentDelimiter} in the segment.
     * @return true if the path is from current object
     */
    public boolean fromCurrent() {
        if (segments.isEmpty()) return false;
        return Current.equals(toObjectName(segments.getFirst()));
    }

    /**
     * Check if the hierarchy path can target and resolve to a component or not.
     * This means the path had been appended with {@link #ComponentDelimiter} and component's name in the last segment.
     * @return true if the path can target and resolve to a component
     */
    public boolean targetComponent() {
        if (segments.isEmpty()) return false;
        return segments.getLast().contains(ComponentDelimiter);
    }

    /**
     * Get an unmodifiable list of segment from this hierarchy path.
     * @return the list of segment string
     */
    public List<String> segments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Extract the name of the component from a segment string.
     * This will extract the object's name and the {@link #ComponentDelimiter} out of the given string.
     * @param segment the segment string to extract name from
     * @return the name of the component in the segment or null if there is no component appended
     */
    public static String toComponentName(String segment) {
        if (segment == null || !segment.contains(ComponentDelimiter)) return null;
        int delimiterIndex = segment.indexOf(ComponentDelimiter);
        String component = segment.substring(delimiterIndex + ComponentDelimiter.length());
        return component.isEmpty() ? null : component;
    }

    /**
     * Extract the name of the game object from a segment string.
     * This will extract the {@link #ComponentDelimiter} and component name out of the given string.
     * @param segment the segment string to extract name from
     * @return the name of the object in the segment or null if there is no object
     */
    public static String toObjectName(String segment) {
        if (segment == null || !segment.contains(ComponentDelimiter)) return segment;
        int delimiterIndex = segment.indexOf(ComponentDelimiter);
        String object = segment.substring(0, delimiterIndex);
        return object.isEmpty() ? null : object;
    }

    /**
     * Get the hierarchy path represent the given game object.
     * @param object the game object to get hierarchy for
     * @return the {@link HierarchyPath} that is the absolute path to the object, or null if path build failed
     * @see HierarchyPaths#of(GameObject)
     */
    public static HierarchyPath of(GameObject object) {
        return HierarchyPaths.of(object);
    }

    /**
     * Get the hierarchy path represent the given component.
     * @param component the game object to get hierarchy for
     * @return the {@link HierarchyPath} that is the absolute path to the component, or null if path build failed
     * @see HierarchyPaths#of(Component)
     */
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