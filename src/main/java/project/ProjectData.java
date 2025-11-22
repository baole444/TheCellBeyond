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
    public ProjectData(String version, ProjectPreference preference) {
        this(version, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName());
    }
}
