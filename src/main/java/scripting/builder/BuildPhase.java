package scripting.builder;

public enum BuildPhase {
    Idle,
    Transpiling,
    Building,
    Succeeded,
    TranspilerFailed,
    BuildFailed
}
