package editor;

import org.yaml.snakeyaml.Yaml;
import utility.PathResolver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: find a way to combine the project's root with relative path in project file.
public class Project {
    private String version;
    private ProjectInfo project;
    private Map<String, projectAssetMap> assets;
    private Map<String, projectSheetMap> sheets;
    private Map<String, projectSceneMap> scenes;
    private List<String> sceneNames = new ArrayList<>();

    public static Project CurrentProject;
    public static String ProjectRoot;


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

    public Map<String, projectAssetMap> getAssets() {
        return assets;
    }

    public void setAssets(Map<String, projectAssetMap> assets) {
        this.assets = assets;
    }

    public Map<String, projectSheetMap> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String, projectSheetMap> sheets) {
        this.sheets = sheets;
    }

    public Map<String, projectSceneMap> getScenes() {
        return scenes;
    }

    public void setScenes(Map<String, projectSceneMap> scenes) {
        this.scenes = scenes;
    }

    public List<String> getSceneNames() {
        return sceneNames;
    }

    public void setSceneNames(List<String> sceneNames) {
        this.sceneNames = sceneNames;
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

    public static class projectAssetMap {
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

    public static class projectSheetMap {
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

    public static class projectSceneMap {
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

    public static Project loadFromYaml(String path) {
        try {
            InputStream inputStream = new FileInputStream(path);
            Yaml yaml = new Yaml();
            CurrentProject = yaml.loadAs(inputStream, Project.class);

            ProjectRoot = PathResolver.toRoot(path);

            if (CurrentProject.getScenes() != null) {
                List<String> sN = new ArrayList<>(CurrentProject.getScenes().keySet());
                CurrentProject.setSceneNames(sN);
            }

            //System.out.println("Root directory is " + ProjectRoot);
            return CurrentProject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
