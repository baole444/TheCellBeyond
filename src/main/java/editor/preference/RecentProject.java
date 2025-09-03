package editor.preference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;

public record RecentProject(String title, String path, String lastOpenScene) {
    @JsonIgnore
    public boolean isPresentedAtPath() {
        if (path == null) return false;

        File file = new File(path);
        return file.exists();
    }
}
