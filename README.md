# The Cell Beyond Engine

A 2D game engine for Java application.

## Target goal:
A Java-based game editor with project compile ability, code auto-generation, and scripting support.<br>

*Note: code auto-generation is not AI code suggestion, this is referring to User's project auto build system.*

## Tasks:
- [X] Basic engine functions.
- [X] ImGui implementation.
- [ ] Improve internal API and resolve conflicts.
- [ ] Editor features. (Currently being worked on.)
- [ ] Code template.
- [ ] Scripting system.


## Problems need attention:
- Potential concurrent exception with GameObject operation in Scene's queueing lists.

## Solved problems:
- The Scene tree adds two entries per game object, 
which was caused by an unchecked addition after loading scene.

## Additional maintenance:
- Update ImGui to [latest release](https://github.com/SpaiR/imgui-java/releases).
- Maintain compatibility of the project loading system.

## Current work:
- [X] Project system.
- [X] Unified Path System.
- [X] GameObject hierarchy structure.
- [X] Reimplementation of Scene Tree.
- [X] Investigating Scripting Engine support.
- [X] Migration to the new object UUID system.
- [X] Native Filed Dialog implementation.
- [ ] Add a way to save a project.
- [ ] Extend the Project system to cover animation and sound effects.
- [ ] Dynamically loaded assets.

*To be continued*