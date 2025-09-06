package TheCellBeyond;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ConsoleStream extends OutputStream {
    private static ConsoleStream instance = null;
    private final StringBuilder buffer = new StringBuilder();
    private final List<String> lines = new ArrayList<>();

    private ConsoleStream() {}

    public static ConsoleStream get() {
        if (instance == null) instance = new ConsoleStream();
        return instance;
    }

    @Override
    public synchronized void write(int b) {
        if (b == '\n') {
            lines.add(buffer.toString());
            buffer.setLength(0);
        } else {
            buffer.append((char) b);
        }
    }

    public synchronized List<String> getLines() {
        return new ArrayList<>(lines);
    }

    public synchronized void clear() {
        lines.clear();
    }
}
