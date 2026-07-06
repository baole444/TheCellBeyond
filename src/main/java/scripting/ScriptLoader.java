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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * ScriptLoader is a collection of static method use to load or unload classes from JAR files into the engine.
 * <p>
 * All classes in the JAR files are loaded in. For any class annotated with either {@link RegisterGameObject} or {@link RegisterComponent},
 * They are cached to use elsewhere.
 * </p>
 * ScriptLoader's operation are synchronous on the thread it bound to with {@link #bindMainThread()}.
 * Call to {@link #load(List)}, {@link #reload()} and {@link #unload()} are thread safe.
 */
public final class ScriptLoader {
    private static final EngineLog Logger = new EngineLog(ScriptLoader.class);
    private static final String ClassExtension = ".class";
    private static URLClassLoader ScriptClassLoader;
    private static volatile List<Path> scanDirectories = List.of();
    private static final List<TypeEntry> gameObjectTypes = new ArrayList<>();
    private static final List<TypeEntry> componentTypes = new ArrayList<>();
    private static final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private static final AtomicReference<CompletableFuture<Void>> currentTask = new AtomicReference<>();
    private static volatile Thread mainThread = null;

    private ScriptLoader() {}

    /**
     * Bind the script loader to the current thread. This should be called once on the main thread before any script loading process started.
     * <p>
     * All load and unload process will be run on the bound thread.
     */
    public static void bindMainThread() {
        if (mainThread == null) mainThread = Thread.currentThread();
    }

    /**
     * Execute any load or reload request queued by other threads.
     * <p>
     * This should be call oncer per engine main loop.
     */
    public static void processPending() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                Logger.error("Failed to run queued script loader task: " + e.getMessage());
            }
        }
    }

    /**
     * Request to load the script JAR files from the given scan directories.
     * <p>
     * If the request not originated from the loader's main thread, the task i8s queued for polling.
     * @param scanDirs the directories to scan for script JARs
     * @return a future completing when loading finished, or a completed future if there is nothing to load
     */
    public static CompletableFuture<Void> load(List<Path> scanDirs) {
        if (scanDirs == null || scanDirs.isEmpty()) return CompletableFuture.completedFuture(null);
        scanDirectories = List.copyOf(scanDirs);
        return requestLoad(scanDirectories, false);
    }

    /**
     * Unload all script classes and close the current class loader.
     */
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

    /**
     * Request to reload the script JAR files using the last scan directories.
     * @return a future completing when reloading finished, or an ongoing load's future
     */
    public static CompletableFuture<Void> reload() {
        return requestLoad(scanDirectories, true);
    }

    /**
     * Get the current class loader of {@link ScriptLoader}.
     * @return the in used class loader
     */
    public static ClassLoader classLoader() {
        if (ScriptClassLoader != null) return ScriptClassLoader;
        return ScriptLoader.class.getClassLoader();
    }

    /**
     * Get an unmodifiable view of the loaded game object types annotated with {@link RegisterGameObject}.
     * @return the list of type entries for game objects
     */
    public static List<TypeEntry> gameObjectTypes() {
        return Collections.unmodifiableList(gameObjectTypes);
    }

    /**
     * Get an unmodifiable view of the loaded component types annotated with {@link RegisterComponent}.
     * @return the list of type entries for components
     */
    public static List<TypeEntry> componentTypes() {
        return Collections.unmodifiableList(componentTypes);
    }

    private static CompletableFuture<Void> requestLoad(List<Path> dirs, boolean emitReload) {
        Thread bound = mainThread;
        if (bound == null) {
            Logger.error("Failed to load scripts: main thread for script loader not bounded");
            return CompletableFuture.failedFuture(new IllegalStateException("ScriptLoader main thread unbound, please bound the main thread on engine startup"));
        }
        CompletableFuture<Void> task = new CompletableFuture<>();
        CompletableFuture<Void> existing = currentTask.compareAndExchange(null, task);
        if (existing != null) return existing;
        Runnable job = () -> {
            try {
                loadScripts(dirs);
                if (emitReload) EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.ScriptCLassReloaded));
                task.complete(null);
            } catch (RuntimeException e) {
                task.completeExceptionally(e);
            } finally {
                currentTask.set(null);
            }
        };
        if (Thread.currentThread() == bound) job.run();
        else tasks.add(job);
        return task;
    }

    private static void loadScripts(List<Path> scanDirs) {
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
                    .filter(name -> name.endsWith(ClassExtension))
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
