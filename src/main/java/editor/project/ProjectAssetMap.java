package editor.project;

public class ProjectAssetMap {
    private String path;
    private int sizeX;
    private int sizeY;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "path='" + path + '\'' +
                ", sizeX=" + sizeX +
                ", sizeY=" + sizeY +
                "}";
    }
}
