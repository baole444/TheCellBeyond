package editor.project;

import java.util.HashMap;
import java.util.Map;

public record ProjectData(
        String version, ProjectPreference project,
        Map<String, ProjectAssetMap> assets,
        Map<String, Map<String, ProjectSheetMap>> sheets,
        Map<String, ProjectSceneMap> scenes
) {
    public ProjectData(String version, ProjectPreference preference) {
        this(version, preference, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }
}
