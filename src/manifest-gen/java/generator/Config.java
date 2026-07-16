package generator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Argument parsing with repeatable {@code --key value} pairs.
 */
final class Config {
    String classpath;
    Path outPath;
    Path reportPath;
    String seedAnnotation = APISurfaceGenerator.APIMarkerType;
    final List<String> declared = new ArrayList<>();
    final List<String> provisioned = new ArrayList<>();
    final List<String> provisionedJars = new ArrayList<>();
    private String outPathString;
    private String reportPathString;

    static Config parse(String[] args) {
        Config config = new Config();
        for (int i = 0; i < args.length - 1; i += 2) {
            String value = args[i + 1];
            switch (args[i]) {
                case "--classpath" -> config.classpath = value;
                case "--out" -> config.outPathString = value;
                case "--report" -> config.reportPathString = value;
                case "--seed-annotation" -> config.seedAnnotation = value;
                case "--declared" -> config.declared.add(value);
                case "--provisioned" -> config.provisioned.add(value);
                case "--provisioned-jar" -> config.provisionedJars.add(value);
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (config.classpath == null || config.outPathString == null || config.reportPathString == null) throw new IllegalArgumentException("Usage: APISurfaceGenerator --classpath <cp> --out <file> --report <file> [--seed-annotation <fqn>] [--declared <prefix>]... [--provisioned <prefix>]... [--provisioned-jar <path>]...");
        config.outPath = Path.of(config.outPathString);
        config.reportPath = Path.of(config.reportPathString);
        return config;
    }
}
