package editor.project;

public class ProjectSheetMap {
    private String category;
    private String path;
    private int count;
    private int sizeX;
    private int sizeY;
    private int padding;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    @Override
    public String toString() {
        return "Sheet{" +
                "category='" + category + '\'' +
                ", path='" + path + '\'' +
                ", count=" + count +
                ", sizeX=" + sizeX +
                ", sizeY=" + sizeY +
                ", padding=" + padding +
                '}';
    }
}
