package project;

import TheCellBeyond.InputAction;

import java.util.*;

public record ProjectData(
        int version, ProjectPreference project,
        Map<UUID, ProjectAssetMap> assets,
        Map<String, Map<String, ProjectSheetMap>> sheets,
        Map<String, ProjectSceneMap> scenes,
        Map<String, InputAction> inputActions,
        PhysicLayerName physicLayers,
        List<String> scriptScanDirs
) {
    /**
     * The current project file schema version. Files without version are treated as version 0.
     */
    static final int SaveVersion = 1;
    /**
     * The YAML key that store the project file schema version number.
     */
    static final String VersionKey = "version";
    private static final List<String> DefaultScriptScanDir = List.of("scripts");

    public ProjectData {
        if (project == null) project = new ProjectPreference();
        if (assets == null) assets = new HashMap<>();
        if (sheets == null) sheets = new HashMap<>();
        if (scenes == null) scenes = new HashMap<>();
        if (inputActions == null) inputActions = new HashMap<>();
        if (physicLayers == null) physicLayers = new PhysicLayerName();
        if (scriptScanDirs == null) scriptScanDirs = new ArrayList<>(DefaultScriptScanDir);
    }

    public ProjectData(ProjectPreference preference) {
        this(SaveVersion, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName(), new ArrayList<>(DefaultScriptScanDir));
    }

    public ProjectData(int version, ProjectPreference preference) {
        this(version, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName(), new ArrayList<>(DefaultScriptScanDir));
    }
}
