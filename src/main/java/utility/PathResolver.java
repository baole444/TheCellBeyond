package utility;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {
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

}
