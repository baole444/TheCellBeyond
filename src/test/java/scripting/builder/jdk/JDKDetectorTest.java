package scripting.builder.jdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class JDKDetectorTest {
    private static final int Required = JDKDetector.RequiredVersion;

    @Test
    public void inspectReadsVendorAndVersionFromRelease(@TempDir Path dir) throws IOException {
        Path home = fakeJdk(dir, Required + ".0.1", "Eclipse Adoptium");
        JDKInstallation jdk = JDKDetector.inspect(home, JDKInstallation.Source.UserAdded);
        assertTrue(jdk.valid());
        assertEquals(Required, jdk.featureVersion());
        assertEquals("Eclipse Adoptium", jdk.vendor());
        assertEquals(home, jdk.home());
    }

    @Test
    public void inspectRejectsJreWithoutJavac(@TempDir Path dir) throws IOException {
        Path home = dir.resolve("jre");
        Files.createDirectories(home);
        writeRelease(home, Required + ".0.1", "Eclipse Adoptium");
        JDKInstallation jdk = JDKDetector.inspect(home, JDKInstallation.Source.UserAdded);
        assertFalse(jdk.valid(), "a JRE (no bin/javac) is not a usable JDK");
        assertEquals(JDKInstallation.MissingLabel, jdk.displayName());
    }

    @Test
    public void inspectRejectsBelowRequiredVersion(@TempDir Path dir) throws IOException {
        Path home = fakeJdk(dir, (Required - 1) + ".0.2", "Eclipse Adoptium");
        JDKInstallation jdk = JDKDetector.inspect(home, JDKInstallation.Source.UserAdded);
        assertFalse(jdk.valid());
    }

    @Test
    public void inspectRejectsLegacyVersionString(@TempDir Path dir) throws IOException {
        Path home = fakeJdk(dir, "1.8.0_392", "Oracle Corporation");
        JDKInstallation jdk = JDKDetector.inspect(home, JDKInstallation.Source.UserAdded);
        assertFalse(jdk.valid(), "1.8 parses to feature 8, below the required version");
    }

    @Test
    public void inspectResolvesMacOsContentsHome(@TempDir Path dir) throws IOException {
        Path bundle = dir.resolve("temurin-25.jdk");
        Path realHome = bundle.resolve("Contents").resolve("Home");
        Files.createDirectories(realHome);
        writeJavac(realHome);
        writeRelease(realHome, Required + ".0.1", "Eclipse Adoptium");
        JDKInstallation jdk = JDKDetector.inspect(bundle, JDKInstallation.Source.UserAdded);
        assertTrue(jdk.valid());
        assertEquals(realHome, jdk.home(), "the Contents/Home layout is resolved to the real home");
    }

    @Test
    public void javaHomeDisplayNameIsPrefixed(@TempDir Path dir) throws IOException {
        Path home = fakeJdk(dir, Required + ".0.1", "Azul Systems, Inc.");
        JDKInstallation jdk = JDKDetector.inspect(home, JDKInstallation.Source.JavaHome);
        assertTrue(jdk.displayName().startsWith("JAVA_HOME "));
        assertTrue(jdk.displayName().contains("Azul Systems, Inc."));
    }

    @Test
    public void parseProbeVersionReadsFeatureVersionFromOutput() {
        assertEquals(25, JDKDetector.parseReadVersion("openjdk version \"25.0.1\" 2025-10-21\nOpenJDK Runtime Environment Temurin-25.0.1+9"));
        assertEquals(25, JDKDetector.parseReadVersion("java version \"25\" 2025-09-16"));
        assertEquals(8, JDKDetector.parseReadVersion("java version \"1.8.0_392\""));
        assertEquals(-1, JDKDetector.parseReadVersion("no version information here"));
    }

    @Test
    public void probeVersionInvokesRunningJdk() {
        Path realHome = Path.of(System.getProperty("java.home"));
        Path bin = realHome.resolve("bin");
        boolean runnable = Files.isRegularFile(bin.resolve("java")) || Files.isRegularFile(bin.resolve("java.exe"));
        assumeTrue(runnable, "test JVM home must expose a runnable bin/java");
        assertEquals(Runtime.version().feature(), JDKDetector.readVersion(realHome), "probe reports the running JDK feature version");
    }

    private static Path fakeJdk(Path dir, String javaVersion, String vendor) throws IOException {
        Path home = dir.resolve("jdk");
        Files.createDirectories(home);
        writeJavac(home);
        writeRelease(home, javaVersion, vendor);
        return home;
    }

    private static void writeJavac(Path home) throws IOException {
        Path bin = home.resolve("bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve("javac.exe"), "", StandardCharsets.UTF_8);
    }

    private static void writeRelease(Path home, String javaVersion, String vendor) throws IOException {
        String content = "IMPLEMENTOR=\"" + vendor + "\"\nJAVA_VERSION=\"" + javaVersion + "\"\n";
        Files.writeString(home.resolve("release"), content, StandardCharsets.UTF_8);
    }
}
