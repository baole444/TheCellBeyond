# The Cell Beyond Engine

A 2D game engine for Java application.

## Target goal:
A Java-based game editor with project compile ability, code auto-generation, and scripting support.<br>

*Note: code auto-generation is not AI code suggestion, this is referring to User's project auto build system.*

## Tasks:
- [X] Basic engine functions.
- [X] ImGui implementation.
- [X] Improve internal API and resolve conflicts.
- [ ] Editor features. (Currently being worked on.)
- [ ] Code template.
- [ ] Scripting system.

## Problems need attention:
- Saving while in test run (runtime mode) will cause override on scene's file.
- Upon creating a new project, create an object and press play immediately without creating the first scene, popup failed to prevent entering runtime mode, cause lost of scene data.

## Solved problems:
- N/A

## Additional maintenance:
- Update ImGui to [latest release](https://github.com/SpaiR/imgui-java/releases).
- Maintain compatibility of the project loading system.

## Current work:
- [ ] Implement API for Sound effect.

### Finished work:
This is a list of finished work and is now in maintaining state:
<details>
    <summary>Past works</summary>

- [X] Tile Map and Tile Set API.
- [X] Project system.
- [X] Unified Path System.
- [X] GameObject hierarchy structure.
- [X] Reimplementation of Scene Tree.
- [X] Investigating Scripting Engine support.
- [X] Migration to the new object UUID system.
- [X] Native Filed Dialog implementation.
- [X] Reimplementation of object properties panel and component addition/deletion workflow
- [X] Add a way to save a project.
- [X] Dynamically loaded assets.
- [X] Implement API for StateEngine

</details>

*To be continued*