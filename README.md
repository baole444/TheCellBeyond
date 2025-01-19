# The Cell Beyond Engine

A simple 2D (and maybe 3D) graphic engine for Java application.

## Target goal:
A Java-based game editor with project compile ability, code auto-generation, and scripting support.

## Tasks:
- [X] Basic engine functions.
- [X] ImGui implementation.
- [ ] Editor features. (Currently being worked on.)
- [ ] Code template.
- [ ] Scripting system.


## Problems need attention:
Currently, not detecting any unusual problem.

## Solved problems:
- [X] When placing an object, there is ghosting left behind.
- [X] When deleting an object, the deleted object is not displayed correctly.

Both mentioned problems require user to reload the file. Suspected to be caused by leftover object in memory.

__Update:__ The incorrectly removed item is caused by render batch mark incorrect sprite for update call.


## Additional maintenance:
- Update ImGui to [latest release](https://github.com/SpaiR/imgui-java/releases).
- Maintain compatibility of the project loading system.

## Current work:
- [X] Project system.
- [ ] Add a way to save a project.
- [ ] Extend the Project system to cover animation and sound effects.
- [ ] Dynamically loaded assets.


*To be continued*
