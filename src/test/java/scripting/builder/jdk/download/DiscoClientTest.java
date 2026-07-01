package scripting.builder.jdk.download;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DiscoClientTest {
    private static final ObjectMapper Mapper = new ObjectMapper().rebuild().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    @Test
    public void packageMapsSnakeCaseFields() {
        String json = """
                {
                  "id": "d76df094a9cbbabd3b08251f9e61444a",
                  "filename": "OpenJDK25U-jdk_x64_windows_hotspot_25.0.3_9.zip",
                  "archive_type": "zip",
                  "java_version": "25.0.3+9",
                  "major_version": 25,
                  "size": 141131903,
                  "unknown_field": "ignored"
                }""";
        DiscoPackage pkg = Mapper.readValue(json, DiscoPackage.class);
        assertEquals("d76df094a9cbbabd3b08251f9e61444a", pkg.id());
        assertEquals("OpenJDK25U-jdk_x64_windows_hotspot_25.0.3_9.zip", pkg.filename());
        assertEquals("zip", pkg.archiveType());
        assertEquals("25.0.3+9", pkg.javaVersion());
        assertEquals(25, pkg.majorVersion());
        assertEquals(141131903L, pkg.size());
    }

    @Test
    public void packageInfoMapsDownloadAndChecksum() {
        String json = """
                {
                  "filename": "OpenJDK25U-jdk_x64_windows_hotspot_25.0.3_9.zip",
                  "direct_download_uri": "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.3%2B9/OpenJDK25U-jdk_x64_windows_hotspot_25.0.3_9.zip",
                  "download_site_uri": "",
                  "checksum": "709312cd0420296d9b9de917fe6e28a5b979e875ee5ab91783fb79bcd5857235",
                  "checksum_type": "sha256"
                }""";
        PackageInfo info = Mapper.readValue(json, PackageInfo.class);
        assertEquals("https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.3%2B9/OpenJDK25U-jdk_x64_windows_hotspot_25.0.3_9.zip", info.directDownloadUri());
        assertEquals("709312cd0420296d9b9de917fe6e28a5b979e875ee5ab91783fb79bcd5857235", info.checksum());
        assertEquals("sha256", info.checksumType());
    }
}
