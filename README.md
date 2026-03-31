<div style="text-align: center;">

## TheCellBeyond Game Engine

A 2D game engine for in Java with editor for building scenes and levels.
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
which had been marked as `compiledOnly` in the [build.gradle](https://github.com/baole444/TheCellBeyond/blob/Dev-build/src/main/resources/templates/script-project/build.gradle) 
generated for scripting project.

For any other dependencies that might be brought in, they need to be `implementation` instead of `compileOnly`.

### Compatibility
Below is the minimum API version for scripting to be compatible with the engine's release version:

| Engine version |                                          API version                                          |
|:--------------:|:---------------------------------------------------------------------------------------------:|
| 1.0.5 - 1.0.7  | [__1.0.5__](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.0.5) |
|   ~~1.0.4~~    | [~~1.0.4~~](https://central.sonatype.com/artifact/io.github.baole444/thecellbeyond-api/1.0.4) |

Using any version older than 1.0.7 might not work properly.

Annotate a class as GameObject type:
```java
import physic2d.CharacterBody2D;
import scripting.RegisterGameObject;

@RegisterGameObject(label = "Player Object", description = "Main player controlled object")
public class MainPlayer extends CharacterBody2D {
    @Override
    protected void onPhysicUpdate(float dt) {
        super.onPhysicUpdate(dt);
        
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
