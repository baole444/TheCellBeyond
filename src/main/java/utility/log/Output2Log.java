package utility.log;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class Output2Log extends OutputStream {
    private final Level level;
    private final String source;
    private final StringBuilder builder = new StringBuilder();

    Output2Log(Level level, String source) {
        if (level == null) level = Level.Debug;
        if (source == null || source.isBlank()) source = "TCB";
        this.level = level;
        this.source = source;
    }

    @Override
    public void write(int b) {
        char c = (char) (b & 0xFF);
        if (c == '\n') flush();
        else if (c != '\r') builder.append(c);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        for (char c : new String(b, off, len, StandardCharsets.UTF_8).toCharArray()) {
            if (c == '\n') flush();
            else if (c != '\r') builder.append(c);
        }
    }

    @Override
    public void flush() {
        if (builder.isEmpty()) return;
        EngineLog.log(level, source, builder.toString());
        builder.setLength(0);
    }
}
