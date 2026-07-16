package generator;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Compute the transitive public and protected signature closure of the annotated API types and emits the class file include list for the API jar.
 * <p>
 * From each annotated type, walk the supertype and interface chains, along with types on every public or protected member signature, field types, constructor and method params/return/throws types, etc.
 * Engine types reached this way become the include list, third party types are external surface, checked against the declared and provisioned lists.
 * </p>
 * Declaration annotations are ignored, allow the API annotation itself to be excluded from inclusion.
 * Runtime annotation such as Export and Register are still shipped regardless.
 */
public final class APISurfaceGenerator {
    private static final List<String> JDKPrefixes = List.of("java.", "javax.", "jdk.", "sun.", "com.sun.");
    /**
     * The API seed marker type to exclude.
     */
    static final String APIMarkerType = "scripting.API";

    private APISurfaceGenerator() {}

    static void main(String[] args) throws IOException {
        Config config = Config.parse(args);
        ClassGraph classGraph = new ClassGraph().overrideClasspath(config.classpath).enableAllInfo().enableExternalClasses();
        try (ScanResult scan = classGraph.scan()) {
            run(scan, config);
        }
    }

    private static void run(ScanResult scan, Config config) throws IOException {
        Set<String> engineClasses = new HashSet<>();
        for (ClassInfo info : scan.getAllClasses()) if (!info.isExternalClass()) engineClasses.add(info.getName());
        ClassInfoList seeds = scan.getClassesWithAnnotation(config.seedAnnotation);
        if (seeds.isEmpty()) throw new IllegalStateException("Empty API seed, no type annotated with " + config.seedAnnotation);
        Closure closure = new Closure(scan, engineClasses);
        seeds.forEach(seed -> closure.enqueue(seed.getName(), "@API seed"));
        closure.walk();
        List<String> failures = new ArrayList<>();
        List<String> unresolvedExternals = new ArrayList<>();
        boolean jbox2DOnSurface = false;
        for (Map.Entry<String, TreeSet<String>> entry : closure.externals.entrySet()) {
            String type = entry.getKey();
            if (jdkType(type)) continue;
            if (matchesPrefix(type, config.declared)) continue;
            if (matchesPrefix(type, config.provisioned)) {
                jbox2DOnSurface = true;
                continue;
            }
            unresolvedExternals.add(type);
        }
        if (!unresolvedExternals.isEmpty()) {
            unresolvedExternals.sort(Comparator.naturalOrder());
            failures.add("Third party types on the API surface are neither declared in the POM nor provisioned externally:");
            for (String type : unresolvedExternals) failures.add(String.format("\t%s <- %s", type, String.join(", ", closure.externals.get(type))));
        }
        List<String> missingJars = new ArrayList<>();
        if (jbox2DOnSurface) {
            for (String jar : config.provisionedJars) {
                if (Files.exists(Path.of(jar))) continue;
                missingJars.add(jar);
                failures.add("Provisioned dependency is on the API surface but not shipped: missing " + jar);
            }
        }
        StringBuilder report = new StringBuilder();
        report.append("API surface closure report\n")
                .repeat("=", 16).append("\n\n")
                .append(String.format("Engine classes scanned: %d%n", engineClasses.size()))
                .append(String.format("Seeds API: %d%n", seeds.size()))
                .append(String.format("Closure size: %d%n", closure.included.size()))
                .append(String.format("External types: %d%n", closure.externals.size()))
                .append(String.format("Unresolved externals: %d%n%n", unresolvedExternals.size()))
                .append("Seeds:\n");
        seeds.getNames().stream().sorted().forEach(n -> report.append(String.format("\t%s%n", n)));
        report.append("\nExternal types on the public surface (referrers):\n");
        for (Map.Entry<String, TreeSet<String>> entry : closure.externals.entrySet()) {
            String kind = classify(entry.getKey(), config);
            report.append(String.format("\t[%s] %s%n", kind, entry.getKey()));
            for (String referrer : entry.getValue()) report.append(String.format("\t\t<- %s%n", referrer));
        }
        if (jbox2DOnSurface) {
            report.append("\njbox2d leak points on the public surface:\n");
            for (Map.Entry<String, TreeSet<String>> entry : closure.externals.entrySet()) {
                if (!matchesPrefix(entry.getKey(), config.provisioned)) continue;
                for (String referrer : entry.getValue()) report.append(String.format("\t%s -> %s%n", referrer, entry.getKey()));
            }
        }
        Files.createDirectories(config.reportPath.getParent());
        Files.writeString(config.reportPath, report.toString(), StandardCharsets.UTF_8);
        if (!failures.isEmpty()) {
            failures.forEach(System.err::println);
            throw new IllegalStateException(String.format("API surface gate failed with %d problem, see %s", unresolvedExternals.size() + missingJars.size(), config.reportPath));
        }
        List<String> lines = new ArrayList<>(closure.included.size());
        for (String type : closure.included) lines.add(type.replace('.', '/'));
        Files.createDirectories(config.outPath.getParent());
        Files.writeString(config.outPath, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        System.out.printf("API surface closure: %d seeds -> %d classes (external: %d types); written to %s%n", seeds.size(), closure.included.size(), closure.externals.size(), config.outPath);
    }

    private static String classify(String type, Config config) {
        if (jdkType(type)) return "jdk";
        if (matchesPrefix(type, config.declared)) return "declared";
        if (matchesPrefix(type, config.provisioned)) return "provisioned";
        return "UNRESOLVED";
    }

    private static boolean jdkType(String type) {
        return matchesPrefix(type, JDKPrefixes);
    }

    private static boolean matchesPrefix(String type, List<String> prefixes) {
        for (String prefix : prefixes) if (type.startsWith(prefix)) return true;
        return false;
    }
}
