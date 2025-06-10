package utility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified path resolver, separated engine's and project's assets.
 * <p>
 * <b>Path formats:</b>
 * <li> <i><u>engine://path/to/asset</u></i> - Engine assets (using classpath's resource directory.)</li>
 * <li> <i><u>project://path/to/asset</u></i> - Project assets (loaded from project directory.)</li>
 * <li> <i><u>relative/path/to/asset</u></i> - Legacy relative path, assume it as project's asset.</li>
 */
public class PathResolver {
    private static final String ENGINE_PREFIX = "engine://";
    private static final String PROJECT_PREFIX = "project://";

    private final String projectRoot;
    private final ConcurrentHashMap<String, AssetPath> pathCache = new ConcurrentHashMap<>();

    private static volatile PathResolver instance;

    public enum AssetType {
        ENGINE, PROJECT
    }

    public record AssetPath(String originalPath, String resolvedPath, AssetType type, boolean isAbsolute) {
        @Override
        public String toString() {
            return originalPath;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AssetPath target)) return false;
            return originalPath.equals(target.originalPath);
        }

        @Override
        public int hashCode() {
            return originalPath.hashCode();
        }
    }

    private PathResolver(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public static void initialize(String projectRoot) {
        instance = new PathResolver(projectRoot);
    }

    public static PathResolver get() {
        if (instance == null) {
            throw new IllegalStateException("PathResolver not initialized. Please call initialize() first.");
        }
        return instance;
    }

    /**
     * Parse and resolve if it is an engine, or a project asset.
     */
    public AssetPath resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        AssetPath cached = pathCache.get(path);
        if (cached != null) return cached;

        AssetPath resolved = parseAndResolve(path);

        pathCache.put(path, resolved);

        return resolved;
    }

    // Resolve the path into engine or project asset base on its starting prefix or lacks of it.
    private AssetPath parseAndResolve(String path) {
        if (path.startsWith(ENGINE_PREFIX)) {
            String enginePath = path.substring(ENGINE_PREFIX.length());

            return new AssetPath(path, enginePath, AssetType.ENGINE, true);
        } else if (path.startsWith(PROJECT_PREFIX)) {
            String projectPath = path.substring(PROJECT_PREFIX.length());
            String absPath = resolveProjectPathToAbsolute(projectPath);

            return new AssetPath(path, absPath, AssetType.PROJECT, true);
        } else {
            if (isKnownEngineAsset(path)) {
                return new AssetPath(path, path, AssetType.ENGINE, false);
            } else {
                String abs = resolveProjectPathToAbsolute(path);

                return new AssetPath(path, abs, AssetType.PROJECT, false);
            }
        }
    }

    private boolean isKnownEngineAsset(String path) {
        return path.startsWith("assets/shaders/") ||
                path.startsWith("assets/fonts/") ||
                path.startsWith("assets/textures/Gizmo.png") ||
                path.equals("assets/textures/TCB icon.png");
    }

    private String resolveProjectPathToAbsolute(String relativePath) {
        if (projectRoot == null) {
            return relativePath;
        }

        Path rootPath = Paths.get(projectRoot);
        Path resolvedPath = rootPath.resolve(relativePath).normalize();
        return resolvedPath.toString();
    }

    public InputStream getAssetStream(AssetPath assetPath) throws IOException {
        switch (assetPath.type()) {
            case ENGINE -> {
                return getEngineAssetStream(assetPath.resolvedPath());
            }
            case PROJECT -> {
                return getProjectAssetStream(assetPath.resolvedPath());
            }
            default -> throw new IllegalArgumentException("Unknown asset type: " + assetPath.type());
        }
    }

    public InputStream getAssetStream(String path) throws IOException {
        return getAssetStream(resolvePath(path));
    }

    private InputStream getEngineAssetStream(String enginePath) throws IOException {
        InputStream stream = PathResolver.class.getClassLoader().getResourceAsStream(enginePath);

        if (stream == null) {
            throw new IOException("Engine asset not found: " + enginePath);
        }

        return stream;
    }

    private InputStream getProjectAssetStream(String absolutePath) throws IOException {
        Path path = Paths.get(absolutePath);
        if (!Files.exists(path)) {
            throw new IOException("Project asset not found: " + absolutePath);
        }

        return Files.newInputStream(path);
    }

    /**
     * Check to see if the assets exist within the project or the engine's scope.
     */
    public boolean exists(AssetPath assetPath) {
        switch (assetPath.type()) {
            case ENGINE -> {
                return PathResolver.class.getClassLoader().getResource(assetPath.resolvedPath()) != null;
            }
            case PROJECT -> {
                return Files.exists(Paths.get(assetPath.resolvedPath()));
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Check to see if the assets exist within the project or the engine's scope.
     */
    public boolean exists(String path) {
        return exists(resolvePath(path));
    }

    /**
     * Convert an absolute project path back to its relative path for serialization.
     */
    public String toProjectRelativePath(String absolutePath) {
        if (projectRoot == null || absolutePath == null) return absolutePath;

        Path root = Paths.get(projectRoot).toAbsolutePath().normalize();
        Path full = Paths.get(absolutePath).toAbsolutePath().normalize();

        // Outside of project directory.
        if (!full.startsWith(root)) return absolutePath;

        Path relative = root.relativize(full);
        return relative.toString().replace("\\", "/");
    }

    /**
     * Convert a path to its true path from storage.
     * Engine assets get prefix and project assets become relative.
     */
    public String toCanonicalPath(String path) {
        AssetPath assetPath = resolvePath(path);

        switch (assetPath.type()) {
            case ENGINE -> {
                if (assetPath.isAbsolute()) {
                    return assetPath.originalPath();
                } else {
                    return ENGINE_PREFIX + assetPath.resolvedPath();
                }
            }
            case PROJECT -> {
                return toProjectRelativePath(assetPath.resolvedPath());
            }
            default -> {
                return path;
            }
        }
    }

    public void clearCache() {
        pathCache.clear();
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    /**
     * Extract project's root directory.
     * @param path Project file absolute path.
     * @return Path to the parent folder of the project file.
     */
    public static String toRoot (String path) {
        Path absPath = Paths.get(path).toAbsolutePath();
        Path rootDir = absPath.getParent();

        return rootDir.toString();
    }

    /**
     * Merge a root path with a relative path together.
     * @param root Absolute project root path.
     * @param relative A relative path pointing to directory or file within the project's children folder.
     * @return Absolute path of the relative path and its root.
     */
    public static String resolveToAbsolute(String root, String relative) {
        if (root == null) return relative;

        Path rootPath = Paths.get(root);
        Path resolvedPath = rootPath.resolve(relative).normalize();

        return resolvedPath.toString();
    }

    /**
     * Extract a relative path from a root path and full path.
     * @param root Absolute project root path.
     * @param absolute A full path pointing to directory or file within the project's children folder.
     * @return Relative path from its root.
     */
    public static String resolveToRelative(String root, String absolute) {
        if (root == null) return absolute;

        Path inputPath = Paths.get(absolute).normalize();

        if (!inputPath.isAbsolute()) {
            return inputPath.toString().replace("\\", "/");
        }

        Path rootPath = Paths.get(root).toAbsolutePath().normalize();
        Path fullPath = inputPath.toAbsolutePath().normalize();

        if (!fullPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Path is outside project directory!");
        }

        Path resolvedPath = rootPath.relativize(fullPath).normalize();

        return resolvedPath.toString().replace("\\", "/");
    }

    /**
     * Check if PathResolver is initialized or not
     * @return true if PathResolver is initialized.
     */
    public static boolean isInitialized () {
        return instance != null;
    }
}
