package project;

import TheCellBeyond.InputAction;

import java.util.*;

/**
 * ProjectData is a record for a game project's manifest file.
 * @param version the manifest version number
 * @param uuid the project's UUID
 * @param project the project's preferences
 * @param assets the imported single sprite records
 * @param sheets the imported sprite sheet records
 * @param scenes the imported scene records
 * @param inputActions the imported input actions
 * @param physicLayers the physic layers' custom name
 * @param scriptScanDirs the directories to scan for user's scripts
 */
public record ProjectData(
        int version, UUID uuid, ProjectPreference project,
        Map<String, Map<String, ProjectAsset>> assets,
        Map<String, Map<String, ProjectSheet>> sheets,
        Map<String, ProjectScene> scenes,
        Map<String, InputAction> inputActions,
        PhysicLayerName physicLayers,
        List<String> scriptScanDirs
) {
    /**
     * The current project file schema version. Files without version are treated as version 0.
     */
    static final int SaveVersion = 2;
    /**
     * The YAML key that store the project file schema version number.
     */
    static final String VersionKey = "version";
    /**
     * The YAML key that store the project's uuid.
     */
    static final String UUIDKey = "uuid";
    private static final List<String> DefaultScriptScanDir = List.of("scripts");

    public ProjectData {
        if (uuid == null) uuid = UUID.randomUUID();
        if (project == null) project = new ProjectPreference();
        if (assets == null) assets = new HashMap<>();
        if (sheets == null) sheets = new HashMap<>();
        if (scenes == null) scenes = new HashMap<>();
        if (inputActions == null) inputActions = new HashMap<>();
        if (physicLayers == null) physicLayers = new PhysicLayerName();
        if (scriptScanDirs == null) scriptScanDirs = new ArrayList<>(DefaultScriptScanDir);
    }

    public ProjectData(ProjectPreference preference) {
        this(SaveVersion, null, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName(), new ArrayList<>(DefaultScriptScanDir));
    }

    public ProjectData(int version, ProjectPreference preference) {
        this(version, null, preference, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new PhysicLayerName(), new ArrayList<>(DefaultScriptScanDir));
    }
}
