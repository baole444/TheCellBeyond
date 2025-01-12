package editor;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class Project {
    private String version;
    private ProjectInfo project;
    private Map<String, Asset> assets;
    private Map<String, Sheet> sheets;
    private Map<String, Scene> scenes;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ProjectInfo getProject() {
        return project;
    }

    public void setProject(ProjectInfo project) {
        this.project = project;
    }

    public Map<String, Asset> getAssets() {
        return assets;
    }

    public void setAssets(Map<String, Asset> assets) {
        this.assets = assets;
    }

    public Map<String, Sheet> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String, Sheet> sheets) {
        this.sheets = sheets;
    }

    public Map<String, Scene> getScenes() {
        return scenes;
    }

    public void setScenes(Map<String, Scene> scenes) {
        this.scenes = scenes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Version: ").append(version).append("\n");
        builder.append("Project Info: ").append(project).append("\n");
        builder.append("Assets: ").append(assets).append("\n");
        builder.append("Sheets: ").append(sheets).append("\n");
        builder.append("Scenes: ").append(scenes).append("\n");

        return builder.toString();
    }

    public static class ProjectInfo {
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

    public static class Asset {
        private String path;

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "Asset{path='" + path + "'}";
        }
    }

    public static class Sheet {
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

    public static class Scene {
        private String path;

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "Scene{path='" + path + "'}";
        }
    }

    public static Project loadFromYaml(String path) {
        try (InputStream inputStream = new FileInputStream(path)) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(inputStream, Project.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
