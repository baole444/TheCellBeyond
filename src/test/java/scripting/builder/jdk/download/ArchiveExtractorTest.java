package scripting.builder.jdk.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

final class ArchiveExtractorTest {
    @Test
    public void extractUnzipsAndReturnsHome(@TempDir Path dir) throws Exception {
        Path archive = dir.resolve("jdk.zip");
        writeZip(archive,
                entry("jdk-25/", null),
                entry("jdk-25/bin/javac.exe", ""),
                entry("jdk-25/release", "JAVA_VERSION=\"25.0.3\"\n"));
        Path installDir = Files.createDirectories(dir.resolve("install"));
        Path home = ArchiveExtractor.extract(archive, installDir, "zip");
        assertEquals(installDir.resolve("jdk-25"), home);
        assertTrue(Files.isRegularFile(home.resolve("bin").resolve("javac.exe")));
        assertTrue(Files.isRegularFile(home.resolve("release")));
    }

    @Test
    public void extractRejectsPathTraversalEntries(@TempDir Path dir) throws Exception {
        Path archive = dir.resolve("evil.zip");
        writeZip(archive,
                entry("jdk-25/bin/javac.exe", ""),
                entry("../escaped.txt", "owned"));
        Path installDir = Files.createDirectories(dir.resolve("install"));
        ArchiveExtractor.extract(archive, installDir, "zip");
        assertFalse(Files.exists(installDir.getParent().resolve("escaped.txt")), "a ../ entry must not be written outside the install dir");
    }

    private static Entry entry(String name, String content) {
        return new Entry(name, content);
    }

    private static void writeZip(Path archive, Entry... entries) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Entry entry : entries) writeEntry(zip, entry);
        }
    }

    private static void writeEntry(ZipOutputStream zip, Entry entry) throws Exception {
        zip.putNextEntry(new ZipEntry(entry.name()));
        if (entry.content() != null) writeBytes(zip, entry.content());
        zip.closeEntry();
    }

    private static void writeBytes(OutputStream out, String content) throws Exception {
        out.write(content.getBytes(StandardCharsets.UTF_8));
    }

    private record Entry(String name, String content) {}
}
