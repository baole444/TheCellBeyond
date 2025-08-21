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
- No proper resource clearing/saving when creating a new scene due to improper name check.
- No proper way to assign texture/sprite from a spritesheet to a SpriteRenderer component.

## Solved problems:
- If additional scaling from transform is applied, the rotation broke. Caused by broken math in GameObject2D.
- Due to lazy load there is no proper way to for hierarchy to actively update their effective transform.

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
- [ ] Reimplementation of object properties panel and component addition/deletion workflow
- [ ] Add a way to save a project.
- [ ] Extend the Project system to cover animation and sound effects.
- [ ] Dynamically loaded assets.

*To be continued*