package scripting.builder.jdk.download;

/**
 * Each JDK distributor constant carries the token used by foojay Disco API's {@code distribution} query parameter.
 * <p>
 * More provider will be added in the future.
 * @apiNote Make sure to check for licencing before adding a provider
 */
public enum JDKProvider {
    /**
     * Eclipse Temurin, Adoptium.
     */
    Temurin("temurin", "Eclipse Temurin");

    /**
     * The {@code distribution} token for this provider.
     */
    public final String distribution;
    /**
     * The vendor's formal name for display
     */
    public final String formalName;

    JDKProvider(String distribution, String formalName) {
        this.distribution = distribution;
        this.formalName = formalName;
    }
}
