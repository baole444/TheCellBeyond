package utility;

public class PathMigrationHelper {
    public static void migrateProjectAssets(String projectRoot) {
        PathResolver.initialize(projectRoot);
        PathResolver resolver = PathResolver.get();

        // TODO: in the future, scan all project cell file and update path if needed.
        //  The goal is to use the canonical path format instead of absolute paths.

    }
}
