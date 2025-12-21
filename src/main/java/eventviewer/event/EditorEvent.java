package eventviewer.event;

public enum EditorEvent {
    EngineStart,
    EngineStop,
    RuntimeCrashed,

    SaveEditingScene,
    LoadEditingScene,

    LoadProject,
    ProjectLoaded,

    CreateNewScene,
    LoadSceneData,
    ReloadSceneResource,

    UserEvent,
}
