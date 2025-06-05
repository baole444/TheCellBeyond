package utility;

import java.nio.file.Path;
import java.util.Objects;

public class AssetReference {
    private final String canonicalPath;
    private volatile PathResolver.AssetPath resolvedPath;

    public AssetReference(String path) {
        PathResolver resolver = PathResolver.get();
        this.resolvedPath = resolver.resolvePath(path);
        this.canonicalPath = resolver.toCanonicalPath(path);
    }

    public String getCanonicalPath() {
        return canonicalPath;
    }

    public PathResolver.AssetPath getResolvedPath() {
        return resolvedPath;
    }

    public String getAbsolutePath() {
        return resolvedPath.resolvedPath();
    }

    public AssetReference copy() {
        return new AssetReference(canonicalPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof AssetReference target)) return false;

        return Objects.equals(canonicalPath, target.canonicalPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalPath);
    }

    @Override
    public String toString() {
        return canonicalPath;
    }
}
