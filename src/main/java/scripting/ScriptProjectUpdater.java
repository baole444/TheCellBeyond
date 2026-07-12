package scripting;

import scripting.transpiler.manifest.APIManifest;
import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScriptProjectUpdater {
    private static final EngineLog Logger = new EngineLog(ScriptProjectUpdater.class);
    private static final String TemplatePath = "engine://templates/script-project/";
    private static final String ApiVersionKey = "TCB_API_version";
    /**
     * Pattern to match against the old hardcoded dependency version.
     */
    private static final Pattern HardcodeDependency = Pattern.compile("(io\\.github\\.baole444:thecellbeyond-api:)([^\"'$]+)");
    private static final String[] WrapperFiles = {
            "gradlew", "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties"
    };

    private ScriptProjectUpdater() {}

    public static boolean update(String projectRoot) {
        if (projectRoot == null) return false;
        Path scriptSource = Path.of(projectRoot, "scripts-src");
        if (!Files.isDirectory(scriptSource)) {
            Logger.error(String.format("No script project found at %s, create or generate one first", scriptSource));
            return false;
        }
        for (String file : WrapperFiles) overrideTemplate(TemplatePath + file, scriptSource.resolve(file));
        updateAPIVersion(scriptSource.resolve("gradle.properties"));
        updateBuildScriptDependency(scriptSource.resolve("build.gradle"));
        Logger.info(String.format("Script project updated at %s to API version %s", scriptSource, APIManifest.apiVersion()));
        return true;
    }

    private static void updateAPIVersion(Path propertiesFile) {
        if (!Files.exists(propertiesFile)) overrideTemplate(TemplatePath + "gradle.properties", propertiesFile);
        String version = APIManifest.apiVersion();
        try {
            List<String> lines = Files.readAllLines(propertiesFile, StandardCharsets.UTF_8);
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (!onAPIKeyLine(lines.get(i))) continue;
                lines.set(i, ApiVersionKey + " = " + version);
                replaced = true;
                break;
            }
            if (!replaced) lines.add(ApiVersionKey + " = " + version);
            Files.write(propertiesFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error(String .format("Failed to update API version in %s: %s", propertiesFile.getFileName(), e.getMessage()));
        }
    }

    private static void overrideTemplate(String resourcePath, Path target) {
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            Logger.error(String.format("Failed to create directory for %s: %s", target, e.getMessage()));
            return;
        }
        try (InputStream stream = UnifiedPaths.getAssetStream(resourcePath)) {
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            if (target.getFileName().toString().equals("gradlew") && target.toFile().setExecutable(true)) {
                Logger.debug("Cannot adjust permission for gradlew, or permission is not supported on this OS");
            }
        } catch (IOException e) {
            Logger.error(String.format("Failed to override template %s: %s", target.getFileName(), e.getMessage()));
        }
    }

    private static boolean onAPIKeyLine(String line) {
        line = line.trim();
        if (line.startsWith("#")) return false;
        int equal = line.indexOf('=');
        if (equal < 0) return false;
        return line.substring(0, equal).trim().equals(ApiVersionKey);
    }

    private static void updateBuildScriptDependency(Path buildScript) {
        if (!Files.exists(buildScript)) return;
        try {
            String content = Files.readString(buildScript, StandardCharsets.UTF_8);
            Matcher matcher = HardcodeDependency.matcher(content);
            String updated = matcher.replaceAll(match -> Matcher.quoteReplacement(match.group(1) + "$" + ApiVersionKey));
            if (updated.equals(content)) return;
            Files.writeString(buildScript, updated, StandardCharsets.UTF_8);
            Logger.debug("Normalized API dependency version in build script");
        } catch (IOException e) {
            Logger.error(String.format("Failed to update API dependency in %s: %s", buildScript.getFileName(), e.getMessage()));
        }
    }
}
