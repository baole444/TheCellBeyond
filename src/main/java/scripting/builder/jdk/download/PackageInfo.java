package scripting.builder.jdk.download;

/**
 * The resolved download detail for a package, returned by the foojay Disco {@code /ids/&#123;id&#125;} endpoint.
 * @param filename the archive file name
 * @param directDownloadUri the direct download URL
 * @param checksum the archive checksum
 * @param checksumType the checksum algorithm, such as {@code sha256}
 */
public record PackageInfo(String filename, String directDownloadUri, String checksum, String checksumType) {}
