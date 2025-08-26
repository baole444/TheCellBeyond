package editor.project;

import java.util.Map;

public record ProjectData(
        String version, ProjectPreference project,
        Map<String, ProjectAssetMap>assets,
        Map<String, ProjectSheetMap> sheets,
        Map<String, ProjectSceneMap> scenes
) {}
