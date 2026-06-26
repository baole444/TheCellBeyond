## Class hierarchy visualisation tool

This is a standalone tool using `ClassGraph` and `GraphViz` to generate hierarchy tree of the given class.

Run with default parameters (generate for `GameObject` and `Component` hierarchy trees):
```bash
./gradlew generateClassHierarchyGraph
```

### Requirements
- [GraphViz](https://www.graphviz.org/download/) `dot` CLI on PATH for PNG/SVG generation.

### Options
* Parameters are separated using commas

| Property              | Default                                         | Use                                               |
|-----------------------|-------------------------------------------------|---------------------------------------------------|
| `graphRoots`          | `TheCellBeyond.GameObject,components.Component` | Root type(s) to draw subtrees from                |
| `graphExclude`        | `editor`                                        | Package(s) to omit                                |
| `graphExcludeClasses` | `components.CrashComponent`                     | Class FQN(s) to omit                              |
| `graphDpi`            | `150`                                           | Image resolution                                  |
| `graphSize`           | `120`                                           | GraphViz size bound                               |
| `grapRankSep`         | `0.75`                                          | Vertical spacing between hierarchy level (inches) |

Example:
```bash
./gradlew generateClassHierarchyGraph -PgraphRoots=scene.Scene -PgraphDpi=300 -PgraphExclude=editor,eventviewer -PgraphExcludeClasses=components.CrashComponent,components.FooComponent
```

> [!NOTE]
> On Windows PowerShell: wrap each parameter section in a double quote `""`.
> For example:
> ```powershell
> ./gradlew generateClassHierarchyGraph "-PgraphRoots=scene.Scene" "-PgraphDpi=300" "-PgraphRankSep=1.2" "-PgraphExclude=editor,eventviewer" "-PgraphExcludeClasses=components.CrashComponent,components.FooComponent"
> ```
