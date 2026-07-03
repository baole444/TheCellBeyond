package scripting.builder.jdk;

import java.nio.file.Path;

/**
 * A JDK installation on disk as resolved by {@link JDKDetector}.
 * @param home the resolved JDK home directory
 * @param vendor the vendor string that release the JDK
 * @param version the full version string
 * @param featureVersion the major Java feature version
 * @param source the installation's origin
 * @param valid is the JDK still present or compatible or not
 */
public record JDKInstallation(Path home, String vendor, String version, int featureVersion, Source source, boolean valid) {
    /**
     * The source of the JDK installation when it is discovered.
     */
    public enum Source {
        JavaHome,
        Detected,
        UserAdded,
        Downloaded
    }

    private static final String JavaHomePrefix = "JAVA_HOME ";
    /**
     * Display text for a registered entry with missing JDK or the JDK is no longer valid.
     */
    public static final String MissingLabel = "<No JDK found>";

    /**
     * Get the displace name for the JDK installation in {@code [JAVA_HOME] <vendor> <feature_version>} format.
     * @return A string comprised of optional Java home prefix, follow by vendor and feature version
     */
    public String displayName() {
        if (!valid) return MissingLabel;
        String vendorName = vendor == null || vendor.isBlank() ? "Unknown" : vendor;
        if (source == Source.JavaHome) return JavaHomePrefix + vendorName + " " + featureVersion;
        return vendorName + " " + featureVersion;
    }

    /**
     * Check if this installation is sourced from {@code JAVA_HOME} env.
     * @return true if from {@code JAVA_HOME} env
     */
    public boolean fromJavaHome() {
        return source == Source.JavaHome;
    }
}
