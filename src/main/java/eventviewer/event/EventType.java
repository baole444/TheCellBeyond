package eventviewer.event;

public enum EventType {
    ENGINE_START,
    ENGINE_END,

    LEVEL_SAVE,
    LEVEL_LOAD,

    PROJECT_LOAD,

    SCENE_NEW,
    SCENE_LOAD,
    SCENE_RELOAD_RESOURCE,

    UserEvent,
}
