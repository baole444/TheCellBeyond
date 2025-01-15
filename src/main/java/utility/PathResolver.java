package utility;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {
    public static String toRoot (String path) {
        Path absPath = Paths.get(path).toAbsolutePath();
        Path rootDir = absPath.getParent();

        return rootDir.toString();
    }

    public static String resolveRelative (String root, String relative) {
        Path rootPath = Paths.get(root);
        Path resovledPath = rootPath.resolve(relative).normalize();

        return resovledPath.toString();
    }
}
