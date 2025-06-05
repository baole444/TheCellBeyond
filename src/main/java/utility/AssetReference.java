package utility;

import java.util.Objects;

public class AssetReference {
    private final String canonicalPath;
    private transient volatile PathResolver.AssetPath resolvedPath;

    public AssetReference(String path) {
        PathResolver resolver = PathResolver.get();
        this.canonicalPath = resolver.toCanonicalPath(path);
        initializeResolvedPath();
    }

    private void initializeResolvedPath() {
        if (this.resolvedPath == null) {
            PathResolver resolver = PathResolver.get();
            this.resolvedPath = resolver.resolvePath(this.canonicalPath);
        }
    }

    public String getCanonicalPath() {
        return canonicalPath;
    }

    public PathResolver.AssetPath getResolvedPath() {
        if (resolvedPath == null) initializeResolvedPath();

        return resolvedPath;
    }

    public String getAbsolutePath() {
        return getResolvedPath().resolvedPath();
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
