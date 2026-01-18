package project;

import TheCellBeyond.InputAction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ProjectData(
        String version, ProjectPreference project,
        Map<UUID, ProjectAssetMap> assets,
        Map<String, Map<String, ProjectSheetMap>> sheets,
        Map<String, ProjectSceneMap> scenes,
        Map<String, InputAction> inputActions,
        PhysicLayerName physicLayers
) {
    public ProjectData {
        if (project == null) project = new ProjectPreference();
        if (assets == null) assets = new HashMap<>();
        if (sheets == null) sheets = new HashMap<>();
        if (scenes == null) scenes = new HashMap<>();
        if (inputActions == null) inputActions = new HashMap<>();
        if (physicLayers == null) physicLayers = new PhysicLayerName();
    }

    public ProjectData(String version, ProjectPreference preference) {
        this(version, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName());
    }
}
