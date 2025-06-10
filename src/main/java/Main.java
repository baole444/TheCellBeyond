import TheCellBeyond.Window;
import utility.PathResolver;

public class Main {
    public static void main(String[] args){
        // Initialize Path resolver to null on startup
        // This is before windows is started.
        PathResolver.initialize(null);

        Window window = Window.get();
        window.run();
    }
}
