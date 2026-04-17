package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import utility.UnifiedPaths;

public record ProjectAssetMap(String path, int sizeX, int sizeY) {
    public static final String metadataTag = "TextureUnit";

    @JsonIgnore
    public String metadata() {
        if (path == null || path.isBlank()) return null;
        return UnifiedPaths.appendMetadata(UnifiedPaths.ProjectPrefix + path, metadataTag, String.valueOf(sizeX), String.valueOf(sizeY));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProjectAssetMap(String path1, int x, int y))) return false;
        return this.path.equals(path1) && this.sizeX == x && this.sizeY == y;
    }
}
