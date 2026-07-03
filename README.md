<div style="text-align: center;" align="center">

# TheCellBeyond Game Engine

**A 2D game engine in Java, with scripting support and editor UI for building scenes and levels**

</div>

## Running the engine
The executable from release contains a bundled JRE needed to run the core engine and potential prebuilt script jar file.

The generated script project come with Gradle wrapper, but still require [JDK 25](https://adoptium.net/temurin/releases?version=25&os=any&arch=any) in order to jar the script.

### macOS
Due to lack of signing, the engine file on macOS might be blocked from executing.
This can be bypassed by granting the app permission in terminal:
```
xattr -cr /path/to/TheCellBeyond_<version>.app
```
Alternatively, right click and choose **Open** and confirm on the dialogue to bypass restriction.

## Scripting
The engine's API is available on [Maven central repository](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api), 
which had been marked as `compileOnly` in the [build.gradle](https://github.com/baole444/TheCellBeyond/blob/Dev-build/src/main/resources/templates/script-project/build.gradle) 
generated for scripting project.

For any other dependencies that might be brought in, they need to be `implementation` instead of `compileOnly`.

### Compatibility
Below is the minimum API version for scripting to be compatible with the engine's release version:

| Engine version  |                                        API version                                        |
|:---------------:|:-----------------------------------------------------------------------------------------:|
| __1.8 - 1.8.1__ | [__1.8__](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.8) |
|       1.7       |   [1.7](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.7)   |
|   1.6 - 1.6.1   |   [1.6](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.6)   |
|   1.5 - 1.5.2   |   [1.5](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.5)   |
|       1.4       |   [1.4](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.4)   |

*Using any version older than __1.8.1__ might not work properly (The version mention in this increased if new release contain fix for crashes or incorrect logic.)*

Annotate a class as GameObject type:
```java
import components.AnimatedSpriteRenderer;
import physic2d.CharacterBody2D;
import scripting.RegisterGameObject;
import utility.HierarchyPaths;

@RegisterGameObject(label = "Player Object", description = "Main player controlled object")
public class MainPlayer extends CharacterBody2D {
    public AnimatedSpriteRenderer animation2D;
    
    @Override
    protected void onReady() {
        animation2D = (AnimatedSpriteRenderer) HierarchyPaths.toComponent("::Animation2D", this);
    }
    
    @Override
    protected void onPhysicUpdate(float dt) {
        // Other logic you might have
        moveAndSlide();
    }
}
```

Annotate a class as Component type:
```java
import components.StateEngine;
import scripting.RegisterComponent;

@RegisterComponent(label = "Main char state engine", description = "State engine for main character")
public class MainStateEngine extends StateEngine {
    @Override
    protected void onReady() {
        switchState("Idle");
    }
}
```

Annotate a field with Export for editing in the editor UI:
```java
import org.joml.Vector2f;
import physic2d.CharacterBody2D;
import scripting.RegisterGameObject;
import scripting.Export;
import scripting.TypeHint;

@RegisterGameObject(label = "Player Object", description = "Main player controlled object")
public class MainPlayer extends CharacterBody2D {
    public enum Direction {
        Left(-1),
        Right(1);
        
        public final int x;
        
        Direction(int x) {
            this.x = x;
        }
    }
    
    @Export
    public float movementSpeed = 2.0f;
    
    @Export(label = "Custom label",  description = "Custom field", type = TypeHint.Vector2)
    public Vector2f currentDirection = new Vector2f();
    
    @Export(label = "Starting direction")
    public Direction startingDir = Direction.Right;
}
```
For most cases, defining type for the exporting variables is optional, 
unless the editor's inspector is recognizing something incorrectly.
