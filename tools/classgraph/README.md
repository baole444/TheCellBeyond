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

| Property              | Default                                         | Use                                |
|-----------------------|-------------------------------------------------|------------------------------------|
| `graphRoots`          | `TheCellBeyond.GameObject,components.Component` | Root type(s) to draw subtrees from |
| `graphExclude`        | `editor`                                        | Package(s) to omit                 |
| `graphExcludeClasses` | `components.CrashComponent`                     | Class FQN(s) to omit               |
| `graphDpi`            | `150`                                           | Image resolution                   |
| `graphSize`           | `120`                                           | GraphViz size bound                |

Example:
```bash
./gradlew generateClassHierarchyGraph -PgraphRoots=scene.Scene -PgraphDpi=300 -PgraphExclude=editor,eventviewer -PgraphExcludeClasses=components.CrashComponent,components.FooComponent
```
