package eventviewer.event;

public enum EditorEvent {
    EngineStart,
    EngineStop,

    SaveEditingScene,
    LoadEditingScene,

    LoadProject,
    ProjectLoaded,

    CreateNewScene,
    LoadSceneData,
    ReloadSceneResource,

    UserEvent,
}
