package scripting.builder.jdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JDKManagerTest {
    @Test
    public void scanPopulatesUserAddedFromPaths(@TempDir Path dir) throws IOException {
        Path jdk = fakeJdk(dir, "alpha");
        JDKManager.scan(List.of(jdk)).join();
        assertTrue(containsHome(JDKManager.userAdded(), jdk), "scanned user path appears in the user-added list");
    }

    @Test
    public void addCachesValidJdkAndRejectsInvalid(@TempDir Path dir) throws IOException {
        JDKManager.scan(List.of()).join();
        Path jdk = fakeJdk(dir, "beta");
        JDKInstallation added = JDKManager.add(jdk).join();
        assertTrue(added.valid());
        assertTrue(containsHome(JDKManager.userAdded(), jdk));
        Path notJdk = Files.createDirectories(dir.resolve("empty"));
        JDKInstallation invalid = JDKManager.add(notJdk).join();
        assertFalse(invalid.valid(), "a non-JDK directory is rejected");
        assertFalse(containsHome(JDKManager.userAdded(), notJdk), "rejected directory is not cached");
    }

    @Test
    public void removeDropsUserAddedEntry(@TempDir Path dir) throws IOException {
        Path jdk = fakeJdk(dir, "gamma");
        JDKManager.scan(List.of(jdk)).join();
        assertTrue(containsHome(JDKManager.userAdded(), jdk));
        JDKManager.remove(jdk).join();
        assertFalse(containsHome(JDKManager.userAdded(), jdk));
    }

    private static boolean containsHome(List<JDKInstallation> list, Path home) {
        Path target = home.toAbsolutePath().normalize();
        return list.stream().anyMatch(jdk -> jdk.home() != null && jdk.home().toAbsolutePath().normalize().equals(target));
    }

    private static Path fakeJdk(Path dir, String name) throws IOException {
        Path home = dir.resolve(name);
        Path bin = home.resolve("bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve("javac.exe"), "", StandardCharsets.UTF_8);
        Files.writeString(home.resolve("release"), "JAVA_VERSION=\"" + JDKDetector.RequiredVersion + ".0.1\"\n", StandardCharsets.UTF_8);
        return home;
    }
}
