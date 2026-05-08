package utility.log;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Stream2Log is a utility class that allow redirecting prints to {@link EngineLog}. This can be used
 * where print stream is required and its output is desired to be logged or visible in the editor/error dialogue.
 * <p>
 * Stream2Log also provides 3 static print streams {@link #debug}, {@link #out} and {@link #err} to use.
 * </p>
 * Other than the mentioned purpose, it is recommended to use engine log directly if possible.
 */
public final class Stream2Log extends PrintStream {
    public static final Stream2Log debug = new Stream2Log(Level.Debug, ">TCB");
    public static final Stream2Log out = new Stream2Log(Level.Info, ">TCB");
    public static final Stream2Log err = new Stream2Log(Level.Error, ">TCB");

    public Stream2Log(Level level, Class<?> source) {
        String s = source != null ? source.getSimpleName() : "Unknown";
        this(level, s);
    }

    public Stream2Log(Level level, String source) {
        super(new Output2Log(level, source), true, StandardCharsets.UTF_8);
    }
}