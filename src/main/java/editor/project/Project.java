package editor.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import utility.PathResolver;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Project {
    public static ProjectData CurrentProject = null;
    public static String ProjectRoot = null;

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static ProjectData loadFromYaml(String path) {
        try {
            File projectFile = new File(path);

            CurrentProject = YAML_MAPPER.readValue(projectFile, ProjectData.class);
            ProjectRoot = PathResolver.toRoot(path);

            return CurrentProject;
        } catch (IOException e) {
            System.err.println("Failed to load project file: " + e.getMessage());
            return null;
        }
    }

    public static void saveToYaml(String path) {
        try {
            if (CurrentProject != null) YAML_MAPPER.writeValue(new File(path), CurrentProject);
        } catch (IOException e) {
            System.err.println("Failed to save project file: " + e.getMessage());
        }
    }

    public static List<String> getSceneNames() {
        if (CurrentProject == null) return List.of();

        return CurrentProject.scenes().keySet().stream().toList();
    }
}
