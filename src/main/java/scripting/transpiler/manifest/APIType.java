package scripting.transpiler.manifest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * APIType is an entry for a public API type (class, enum, interface or annotation) in the script API manifest.
 * Each entry record the name, kind, superclass chain and public members.
 * <p>
 * Members are the type's declared public methods and fields. Inherited members are found by walking the {@link #superClassChain},
 * via the
 * </p>
 */
public final class APIType {
    /**
     * Kind is used for classifying the API type in serialization of the manifest.
     */
    public enum Kind {
        Class,
        Enum,
        Interface,
        Annotation
    }

    /**
     * Type's fully qualified name.
     */
    public String fqn;
    /**
     * Type's canonical name.
     */
    public String simpleName;
    /**
     * Type's classification.
     */
    public Kind kind;
    /**
     * Whether the type is declared {@code final}, and cannot be extended.
     */
    public boolean isFinal;
    /**
     * The chain of type's super class(es).
     */
    public List<String> superClassChain;
    /**
     * The list of type's public methods.
     */
    public List<MemberInfo> methods;
    /**
     * The list of type's public fields.
     */
    public List<MemberInfo> fields;
    /**
     * The list of type's enum constants (Enum class.)
     */
    public List<String> enumConstants;

    private transient Map<String, MemberInfo> methodsByAlias;
    private transient Map<String, MemberInfo> fieldsByAlias;

    public APIType() {}

    public APIType(String fqn, String simpleName, Kind kind, boolean isFinal, List<String> superClassChain, List<MemberInfo> methods, List<MemberInfo> fields, List<String> enumConstants) {
        this.fqn = fqn;
        this.simpleName = simpleName;
        this.kind = kind;
        this.isFinal = isFinal;
        this.superClassChain = superClassChain;
        this.methods = methods;
        this.fields = fields;
        this.enumConstants = enumConstants;
    }

    public Optional<MemberInfo> findMethod(String snakeAlias) {
        if (methodsByAlias == null) methodsByAlias = indexByAlias(methods);
        return Optional.ofNullable(methodsByAlias.get(snakeAlias));
    }

    public Optional<MemberInfo> findField(String snakeAlias) {
        if (fieldsByAlias == null) fieldsByAlias = indexByAlias(fields);
        return Optional.ofNullable(fieldsByAlias.get(snakeAlias));
    }

    private static Map<String, MemberInfo> indexByAlias(List<MemberInfo> members) {
        Map<String, MemberInfo> index = new HashMap<>();
        if (members == null) return index;
        for (MemberInfo member : members) index.putIfAbsent(member.snakeAlias(), member);
        return index;
    }
}
