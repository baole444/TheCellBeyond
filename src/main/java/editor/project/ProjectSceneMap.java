package editor.project;

import java.util.List;

public class ProjectSceneMap {
    private String path;
    private List<String> asset;
    private List<String> sheet;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public List<String> getAsset() {
        return asset;
    }

    public void setAsset(List<String> asset) {
        this.asset = asset;
    }

    public List<String> getSheet() {
        return sheet;
    }

    public void setSheet(List<String> sheet) {
        this.sheet = sheet;
    }

    @Override
    public String toString() {
        return "Scene{" +
                "path='" + path + '\'' +
                ", asset=" + asset +
                ", sheet=" + sheet +
                '}';
    }
}
