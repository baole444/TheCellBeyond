package utility;

import scripting.API;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified path resolver, separated engine, project or external assets.
 * <p>
 * <b>Unified Path Formats:</b>
 * <ul>
 *      <li> <i><u>engine://path/to/asset</u></i> - Engine assets (using classpath's resource directory.)</li>
 *      <li> <i><u>project://path/to/asset</u></i> - Project assets (loaded from project directory).</li>
 *      <li> <i><u>relative/path/to/asset</u></i> - Legacy relative path, assume it is project asset.</li>
 *      <li> <i><u>/dir/path/to/asset</u></i> - Potential absolute path, can be resolved as external or project asset</li>
 * </ul>
 */
@API
public final class UnifiedPaths {
    /**
     * Prefix to mark a path belong to the engine classpath, not on file system.
     */
    public static final String EnginePrefix = "engine://";
    /**
     * Prefix to mark that a path belong to the user project, on file system.
     */
    public static final String ProjectPrefix = "project://";
    public static final String MetaStart = "<";
    public static final String MetaEnd = ">";
    public static final String MetaTag = "$";
    public static final String MetaValueDelimiter = "::";
    public static final String MetaSeparator = ",";
    private static String projectRoot = null;
    private static final ConcurrentHashMap<String, AssetPath> pathCache = new ConcurrentHashMap<>();
    private UnifiedPaths() {}

    /**
     * Asset classification, base on the path that leads to the asset's file on the system.
     */
    public enum AssetType {
        /**
         * Assets stored in the engine's classpath, compiled with the engine.
         * Used for default shader programs, fonts, textures and sounds.
         */
        ENGINE,
        /**
         * Assets stored within the user project's root directory.
         * Often is the case for most of user's imported resources.
         */
        PROJECT,
        /**
         * Assets or files stored outside the engine's classpath or the user project's root directory.
         * Often is the case for temporary imported resources or configurations
         * stored in operating system's designated directory, for example {@code AppData} on Windows.
         */
        EXTERNAL
    }

    /**
     * A record of the asset's path, resolved to follow unified path format.
     * @param originalPath the path that needed resolving
     * @param resolvedPath the result of resolving the original path
     * @param type the type of the asset path
     * @param isAbsolute is the original path absolute
     * @see AssetReference Wrap a path to unified path automatically
     */
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

    /**
     * Get the path that is the project root.
     * @return the project root or null if there is none
     */
    public static String projectRoot() {
        return projectRoot;
    }

    /**
     * Set the {@link UnifiedPaths} to use the provided project root path.
     * @param root the absolute path that leads to the user project's root directory
     */
    public static void projectRoot(String root) {
        pathCache.clear();
        projectRoot = root;
    }

    /**
     * Clear the cached resolved paths.
     */
    public static void clearCache() {
        pathCache.clear();
    }

    /**
     * Parse and resolve the given path, then cache it for subsequence resolve request.
     * @param path the relative or absolute path that need resolving
     * @return an existing or new record of {@link AssetPath}
     */
    public static AssetPath resolvePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        AssetPath cached = pathCache.get(path);
        if (cached != null) return cached;
        AssetPath resolved = parseAndResolve(path);
        pathCache.put(path, resolved);
        return resolved;
    }

    public static InputStream getAssetStream(AssetPath assetPath) throws IOException {
        switch (assetPath.type()) {
            case ENGINE -> {
                return getEngineAssetStream(assetPath.resolvedPath());
            }
            case PROJECT, EXTERNAL -> {
                return getProjectAssetStream(assetPath.resolvedPath());
            }
            default -> throw new IllegalArgumentException("Unknown asset type: " + assetPath.type());
        }
    }

    public static InputStream getAssetStream(String path) throws IOException {
        return getAssetStream(resolvePath(path));
    }

    /**
     * Check if a file or asset exists within the project, engine's scope or externally.
     * @param assetPath The {@link AssetPath} to check
     * @return true if the asset or file exist.
     * @see UnifiedPaths#isPathInsideProject(String path) Check if a path is of AssetType Project
     */
    public static boolean exists(AssetPath assetPath) {
        switch (assetPath.type()) {
            case ENGINE -> {
                return UnifiedPaths.class.getClassLoader().getResource(assetPath.resolvedPath()) != null;
            }
            case PROJECT, EXTERNAL -> {
                return Files.exists(Paths.get(assetPath.resolvedPath()));
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Check if a file or asset exists within the project, engine's scope or externally.
     * @param path the relative or absolute path to check
     * @return true if the asset or file exist
     * @see UnifiedPaths#isPathInsideProject(String path) Check if a path is of AssetType Project
     */
    public static boolean exists(String path) {
        if (path == null || path.isBlank()) return false;
        return exists(resolvePath(path));
    }

    /**
     * Check if a file or asset path located inside the project directory.
     * @param path the relative or absolute path to check
     * @return true if the path resolved as {@link AssetType#PROJECT}
     * @see UnifiedPaths#exists(String path) Check if a file or asset exists
     */
    public static boolean isPathInsideProject(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            AssetPath assetPath = resolvePath(path);
            return assetPath.type() == AssetType.PROJECT;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert an absolute project path back to its relative path for serialization.
     */
    public static String toProjectRelativePath(String absolutePath) {
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
    public static String toCanonicalPath(String path) {
        if (path == null) return null;
        int metaIndex = path.indexOf(MetaStart);
        String filePart = metaIndex >= 0 ? path.substring(0, metaIndex) : path;
        String rawMeta = metaIndex >= 0 ? path.substring(metaIndex) : null;
        AssetPath assetPath = resolvePath(filePart);
        String canonical = switch (assetPath.type) {
            case ENGINE -> assetPath.isAbsolute() ? assetPath.originalPath() : EnginePrefix + assetPath.resolvedPath;
            case PROJECT -> toProjectRelativePath(assetPath.resolvedPath);
            case EXTERNAL -> assetPath.resolvedPath;
        };
        return rawMeta != null ? canonical + rawMeta : canonical;
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
        String sanctioned = relative.startsWith("/") ? relative.substring(1) : relative;
        Path rootPath = Paths.get(root);
        Path resolvedPath = rootPath.resolve(sanctioned).normalize();
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

    public static boolean hasMetadata(String path) {
        return path != null && path.contains(MetaStart);
    }

    public static String stripMetadata(String path) {
        if (path == null) return null;
        int index = path.indexOf(MetaStart);
        return index >= 0 ? path.substring(0, index) : path;
    }

    public static String extractRawMetadata(String path) {
        if (path == null) return null;
        int index = path.indexOf(MetaStart);
        return index >= 0 ? path.substring(index) : null;
    }

    public static String appendMetadata(String canonicalPath, String tagName, String... values) {
        if (canonicalPath == null) return null;
        String tag = MetaTag + tagName + (values.length > 0 ? MetaValueDelimiter + String.join(MetaValueDelimiter, values) : "");
        String existingMeta = extractRawMetadata(canonicalPath);
        if (existingMeta != null) {
            String filePart = stripMetadata(canonicalPath);
            String currentValues = existingMeta.substring(MetaStart.length(), existingMeta.length() - MetaEnd.length());
            return filePart + MetaStart + currentValues + MetaSeparator + tag + MetaEnd;
        }
        return canonicalPath + MetaStart + tag + MetaEnd;
    }

    public static List<String> parseMetaTag(String path, String tagName) {
        if (invalidMetadata(tagName)) return List.of();
        String raw = extractRawMetadata(path);
        if (raw == null) return List.of();
        String inner = raw.substring(1, raw.length() - 1);
        for (String tag : inner.split(MetaSeparator)) {
            if (!tag.startsWith(MetaTag)) continue;
            String[] parts = tag.substring(MetaTag.length()).split(MetaValueDelimiter);
            if (parts.length > 0 && tagName.equals(parts[0])) return new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
        }
        return List.of();
    }

    public static boolean invalidMetadata(String value) {
        if (value == null || value.isBlank()) return true;
        return value.contains(MetaStart) || value.contains(MetaEnd) || value.contains(MetaTag) || value.contains(MetaValueDelimiter) || value.contains(MetaSeparator);
    }

    /**
     * Resolve the path into engine, project or external asset base on its starting prefix or the lack of it.
     */
    private static AssetPath parseAndResolve(String path) {
        if (path.startsWith(EnginePrefix)) {
            String enginePath = path.substring(EnginePrefix.length());
            return new AssetPath(path, enginePath, AssetType.ENGINE, true);
        }
        if (path.startsWith(ProjectPrefix)) {
            String projectPath = path.substring(ProjectPrefix.length());
            String absPath = resolveProjectPathToAbsolute(projectPath);
            return new AssetPath(path, absPath, AssetType.PROJECT, true);
        }
        Path inputPath = Paths.get(path);
        if (inputPath.isAbsolute()) {
            Path normalized = inputPath.toAbsolutePath().normalize();
            AssetPath external = new AssetPath(path, normalized.toString(), AssetType.EXTERNAL, true);
            if (projectRoot == null) return external;
            Path root = Paths.get(projectRoot).toAbsolutePath().normalize();
            if (normalized.startsWith(root)) return new AssetPath(path, normalized.toString(), AssetType.PROJECT, true);
            return external;
        }
        String abs = resolveProjectPathToAbsolute(path);
        return new AssetPath(path, abs, AssetType.PROJECT, false);
    }

    private static String resolveProjectPathToAbsolute(String relativePath) {
        if (projectRoot == null) return relativePath;
        String sanctioned = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        Path rootPath = Paths.get(projectRoot);
        Path resolvedPath = rootPath.resolve(sanctioned).normalize();
        return resolvedPath.toString();
    }

    private static InputStream getEngineAssetStream(String enginePath) throws IOException {
        if (enginePath == null || enginePath.isBlank()) {
            throw new IllegalArgumentException("Engine path cannot be null");
        }
        InputStream stream = UnifiedPaths.class.getClassLoader().getResourceAsStream(enginePath);
        if (stream == null) {
            throw new IOException("Engine asset not found: " + enginePath);
        }
        return stream;
    }

    private static InputStream getProjectAssetStream(String absolutePath) throws IOException {
        Path path = Paths.get(absolutePath);
        if (!Files.exists(path)) {
            throw new IOException("Project asset not found: " + absolutePath);
        }
        return Files.newInputStream(path);
    }
}
