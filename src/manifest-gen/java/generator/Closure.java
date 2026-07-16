package generator;

import io.github.classgraph.*;

import java.util.*;

/**
 * Closure allow walking over the public/protected signature graph, collecting engine types and external references.
 */
final class Closure {
    final TreeSet<String> included = new TreeSet<>();
    final TreeMap<String, TreeSet<String>> externals = new TreeMap<>();
    private final ScanResult scan;
    private final Set<String> engineClasses;
    private final Deque<String> queue = new ArrayDeque<>();
    private final Set<String> seen = new HashSet<>();

    Closure(ScanResult scan, Set<String> engineClasses) {
        this.scan = scan;
        this.engineClasses = engineClasses;
    }

    void enqueue(String type, String referrer) {
        if (type == null || type.equals(APISurfaceGenerator.APIMarkerType)) return;
        if (!engineClasses.contains(type)) {
            externals.computeIfAbsent(type, _ -> new TreeSet<>()).add(referrer);
            return;
        }
        if (seen.add(type)) queue.add(type);
    }

    void walk() {
        while (!queue.isEmpty()) {
            String name = queue.poll();
            included.add(name);
            ClassInfo info = scan.getClassInfo(name);
            if (info == null) continue;
            info.getInterfaces().forEach(i -> enqueue(i.getName(), name));
            ClassInfo superClass = info.getSuperclass();
            if (superClass != null) enqueue(superClass.getName(), name);
            walkClassSignature(info, name);
            walkMembers(info, name);
        }
    }

    private void walkMembers(ClassInfo info, String owner) {
        for (MethodInfo method : info.getDeclaredMethodAndConstructorInfo()) {
            if (!method.isPublic() && !method.isProtected()) continue;
            for (MethodParameterInfo param : method.getParameterInfo()) walkSignature(param.getTypeSignatureOrTypeDescriptor(), owner);
            MethodTypeSignature sig = method.getTypeSignatureOrTypeDescriptor();
            if (sig == null) continue;
            walkTypeParameters(sig.getTypeParameters(), owner);
            walkSignature(sig.getResultType(), owner);
            sig.getThrowsSignatures().forEach(ref -> walkSignature(ref, owner));
        }
        for (FieldInfo field : info.getDeclaredFieldInfo()) {
            if (!field.isPublic() && !field.isProtected()) continue;
            walkSignature(field.getTypeSignatureOrTypeDescriptor(), owner);
        }
    }

    private void walkClassSignature(ClassInfo info, String owner) {
        ClassTypeSignature signature = info.getTypeSignatureOrTypeDescriptor();
        if (signature == null) return;
        walkTypeParameters(signature.getTypeParameters(), owner);
        walkSignature(signature.getSuperclassSignature(), owner);
        signature.getSuperinterfaceSignatures().forEach(ref -> walkSignature(ref, owner));
    }

    private void walkTypeParameters(List<TypeParameter> params, String owner) {
        if (params == null) return;
        for (TypeParameter param : params) {
            walkSignature(param.getClassBound(), owner);
            param.getInterfaceBounds().forEach(ref -> walkSignature(ref, owner));
        }
    }

    private void walkSignature(TypeSignature signature, String owner) {
        switch (signature) {
            case ArrayTypeSignature array -> walkSignature(array.getElementTypeSignature(), owner);
            case ClassRefTypeSignature ref -> {
                ClassInfo info = ref.getClassInfo();
                enqueue(info != null ? info.getName() : ref.getFullyQualifiedClassName(), owner);
                for (TypeArgument arg : ref.getTypeArguments()) walkSignature(arg.getTypeSignature(), owner);
            }
            case null, default -> {}
        }
    }
}
