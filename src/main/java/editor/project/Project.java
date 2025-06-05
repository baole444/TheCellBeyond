package editor.project;

import org.yaml.snakeyaml.Yaml;
import utility.PathResolver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO: add packing project file support

public class Project {
    private String version;
    private ProjectInfo project;
    private Map<String, ProjectAssetMap> assets;
    private Map<String, ProjectSheetMap> sheets;
    private Map<String, ProjectSceneMap> scenes;
    private Map<String, ProjectPrefabricationMap> prefabs;

    private List<String> sceneNames = new ArrayList<>();

    public static Project CurrentProject = null;
    public static String ProjectRoot = null;

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

    public Map<String, ProjectAssetMap> getAssets() {
        return assets;
    }

    public void setAssets(Map<String, ProjectAssetMap> assets) {
        this.assets = assets;
    }

    public Map<String, ProjectSheetMap> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String, ProjectSheetMap> sheets) {
        this.sheets = sheets;
    }

    public Map<String, ProjectSceneMap> getScenes() {
        return scenes;
    }

    public void setScenes(Map<String, ProjectSceneMap> scenes) {
        this.scenes = scenes;
    }

    public Map<String, ProjectPrefabricationMap> getPrefabs() {
        return prefabs;
    }

    public void setPrefabs(Map<String, ProjectPrefabricationMap> prefabs) {
        this.prefabs = prefabs;
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
        builder.append("Prefabs: ").append(prefabs).append("\n");

        return builder.toString();
    }

    /**
     * Load a project definition file to memory.
     * Can be accessible via {@link #CurrentProject}
     * or assigned to a local variable
     * @param path location of the project's directory.
     * @return a {@link Project} reference.
     */
    public static Project loadFromYaml(String path) {
        try {
            InputStream inputStream = new FileInputStream(path);
            Yaml yaml = new Yaml();
            CurrentProject = yaml.loadAs(inputStream, Project.class);

            ProjectRoot = PathResolver.toRoot(path);

            PathResolver.initialize(ProjectRoot);

            if (CurrentProject.getScenes() != null) {
                List<String> sN = new ArrayList<>(CurrentProject.getScenes().keySet());
                CurrentProject.setSceneNames(sN);
            }

            if (CurrentProject.getPrefabs() != null && !CurrentProject.getPrefabs().isEmpty()) {
                try {
                    for (ProjectPrefabricationMap prefab : CurrentProject.getPrefabs().values()) {
                        prefab.validate();
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Error in prefabs: " + e.getMessage());
                    CurrentProject.setPrefabs(null);
                }
            }

            //System.out.println("Root directory is " + ProjectRoot);
            return CurrentProject;
        } catch (Exception e) {
            System.out.println();
        }
        return null;
    }

}
