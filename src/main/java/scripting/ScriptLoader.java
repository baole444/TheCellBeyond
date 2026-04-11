package scripting;

import TheCellBeyond.GameObject;
import components.Component;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import utility.log.EngineLog;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ScriptLoader {
    private static final EngineLog Logger = new EngineLog(ScriptLoader.class);
    private static final String ClassExtension = ".class";
    private static URLClassLoader ScriptClassLoader;
    private static List<Path> scanDirectories = new ArrayList<>();
    private static final List<TypeEntry> gameObjectTypes = new ArrayList<>();
    private static final List<TypeEntry> componentTypes = new ArrayList<>();

    private ScriptLoader() {}

    public static void load(List<Path> scanDirs) {
        if (scanDirs == null || scanDirs.isEmpty()) return;
        unload();
        scanDirectories = new ArrayList<>(scanDirs);
        List<URL> jarUrls = new ArrayList<>();
        List<Path> jarPaths = new ArrayList<>();
        for (Path dir : scanDirectories) {
            if (!Files.isDirectory(dir)) continue;
            collectJars(dir, jarUrls, jarPaths);
        }
        if (jarUrls.isEmpty()) {
            Logger.info("No JARs found in scan directories");
            return;
        }
        ScriptClassLoader = new URLClassLoader(jarUrls.toArray(URL[]::new), ScriptLoader.class.getClassLoader());
        for (Path jarPath : jarPaths) scanJar(jarPath);
        Logger.info(String.format("Loaded %d custom game object types(s) and %d custom component type(s)", gameObjectTypes.size(), componentTypes.size()));
    }

    public static void unload() {
        gameObjectTypes.clear();
        componentTypes.clear();
        EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.ScriptClassUnloaded));
        if (ScriptClassLoader == null) return;
        try {
            ScriptClassLoader.close();
        } catch (IOException e) {
            Logger.error(String.format("Failed to close script classloader: %s", e.getMessage()));
        }
        ScriptClassLoader = null;
    }

    public static void reload() {
        load(scanDirectories);
        EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.ScriptCLassReloaded));
    }

    public static ClassLoader classLoader() {
        if (ScriptClassLoader != null) return ScriptClassLoader;
        return ScriptLoader.class.getClassLoader();
    }

    public static List<TypeEntry> gameObjectTypes() {
        return Collections.unmodifiableList(gameObjectTypes);
    }

    public static List<TypeEntry> componentTypes() {
        return Collections.unmodifiableList(componentTypes);
    }

    private static void collectJars(Path dir, List<URL> jarUrls, List<Path> jarPaths) {
        try (Stream<Path> walker = Files.walk(dir)) {
            walker.filter(path -> path.toString().endsWith(".jar") && Files.isRegularFile(path))
                    .forEach(jar -> {
                        try {
                            jarUrls.add(jar.toUri().toURL());
                            jarPaths.add(jar);
                        } catch (IOException e) {
                            Logger.error(String.format("Failed to resolve '%s': %s", jar, e.getMessage()));
                        }
                    });
        } catch (IOException e) {
            Logger.error(String.format("Failed to scan '%s' directory: %s", dir, e.getMessage()));
        }
    }

    private static void scanJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .map(ScriptLoader::entryToClassName)
                    .forEach(ScriptLoader::tryRegisterClass);
        } catch (IOException e) {
            Logger.error(String.format("Failed to scan JAR '%s': %s", jarPath, e.getMessage()));
        }
    }

    private static String entryToClassName(String entryName) {
        return entryName.replace('/', '.').substring(0, entryName.length() - ClassExtension.length());
    }

    private static void tryRegisterClass(String className) {
        Class<?> T;
        try {
            T = ScriptClassLoader.loadClass(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            Logger.warning(String.format("Cannot load class '%s': %s", className, e.getMessage()));
            return;
        }
        RegisterGameObject goAnnotation = T.getAnnotation(RegisterGameObject.class);
        if (goAnnotation != null && GameObject.class.isAssignableFrom(T)) {
            String label = goAnnotation.label().isEmpty() ? T.getSimpleName() : goAnnotation.label();
            String description = goAnnotation.description().isEmpty() ? "Custom game object." : goAnnotation.description();
            gameObjectTypes.add(new TypeEntry(label, description, T));
            Logger.debug(String.format("Registered custom GameObject: %s", label));
            EngineEventCallback.emit(T, new EditorEvent(EditorEvent.Type.ScriptClassLoaded));
            return;
        }
        RegisterComponent componentAnnotation = T.getAnnotation(RegisterComponent.class);
        if (componentAnnotation == null || !Component.class.isAssignableFrom(T)) return;
        String label = componentAnnotation.label().isEmpty() ? T.getSimpleName() : componentAnnotation.label();
        String description = componentAnnotation.description().isEmpty() ? "Custom component." : componentAnnotation.description();
        componentTypes.add(new TypeEntry(label, description, T));
        Logger.debug(String.format("Registered custom Component: %s", label));
        EngineEventCallback.emit(T, new EditorEvent(EditorEvent.Type.ScriptClassLoaded));
    }
}
