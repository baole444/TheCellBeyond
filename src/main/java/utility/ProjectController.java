package utility;

import eventviewer.IEvent;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import org.yaml.snakeyaml.Yaml;

public class ProjectController implements IEvent {
    private Yaml yaml = new Yaml();
    private String projectPath = new String();

    // TODO: Read a path passed in from event system, read the yaml key. Might need to create a file layout for validation.
    // Read this site for more information https://www.baeldung.com/java-snake-yaml

    @Override
    public void whenNotice(Object object, Event event) {
        if (event.type.equals(EventType.LoadProject)) {

        }
    }
}
