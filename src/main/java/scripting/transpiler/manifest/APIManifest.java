package scripting.transpiler.manifest;

import TheCellBeyond.GameObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class APIManifest {
    private static final class ManifestCache {
        static final APIManifest Index = load();
    }

    public static final int CurrentManifestVersion = 2;
    private static final String IndexPath = "META-INF/script-api-index.json";
    private static final String GameObjectFQN = GameObject.class.getName();
    private static final String ComponentFQN = Component.class.getName();
    public int manifestVersion;
    public String apiVersion;
    public List<APIType> classes;
    private transient Map<String, APIType> byFQN;
    private transient Map<String, APIType> bySimpleName;

    public APIManifest() {}

    public APIManifest(int manifestVersion, String apiVersion, List<APIType> classes) {
        this.manifestVersion = manifestVersion;
        this.apiVersion = apiVersion;
        this.classes = classes;
    }

    public static int manifestVersion() {
        return ManifestCache.Index.manifestVersion;
    }

    public static String apiVersion() {
        return ManifestCache.Index.apiVersion;
    }

    public static Optional<APIType> findClassByFQN(String FQN) {
        return Optional.ofNullable(ManifestCache.Index.byFQN.get(FQN));
    }

    public static Optional<APIType> findClassBySimpleName(String simpleName) {
        return Optional.ofNullable(ManifestCache.Index.bySimpleName.get(simpleName));
    }

    public static Optional<MemberInfo> findAPIMember(String classFQN, String snakeAlias) {
        APIType type = ManifestCache.Index.byFQN.get(classFQN);
        if (type == null) return Optional.empty();
        Optional<MemberInfo> method = type.findMethod(snakeAlias);
        return method.isPresent() ? method : type.findField(snakeAlias);
    }

    public static List<String> superclassChain(String classFQN) {
        APIType type = ManifestCache.Index.byFQN.get(classFQN);
        if (type == null || type.superClassChain == null) return List.of();
        return type.superClassChain;
    }

    public static boolean inGameObjectLineage(String classFQN) {
        return inLinage(classFQN, GameObjectFQN);
    }

    public static boolean inComponentLineage(String classFQN) {
        return inLinage(classFQN, ComponentFQN);
    }

    private static APIManifest load() {
        ClassLoader loader = APIManifest.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(IndexPath)) {
            if (stream == null) throw new IllegalStateException("Script API manifest not found on classpath at " + IndexPath);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                APIManifest manifest = gson.fromJson(reader, APIManifest.class);
                manifest.index();
                return manifest;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read script API manifest", e);
        }
    }

    private void index() {
        byFQN = new HashMap<>();
        bySimpleName = new HashMap<>();
        if (classes == null) return;
        for (APIType type : classes) {
            byFQN.put(type.fqn, type);
            bySimpleName.putIfAbsent(type.simpleName, type);
        }
    }

    private static boolean inLinage(String classFQN, String ancestorFQN) {
        if (classFQN == null || ancestorFQN == null) return false;
        if (ancestorFQN.equals(classFQN)) return true;
        APIType type = ManifestCache.Index.byFQN.get(classFQN);
        return type != null && type.superClassChain != null && type.superClassChain.contains(ancestorFQN);
    }
}
