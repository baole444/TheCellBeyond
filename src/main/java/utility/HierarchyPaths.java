package utility;

import TheCellBeyond.GameObject;
import TheCellBeyond.internal.LogicServer;
import components.Component;
import scene.Scene;
import utility.log.EngineLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HierarchyPaths {
    private static final EngineLog Logger = new EngineLog(HierarchyPaths.class);

    private HierarchyPaths() {}

    public static GameObject toGameObject(HierarchyPath absolutePath) {
        return toGameObject(absolutePath, null);
    }

    public static GameObject toGameObject(HierarchyPath path, GameObject context) {
        if (nullPath(path)) return null;
        if (path.isEmpty()) return context;
        if (path.targetComponent()) {
            String lastSegment = path.lastSegment();
            String object = HierarchyPath.toObjectName(lastSegment);

            if (object == null || object.isEmpty()) {
                if (path.segmentCount() == 1) return context;
                HierarchyPath objectPath = path.slice(0, path.segmentCount() - 1);
                return toGameObject(objectPath, context);
            }
        }

        GameObject current = startingObject(path, context);
        if (current == null) return null;
        for (int i = beginSearchIndex(path); i < path.segmentCount(); i++) {
            String segment = path.segment(i);
            current = navigateSubSegment(current, segment, path, i);
            if (current == null) return null;
        }

        return current;
    }

    public static GameObject toGameObject(String absolutePath) {
        return toGameObject(absolutePath, null);
    }

    public static GameObject toGameObject(String path, GameObject context) {
        HierarchyPath hierarchyPath = new HierarchyPath(path);
        return toGameObject(hierarchyPath, context);
    }

    public static Component toComponent(HierarchyPath absolutePath) {
        return toComponent(absolutePath, null);
    }

    public static Component toComponent(HierarchyPath path, GameObject context) {
        if (nullPath(path)) return null;
        if (path.isEmpty()) {
            Logger.warning("Cannot resolve empty path to component");
            return null;
        }
        if (!path.targetComponent()) {
            Logger.warning(String.format("Cannot resolve '%s' to component: no component specified at final agr", path));
            return null;
        }

        String lastSegment = path.lastSegment();
        String componentName = HierarchyPath.toComponentName(lastSegment);
        if (componentName == null || componentName.isEmpty()) {
            Logger.error(String.format("Cannot resolve '%s' to component: invalid component delimiter!", path));
            return null;
        }

        String objectName = HierarchyPath.toObjectName(lastSegment);
        GameObject targetObject;
        if (objectName == null || objectName.isEmpty()) {
            if (path.segmentCount() == 1) targetObject = context;
            else {
                HierarchyPath objectPath = path.slice(0, path.segmentCount() - 1);
                targetObject = toGameObject(objectPath, context);
            }
        } else targetObject = toGameObject(path, context);
        if (targetObject == null) {
            Logger.warning(String.format("Cannot resolve '%s' to component: missing object '%s'.", path, objectName));
            return null;
        }

        Component component = findComponent(targetObject, componentName);
        if (component == null) Logger.warning(String.format("Cannot resolve '%s' to component: component '%s' does not exist for object '%s'.", path, componentName, targetObject.name()));
        return component;
    }

    public static Component toComponent(String absolutePath) {
        return toComponent(absolutePath, null);
    }

    public static Component toComponent(String path, GameObject context) {
        HierarchyPath hierarchyPath = new HierarchyPath(path);
        return toComponent(hierarchyPath, context);
    }

    public static Object resolve(HierarchyPath path, GameObject context) {
        if (path == null) return null;
        if (path.targetComponent()) return toComponent(path, context);
        return toGameObject(path, context);
    }

    public static HierarchyPath of(GameObject object) {
        String objPath = buildObjectPath(object);
        if (objPath == null) return null;
        return new HierarchyPath(objPath);
    }

    public static HierarchyPath of(Component component) {
        if (component == null) return null;
        String objPath = buildObjectPath(component.gameObject);
        if (objPath == null) return null;
        String componentName = component.name();
        if (componentName == null || componentName.isEmpty()) {
            componentName = component.getClass().getSimpleName();
        }

        return new HierarchyPath(objPath + HierarchyPath.ComponentDelimiter + componentName);
    }

    private static GameObject startingObject(HierarchyPath path, GameObject context) {
        if (path.absolute) return fromSceneRoot(path);
        if (context == null) Logger.error(String.format("Cannot resolve relative path '%s': missing context object!", path));
        return context;
    }

    private static int beginSearchIndex(HierarchyPath path) {
        if (path.absolute) return path.fromRoot() ? 2 : 1;
        return path.fromCurrent() ? 1 : 0;
    }

    private static GameObject fromSceneRoot(HierarchyPath path) {
        Scene scene = LogicServer.currentScene();
        if (scene == null) {
            Logger.error(String.format("Cannot resolve absolute path '%s': no active scene!", path));
            return null;
        }

        if (path.segmentCount() == 1 && path.fromRoot()) {
            return scene.root();
        }

        String segment = path.segment(path.fromRoot() ? 1 : 0);
        if (segment == null) {
            Logger.error(String.format("Invalid absolute path '%s': no object defined at the first argument!", path));
            return null;
        }

        GameObject root = scene.root();
        if (root == null) {
            Logger.error(String.format("Cannot resolve absolute path '%s': no root object in scene!", path));
            return null;
        }

        for (GameObject go : root.getChildren()) {
            if (go.name() != null && go.name().equals(segment)) return go;
            if (go.getClass().getSimpleName().equals(segment)) return go;
        }

        Logger.warning(String.format("No root object named '%s' for '%s' founded!", segment, path));
        return null;
    }

    private static GameObject navigateSubSegment(GameObject current, String segment, HierarchyPath path, int segmentIndex) {
        if (segment == null) return null;
        if (segment.equals(HierarchyPath.Current)) return current;
        if (segment.equals(HierarchyPath.Parent)) {
            GameObject parent = current.getParent();
            if (parent == null) Logger.warning(String.format("Cannot resolve '%s' at arg %d: object '%s' has no parent!", path, segmentIndex, current.name()));
            return parent;
        }

        String objectName = HierarchyPath.toObjectName(segment);
        if (objectName == null) {
            Logger.error(String.format("Cannot resolve '%s' at arg %d: invalid segment '%s'!", path, segmentIndex, segment));
            return null;
        }

        GameObject child = findChildObject(current, objectName);
        if (child == null) Logger.warning(String.format("Cannot resolve '%s' at arg %d: no child '%s' of '%s'", path, segmentIndex, objectName, current.name()));
        return child;
    }

    private static GameObject findChildObject(GameObject parent, String name) {
        GameObject byName = parent.getChild(name);
        if (byName != null) return byName;
        for (GameObject child : parent.getChildren()) {
            if (child.getClass().getSimpleName().equals(name)) return child;
        }

        return null;
    }

    private static Component findComponent(GameObject go, String componentName) {
        Component byName = go.findComponentByName(componentName);
        if (byName != null) return byName;
        for (Component c : go.getComponents()) {
            if (c.getClass().getSimpleName().equals(componentName)) return c;
        }
        return null;
    }

    private static String buildObjectPath(GameObject object) {
        if(object == null) return null;
        List<String> segments = new ArrayList<>();
        GameObject current = object;
        while (current != null) {
            segments.add(current.name());
            current = current.getParent();
        }

        Collections.reverse(segments);
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            builder.append(HierarchyPath.Separator);
            builder.append(segment);
        }

        return builder.toString();
    }

    private static boolean nullPath(HierarchyPath path) {
        if (path != null) return false;
        Logger.error("Cannot resolve null path!");
        return true;
    }
}
