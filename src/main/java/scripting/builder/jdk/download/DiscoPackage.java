package scripting.builder.jdk.download;

/**
 * A JDK package entry returned by the foojay Disco {@code /packages} query.
 * @param id the package id, used to resolve the direct download via {@code /ids/&#123;id&#125;}
 * @param filename the archieve file name
 * @param archiveType the archive format, {@code zip} or {@code tar.gz}
 * @param javaVersion the full Java version string of the package
 * @param majorVersion the major Java feature version
 * @param size the archive size in bytes
 */
public record DiscoPackage(String id, String filename, String archiveType, String javaVersion, int majorVersion, long size) {}
