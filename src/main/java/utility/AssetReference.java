package utility;

import TheCellBeyond.internal.ResourceID;
import scripting.API;

import java.util.Objects;

/**
 * AssetReference contains the canonically unified path of the asset, a way to safely persisting location on disk,
 * as the true path get resolved on first request during runtime.
 * <p>
 * This is the recommended way to use {@link utility.UnifiedPaths.AssetPath}, as it provide automatic conversion from original path.
 * </p>
 * Wrapper for asset's raw file path.
 */
@API
public class AssetReference {
    private final String canonicalPath;
    private transient volatile UnifiedPaths.AssetPath resolvedPath;
    /**
     * The resource ID this reference last resolved to during runtime, often belong to a native resource, regardless of status,
     * created from the canonical path of this reference.
     * <p>
     * Null until resolved.
     */
    transient volatile ResourceID RID = null;

    /**
     * Create a new {@link AssetReference} with the given path.
     * @param path the relative or absolute path to wrap
     */
    public AssetReference(String path) {
        canonicalPath = UnifiedPaths.toCanonicalPath(path);
        initializeResolvedPath();
    }

    /**
     * Resolve the canonical path if not yet.
     */
    private void initializeResolvedPath() {
        if (resolvedPath == null) {
            resolvedPath = UnifiedPaths.resolvePath(UnifiedPaths.stripMetadata(canonicalPath));
        }
    }

    /**
     * Get the canonical path of this reference.
     * @return the string of unified canonical path to the asset file
     */
    public String canonicalPath() {
        return canonicalPath;
    }

    /**
     * Get the resolved path of this reference.
     * @return the reference's {@link UnifiedPaths.AssetPath} which contain the resolved path
     */
    public UnifiedPaths.AssetPath resolvedPath() {
        if (resolvedPath == null) initializeResolvedPath();
        return resolvedPath;
    }

    /**
     * Get the resolved absolute path of this reference.
     * @return the string of the absolute path that leads to the asset's file on the system
     */
    public String absolutePath() {
        return resolvedPath().resolvedPath();
    }

    /**
     * Create a new {@link AssetReference} from this reference's canonical path.
     * @return a new {@link AssetReference}
     */
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
