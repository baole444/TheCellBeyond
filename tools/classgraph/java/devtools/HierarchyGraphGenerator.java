package devtools;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HierarchyGraphGenerator {
    private static final String ExcludedPackageKey = "graph.excludePackages";
    private static final String DefaultExcludedPackage = "editor";
    private static final String ExcludedClassKey = "graph.excludeClasses";
    private static final String DefaultExcludedClass = "components.CrashComponent";
    private static final String GraphSizeKey = "graph.size";
    private static final String DefaultGraphSize = "120";
    private static final String GraphDPIKey = "graph.dpi";
    private static final String DefaultGraphDPI = "150";

    private HierarchyGraphGenerator() {}

    static void main(String[] args) throws IOException {
        if (args.length < 3) throw new IllegalArgumentException("Usage: HierarchyGraphGenerator <classesDirsPath> <outputDir> <rootFqn> [<rootFqn> ...]");
        List<String> classpath = Arrays.asList(args[0].split(File.pathSeparator));
        Path outputDir = Path.of(args[1]);
        List<String> roots = Arrays.asList(args).subList(2, args.length);
        Files.createDirectories(outputDir);
        boolean dotAvailable = dotAvailable();
        ClassGraph classGraph = new ClassGraph().overrideClasspath(classpath).enableAllInfo().enableExternalClasses();
        try (ScanResult scan = classGraph.scan()) {
            for (String root : roots) writeGraph(scan, root, outputDir, dotAvailable);
        }
        if (dotAvailable) return;
        System.out.println("GraphViz is not on system PATH, skipping image generation");
    }

    private static void writeGraph(ScanResult scan, String rootFQN, Path outputDir, boolean dotAvailable) throws IOException {
        ClassInfo root = scan.getClassInfo(rootFQN);
        if (root == null) {
            System.err.println("Skipping unknown root type: " + rootFQN);
            return;
        }
        ClassInfoList subtree = scan.getSubclasses(rootFQN);
        if (!subtree.contains(root)) subtree.add(root);
        List<String> excludedPackages = excludedPackages();
        List<String> excludedClasses = excludedClasses();
        ClassInfoList visible = subtree.filter(ci -> !excluded(ci.getName(), excludedPackages) && !excludedClasses.contains(ci.getName()));
        String baseName = root.getSimpleName() + "-hierarchy";
        Path dotFile = outputDir.resolve(baseName + ".dot");
        float size = graphSize();
        String dot = visible.generateGraphVizDotFile(size, size, false, false, false, false, false, false);
        Files.writeString(dotFile, dot, StandardCharsets.UTF_8);
        if (!dotAvailable) return;
        render(dotFile, outputDir.resolve(baseName + ".png"), "png");
        render(dotFile, outputDir.resolve(baseName + ".svg"), "svg");
    }

    private static void render(Path dotFile, Path output, String format) {
        try {
            Process process = new ProcessBuilder("dot", "-T" + format, "-Gdpi=" + graphDPI(), dotFile.toString(), "-o", output.toString()).inheritIO().start();
            if (process.waitFor() != 0) System.err.println("Failed to render " + output);
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to render " + output + ": " + e.getMessage());
        }
    }

    private static float graphSize() {
        return Float.parseFloat(System.getProperty(GraphSizeKey, DefaultGraphSize));
    }

    private static int graphDPI() {
        return Integer.parseInt(System.getProperty(GraphDPIKey, DefaultGraphDPI));
    }

    private static List<String> excludedPackages() {
        return properties(ExcludedPackageKey, DefaultExcludedPackage);
    }

    private static List<String> excludedClasses() {
        return properties(ExcludedClassKey, DefaultExcludedClass);
    }

    private static List<String> properties(String key, String defaultValue) {
        List<String> values = new ArrayList<>();
        for (String entry : System.getProperty(key, defaultValue).split(",")) {
            entry = entry.trim();
            if (!entry.isEmpty()) values.add(entry);
        }
        return values;
    }

    private static boolean excluded(String fqn, List<String> packagePrefixes) {
        for (String prefix : packagePrefixes) {
            if (fqn.equals(prefix) || fqn.startsWith(prefix + ".")) return true;
        }
        return false;
    }

    private static boolean dotAvailable() {
        try {
            Process process = new ProcessBuilder("dot", "-V").redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException _) {
            return false;
        }
    }
}
