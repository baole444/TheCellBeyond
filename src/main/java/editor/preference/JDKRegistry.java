package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Persited registry of user's Gradle JVM choices.
 * @param jdks absolute paths JDK homes added by user or downloaded
 * @param selection the current selection token,  {@value #JavaHomeSelection} or an absolute JDK home path
 */
public record JDKRegistry(List<String> jdks, String selection) {
    /**
     * The selection token point to {@code JAVA_HOME} environment variable.
     */
    public static final String JavaHomeSelection = "JAVA_HOME";

    public JDKRegistry {
        jdks = jdks == null ? List.of() : List.copyOf(jdks);
        if (selection == null || selection.isBlank()) selection = JavaHomeSelection;
    }

    /**
     * Create an empty registry with default selection to {@code JAVA_HOME}.
     */
    @JsonIgnore
    public JDKRegistry() {
        this(List.of(), JavaHomeSelection);
    }
}
