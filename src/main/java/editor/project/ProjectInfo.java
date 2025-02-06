package editor.project;

public class ProjectInfo {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ProjectInfo{name='" + name + "'}";
    }
}
