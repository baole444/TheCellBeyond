package generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.classgraph.*;
import scripting.transpiler.manifest.APIManifest;
import scripting.transpiler.manifest.APIType;
import scripting.transpiler.manifest.MemberInfo;
import scripting.transpiler.manifest.SnakeCaseConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ManifestGenerator {
    /**
     * ACC_ENUM value, Java classification for enum access flag.
     */
    private static final int EnumAccessModifier = 0x4000;
    private static final String MethodNamespace = "method";
    private static final String FieldNamespace = "field";

    private ManifestGenerator() {}

    static void main(String[] args) throws IOException {
        if (args.length != 3) throw new IllegalArgumentException("Usage: ManifestGenerator <apiJarPath> <outputJsonPath> <apiVersion>");
        String apiJarPath = args[0];
        Path outputPath = Path.of(args[1]);
        String apiVersion = args[2];
        List<APIType> classInfos = scan(apiJarPath);
        classInfos.sort(Comparator.comparing(c -> c.fqn));
        APIManifest manifest = new APIManifest(APIManifest.CurrentManifestVersion, apiVersion, classInfos);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().enableComplexMapKeySerialization().create();
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, gson.toJson(manifest) + "\n", StandardCharsets.UTF_8);
        System.out.printf("Written %d script API manifest classes to %s%n", classInfos.size(), outputPath);
    }

    private static List<APIType> scan(String apiJarPath) {
        List<APIType> result = new ArrayList<>();
        List<String> collisions = new ArrayList<>();
        ClassGraph classGraph = new ClassGraph().overrideClasspath(apiJarPath).enableAllInfo().enableExternalClasses();
        try (ScanResult scan = classGraph.scan()) {
            for (ClassInfo info : scan.getAllClasses()) {
                if (info.isExternalClass() || !info.isPublic()) continue;
                result.add(toAPIType(info, collisions));
            }
        }
        if (collisions.isEmpty()) return result;
        collisions.forEach(System.err::println);
        throw new IllegalArgumentException("Script API manifest has " + collisions.size() + " snake alias collision(s)");
    }

    private static APIType toAPIType(ClassInfo info, List<String> collisions) {
        String fqn = info.getName();
        APIType.Kind kind = kindOf(info);
        List<String> superClassChain = info.getSuperclasses().getNames();
        List<MemberInfo> methods = collectMethods(info, fqn, collisions);
        List<MemberInfo> fields = collectFields(info, fqn, collisions);
        List<String> enumConstants = kind == APIType.Kind.Enum ? collectEnumConstants(info) : null;
        return new APIType(fqn, info.getSimpleName(), kind, info.isFinal(), info.isAbstract(), superClassChain, methods, fields, enumConstants);
    }

    private static APIType.Kind kindOf(ClassInfo info) {
        if (info.isEnum()) return APIType.Kind.Enum;
        if (info.isAnnotation()) return APIType.Kind.Annotation;
        if (info.isInterface()) return APIType.Kind.Interface;
        return APIType.Kind.Class;
    }

    private static List<MemberInfo> collectMethods(ClassInfo info, String fqn, List<String> collisions) {
        boolean isEnum = info.isEnum();
        Map<String, List<String>> overloads = new TreeMap<>();
        for (MethodInfo method : info.getDeclaredMethodInfo()) {
            if (!method.isPublic() || method.isBridge() || method.isSynthetic()) continue;
            String name = method.getName();
            if (isEnum && (name.equals("values") || name.equals("valueOf"))) continue;
            overloads.computeIfAbsent(name, _ -> new ArrayList<>()).add(method.getTypeDescriptorStr());
        }
        List<MemberInfo> members = new ArrayList<>();
        Map<String, String> aliasOwner = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : overloads.entrySet()) {
            String javaName = entry.getKey();
            String alias = SnakeCaseConverter.toSnake(javaName);
            detectCollision(aliasOwner, alias, javaName, fqn, MethodNamespace, collisions);
            List<String> signatures = entry.getValue();
            signatures.sort(Comparator.naturalOrder());
            members.add(MemberInfo.method(javaName, alias, signatures));
        }
        return members;
    }

    private static List<MemberInfo> collectFields(ClassInfo info, String fqn, List<String> collisions) {
        Map<String, String> fieldTypes = new TreeMap<>();
        for (FieldInfo field : info.getDeclaredFieldInfo()) {
            if (!field.isPublic() || (field.getModifiers() & EnumAccessModifier) != 0) continue;
            fieldTypes.put(field.getName(), field.getTypeDescriptor().toString());
        }
        List<MemberInfo> members = new ArrayList<>();
        Map<String, String> aliasOwner = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldTypes.entrySet()) {
            String javaName = entry.getKey();
            String alias = SnakeCaseConverter.toSnake(javaName);
            detectCollision(aliasOwner, alias, javaName, fqn, FieldNamespace, collisions);
            members.add(MemberInfo.field(javaName, alias, entry.getValue()));
        }
        return members;
    }

    private static List<String> collectEnumConstants(ClassInfo info) {
        List<String> constants = new ArrayList<>();
        for (FieldInfo field : info.getDeclaredFieldInfo()) {
            if ((field.getModifiers() & EnumAccessModifier) != 0) constants.add(field.getName());
        }
        return constants;
    }

    private static void detectCollision(Map<String, String> aliasOwner, String alias, String javaName, String fqn, String namespace, List<String> collisions) {
        String previousOwner = aliasOwner.putIfAbsent(alias, javaName);
        if (previousOwner == null || previousOwner.equals(javaName)) return;
        collisions.add(String.format("Snake alias collision in %s (%s): '%s' and '%s' both map to '%s'", fqn, namespace, previousOwner, javaName, alias));
    }
}
