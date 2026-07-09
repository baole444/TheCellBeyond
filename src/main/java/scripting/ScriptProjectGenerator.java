package scripting;

import utility.UnifiedPaths;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptProjectGenerator {
    private static final EngineLog Logger = new EngineLog(ScriptProjectGenerator.class);
    private static final String TemplatePath = "engine://templates/script-project/";
    private static final String[] TemplateFiles = {
            "build.gradle", "settings.gradle", "gradle.properties",
            "gradlew", "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties",
            "library/jbox2d-library.jar"
    };
    private static final String[][] DotFiles = {
            {"gitignore", ".gitignore"}
    };

    private ScriptProjectGenerator() {}

    public static boolean generate(String projectRoot) {
        if (projectRoot == null) return false;
        Path scriptSource = Path.of(projectRoot, "scripts-src");
        try {
            Files.createDirectories(scriptSource.resolve("src/main/java"));
            Files.createDirectories(scriptSource.resolve("src/script"));
        } catch (IOException e) {
            Logger.error(String.format("Failed to create script project directories: %s", e.getMessage()));
            return false;
        }
        for (String file : TemplateFiles) copyTemplateIfAbsent(TemplatePath + file, scriptSource.resolve(file));
        for (String[] dotFile: DotFiles) copyTemplateIfAbsent(TemplatePath + dotFile[0], scriptSource.resolve(dotFile[1]));
        Logger.info(String.format("Script project generated at %s", scriptSource));
        return true;
    }

    private static void copyTemplateIfAbsent(String resourcePath, Path target) {
        if (Files.exists(target)) {
            Logger.debug(String.format("Skipping existing file: %s", target.getFileName()));
            return;
        }
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            Logger.error(String.format("Failed to create directory for %s: %s", target, e.getMessage()));
            return;
        }
        try (InputStream stream = UnifiedPaths.getAssetStream(resourcePath)) {
            Files.copy(stream, target);
            if (target.getFileName().toString().equals("gradlew") && !target.toFile().setExecutable(true)) {
                Logger.debug("Cannot adjust permission for gradlew, or permission is not supported on this OS");
            }
        } catch (IOException e) {
            Logger.error(String.format("Failed to copy template %s: %s", target.getFileName(), e.getMessage()));
        }
    }
}
