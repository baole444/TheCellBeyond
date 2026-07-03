package scripting.builder.jdk.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class QuarantineRemoverTest {
    @Test
    @EnabledOnOs(OS.MAC)
    public void stripRemovesQuarantineAttributeRecursively(@TempDir Path dir) throws Exception {
        Path home = Files.createDirectories(dir.resolve("jdk-25").resolve("bin"));
        Path binary = Files.writeString(home.resolve("java"), "", StandardCharsets.UTF_8);
        run("xattr", "-w", "com.apple.quarantine", "0081;00000000;test;", binary.toString());
        assertTrue(hasQuarantine(binary), "precondition: attribute was set");

        QuarantineRemover.strip(dir.resolve("jdk-25"));

        assertFalse(hasQuarantine(binary), "quarantine attribute should be cleared from the extracted tree");
    }

    private static boolean hasQuarantine(Path file) throws Exception {
        Process process = new ProcessBuilder("xattr", "-p", "com.apple.quarantine", file.toString()).redirectErrorStream(true).start();
        return process.waitFor() == 0;
    }

    private static void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        assertEquals(0, process.waitFor(), "command failed: " + String.join(" ", command));
    }
}
