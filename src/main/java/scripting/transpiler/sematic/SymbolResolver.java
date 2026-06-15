package scripting.transpiler.sematic;

import scripting.transpiler.ast.*;
import scripting.transpiler.manifest.APIManifest;
import scripting.transpiler.manifest.APIType;
import scripting.transpiler.manifest.MemberInfo;
import scripting.transpiler.manifest.SnakeCaseConverter;

import java.util.*;

/**
 * Second pass of the semantic phase. Resolves type references and identifiers of a parsed script against the lifecycle table,
 * the script's own members, the project class index, and the API manifest. AST nodes are annotated in place with {@link Resolution} results.
 * It also walks the {@code extends} lineage to set the class's {@link ClassRegistration}, detect circular, final or unresolvable supertypes.
 * <p>
 * Methods are resolved in two passes, with signature first, and then the body. This way, unqualified self call to a translated method emits the same
 * Java name as its declaration. A method declaration is resolved to a lifecycle hook, an inherited API method it overrides, or an identity user method.
 * </p>
 * Member resolution covers the implicit {@code this}, unqualified receiver, whose type is the current class,
 * and qualified access, such as {@code receiver.member} against any receiver whose static type is knowable without inference.
 * @see #resolveQualifiedMember(Expression, String, Map) More on member resolution
 */
public final class SymbolResolver {
    private static final String ObjectType = "Object";
    private static final Set<String> BuiltInType = Set.of("int", "float", "bool", "String", "void", ObjectType);
    private final Map<String, ProjectClassEntry> projectIndex;
    private final List<SemanticError> errors = new ArrayList<>();
    private Map<String, TypeReference> fieldTypes;
    private Set<String> methodNames;
    /**
     * Resolution of each translated lifecycle or override method by script name, for self calls.
     */
    private Map<String, Resolution> selfCallResolution;
    /**
     * Nearest API ancestor of the current class, the receiver type for unqualified members, or null.
     */
    private String apiReceiverFQN;

    public SymbolResolver(Map<String, ProjectClassEntry> projectIndex) {
        this.projectIndex = projectIndex;
    }

    /**
     * Resolve the script in place, annotating its AST nodes.
     * @param scriptFile the parsed script
     * @return the resolution errors, empty when the script is fully resolved
     */
    public List<SemanticError> resolve(ScriptFile scriptFile) {
        ClassDeclaration classDeclaration = scriptFile.classDeclaration;
        indexMembers(classDeclaration);
        resolveExtends(classDeclaration);
        classDeclaration.methods.forEach(this::resolveMethodSignature);
        for (FieldDeclaration field : classDeclaration.fields) {
            resolveType(field.type);
            if (field.initializer != null) resolveExpression(field.initializer, new HashMap<>());
        }
        classDeclaration.methods.forEach(this::resolveMethodBody);
        return errors;
    }

    private void indexMembers(ClassDeclaration classDeclaration) {
        fieldTypes = new HashMap<>();
        methodNames = new HashSet<>();
        selfCallResolution = new HashMap<>();
        classDeclaration.fields.forEach(field -> fieldTypes.put(field.name, field.type));
        classDeclaration.methods.forEach(method -> methodNames.add(method.name));
    }

    private void resolveExtends(ClassDeclaration classDeclaration) {
        apiReceiverFQN = null;
        TypeReference superType = classDeclaration.superType;
        if (superType == null || superType.name.equals(ObjectType)) {
            classDeclaration.registration = ClassRegistration.None;
            return;
        }
        if (BuiltInType.contains(superType.name)) {
            error(superType, "Cannot extend built in type '" + superType.name + "'");
            classDeclaration.registration = ClassRegistration.None;
            return;
        }
        ProjectClassEntry entry = projectIndex.get(superType.name);
        if (entry != null) {
            superType.resolution = new Resolution.ProjectClassResolution(entry.fqn());
            classDeclaration.registration = walkProjectLineage(classDeclaration, entry, superType);
            return;
        }
        Optional<APIType> apiType = APIManifest.findClassBySimpleName(superType.name);
        if (apiType.isPresent()) {
            APIType type = apiType.get();
            superType.resolution = new Resolution.APIClassResolution(type.fqn);
            if (type.isFinal) error(superType, "Cannot extend final type '" + superType.name + "'");
            apiReceiverFQN = type.fqn;
            classDeclaration.registration = registrationOf(type.fqn);
            return;
        }
        error(superType, "Cannot resolve type '" + superType.name + "'");
        classDeclaration.registration = ClassRegistration.None;
    }

    /**
     * Walk the project supertype chain to the nearest API ancestor, set the value for {@link #apiReceiverFQN}.
     * This detects circular and unresolvable links along the chain.
     * @param classDeclaration the class declaration to resolve
     * @param start starting entry for the chain
     * @param superType the origin super type to resolve
     * @return the registration implied by the lineage
     */
    private ClassRegistration walkProjectLineage(ClassDeclaration classDeclaration, ProjectClassEntry start, TypeReference superType) {
        Set<String> visited = new HashSet<>();
        visited.add(classDeclaration.name);
        ProjectClassEntry current = start;
        while (current != null) {
            if (!visited.add(current.simpleName())) {
                error(superType, "Circular extends involving '" + current.simpleName() + "'");
                return ClassRegistration.None;
            }
            String superRef = current.superClassRef();
            if (superRef == null || superRef.equals(ObjectType)) return ClassRegistration.None;
            ProjectClassEntry next = projectIndex.get(superRef);
            if (next != null) {
                current = next;
                continue;
            }
            Optional<APIType> api = APIManifest.findClassBySimpleName(superRef);
            if (api.isPresent()) {
                apiReceiverFQN = api.get().fqn;
                return registrationOf(api.get().fqn);
            }
            error(superType, "Cannot resolve ancestor type '" + superRef + "' in extends chain");
            return ClassRegistration.None;
        }
        return ClassRegistration.None;
    }

    private ClassRegistration registrationOf(String apiFQN) {
        if (APIManifest.inGameObjectLineage(apiFQN)) return ClassRegistration.GameObject;
        if (APIManifest.inComponentLineage(apiFQN)) return ClassRegistration.Component;
        return ClassRegistration.None;
    }

    /**
     * Phase one: resolve the declaration of a lifecycle hook or an inherited API method override.
     * This is recorded for self call. Bodies are solved later so self call can see the result.
     * @param method the method to resolve
     */
    private void resolveMethodSignature(MethodDeclaration method) {
        Optional<LifecycleTable.LifecycleHook> hook = LifecycleTable.find(method.name);
        if (hook.isPresent()) method.resolution = new Resolution.LifecycleResolution(hook.get().javaName());
        else resolveAPIOverride(method);
        if (method.resolution != null) selfCallResolution.put(method.name, method.resolution);
    }

    private void resolveMethodBody(MethodDeclaration method) {
        Map<String, TypeReference> scope = new HashMap<>();
        for (ParameterDeclaration param : method.parameters) {
            resolveType(param.type);
            scope.put(param.name, param.type);
            if (param.defaultValue != null) resolveExpression(param.defaultValue, scope);
        }
        resolveType(method.returnType);
        resolveBlock(method.body, scope);
    }

    /**
     * Resolve method declaration that overrides an inherited API method. A match requires both the snake name, and the parameter
     * signature, to line up with the API method. Code generation will emit the API {@code javaName} and {@code @Override}.
     * @param method the method to resolve override for
     */
    private void resolveAPIOverride(MethodDeclaration method) {
        if (apiReceiverFQN == null) return;
        Optional<Resolution.APIMemberResolution> candidate = findAPIMember(apiReceiverFQN, method.name);
        if (candidate.isEmpty()) return;
        APIManifest.findAPIMember(candidate.get().receiverClassFQN(), SnakeCaseConverter.toSnake(method.name))
                .filter(member -> member.signatures() != null)
                .filter(member -> anyOverride(method, member.signatures()))
                .ifPresent(_ -> method.resolution = candidate.get());
    }

    private void resolveBlock(Block block, Map<String, TypeReference> scope) {
        for (Statement statement : block.statements) resolveStatement(statement, scope);
    }

    private void resolveStatement(Statement statement, Map<String, TypeReference> scope) {
        switch (statement) {
            case LocalVariableDeclaration local -> {
                resolveType(local.type);
                if (local.initializer != null) resolveExpression(local.initializer, scope);
                scope.put(local.name, local.type);
            }
            case ExpressionStatement expression -> resolveExpression(expression.expression, scope);
            case AssignmentStatement assign -> {
                resolveExpression(assign.target, scope);
                resolveExpression(assign.value, scope);
            }
            case ReturnStatement re -> {
                if (re.value != null) resolveExpression(re.value, scope);
            }
            case IfStatement ifStatement -> resolveIf(ifStatement, scope);
            case WhileStatement whileStatement -> {
                resolveExpression(whileStatement.condition, scope);
                resolveBlock(whileStatement.body, scope);
            }
            case ForStatement forStatement -> {
                resolveExpression(forStatement.iterable, scope);
                scope.put(forStatement.variable, null);
                resolveBlock(forStatement.body, scope);
            }
            default -> {}
        }
    }

    private void resolveIdentifier(IdentifierExpression identifier, Map<String, TypeReference> scope) {
        String name = identifier.name;
        if (scope.containsKey(name) || fieldTypes.containsKey(name) || methodNames.contains(name)) {
            identifier.resolution = new Resolution.UserMemberResolution(name);
            return;
        }
        Optional<Resolution.APIMemberResolution> inherited = findInheritedAPIMember(name);
        if (inherited.isPresent()) {
            identifier.resolution = inherited.get();
            return;
        }
        Optional<Resolution> classRef = resolveClassReference(name);
        if (classRef.isPresent()) {
            identifier.resolution = classRef.get();
            return;
        }
        error(identifier, "Cannot resolve '" + name + "'");
    }

    private void resolveIf(IfStatement ifStatement, Map<String, TypeReference> scope) {
        resolveExpression(ifStatement.condition, scope);
        resolveBlock(ifStatement.thenBlock, scope);
        for (ElifClause clause : ifStatement.elifClauses) {
            resolveExpression(clause.condition, scope);
            resolveBlock(clause.block, scope);
        }
        if (ifStatement.elseBlock != null) resolveBlock(ifStatement.elseBlock, scope);
    }

    private void resolveCall(MethodCallExpression call, Map<String, TypeReference> scope) {
        for (Expression arg : call.arguments) resolveExpression(arg, scope);
        if (call.target != null) {
            resolveExpression(call.target, scope);
            call.resolution = resolveQualifiedMember(call.target, call.methodName, scope);
            return;
        }
        if (methodNames.contains(call.methodName)) {
            Resolution translated = selfCallResolution.get(call.methodName);
            call.resolution = translated != null ? translated : new Resolution.UserMemberResolution(call.methodName);
            return;
        }
        Optional<Resolution.APIMemberResolution> inherited = findInheritedAPIMember(call.methodName);
        if (inherited.isPresent()) {
            call.resolution = inherited.get();
            return;
        }
        error(call, "Cannot resolve method '" + call.methodName + "'");
    }

    private void resolveExpression(Expression expression, Map<String, TypeReference> scope) {
        switch (expression) {
            case IdentifierExpression identifier -> resolveIdentifier(identifier, scope);
            case MethodCallExpression call -> resolveCall(call, scope);
            case MemberAccessExpression access -> {
                resolveExpression(access.target, scope);
                access.resolution = resolveQualifiedMember(access.target, access.memberName, scope);
            }
            case BinaryExpression binary -> {
                resolveExpression(binary.left, scope);
                resolveExpression(binary.right, scope);
            }
            case UnaryExpression unary -> resolveExpression(unary.operand, scope);
            case ConditionalExpression conditional -> {
                resolveExpression(conditional.condition, scope);
                resolveExpression(conditional.thenValue, scope);
                resolveExpression(conditional.elseValue, scope);
            }
            case IndexExpression index -> {
                resolveExpression(index.target, scope);
                resolveExpression(index.index, scope);
            }
            case CastExpression cast -> {
                resolveExpression(cast.value, scope);
                resolveType(cast.type);
            }
            case TypeCheckExpression check -> {
                resolveExpression(check.value, scope);
                resolveType(check.type);
            }
            default -> {}
        }
    }

    /**
     * Resolution for a member accessed through an explicit receiver, such as {@code receiver.member}.
     * <p>
     * Resolve against the receiver's static type when that type is knowable without inference, such as typed member, class static, cast target.
     * For fluent, builder chains, it's the return type of resolved API call, parsed from its descriptor.
     * </p>
     * Type propagation stop, and the member pass through unchanged, such as script project types,
     * where only headers are indexed, primitives or overload sets, whose return types disagreed.
     * @param receiver the resolved receiver expression
     * @param memberName the accessed member name
     * @param scope the locals and parameters within the scope, with their declared types
     * @return the member resolution, or null when left to pass through
     */
    private Resolution resolveQualifiedMember(Expression receiver, String memberName, Map<String, TypeReference> scope) {
        return typeOf(receiver, scope).flatMap(fqn -> findAPIMember(fqn, memberName)).orElse(null);
    }

    private Optional<String> typeOf(Expression expression, Map<String, TypeReference> scope) {
        return switch (expression) {
            case IdentifierExpression identifier -> identifierType(identifier, scope);
            case MethodCallExpression call -> memberType(call.resolution, call.methodName);
            case MemberAccessExpression access -> memberType(access.resolution, access.memberName);
            case CastExpression cast -> apiFQN(cast.type);
            default -> Optional.empty();
        };
    }

    private Optional<String> identifierType(IdentifierExpression identifier, Map<String, TypeReference> scope) {
        TypeReference declared = scope.containsKey(identifier.name) ? scope.get(identifier.name) : fieldTypes.get(identifier.name);
        if (declared != null) return apiFQN(declared);
        if (identifier.resolution instanceof Resolution.APIClassResolution(String fqn)) return Optional.of(fqn);
        return Optional.empty();
    }

    private Optional<String> memberType(Resolution resolution, String name) {
        if (!(resolution instanceof Resolution.APIMemberResolution member)) return Optional.empty();
        Optional<MemberInfo> info = APIManifest.findAPIMember(member.receiverClassFQN(), SnakeCaseConverter.toSnake(name));
        if (info.isEmpty()) return Optional.empty();
        MemberInfo memberInfo = info.get();
        return memberInfo.signatures() != null ? methodReturnType(memberInfo.signatures()) : knownApiFqn(memberInfo.type());
    }

    private Optional<String> apiFQN(TypeReference type) {
        if (type == null || type.arrayDepth != 0) return Optional.empty();
        return APIManifest.findClassBySimpleName(type.name).map(apiType -> apiType.fqn);
    }

    private Optional<Resolution.APIMemberResolution> findInheritedAPIMember(String name) {
        return apiReceiverFQN == null ? Optional.empty() : findAPIMember(apiReceiverFQN, name);
    }

    private Optional<Resolution.APIMemberResolution> findAPIMember(String receiverFQN, String name) {
        String alias = SnakeCaseConverter.toSnake(name);
        Optional<MemberInfo> direct = APIManifest.findAPIMember(receiverFQN, alias);
        if (direct.isPresent()) return Optional.of(new Resolution.APIMemberResolution(receiverFQN, direct.get().javaName()));
        for (String ancestor : APIManifest.superclassChain(receiverFQN)) {
            Optional<MemberInfo> found = APIManifest.findAPIMember(ancestor, alias);
            if (found.isPresent()) return Optional.of(new Resolution.APIMemberResolution(ancestor, found.get().javaName()));
        }
        return Optional.empty();
    }

    private Optional<Resolution> resolveClassReference(String name) {
        ProjectClassEntry entry = projectIndex.get(name);
        if (entry != null) return Optional.of(new Resolution.ProjectClassResolution(entry.fqn()));
        return APIManifest.findClassBySimpleName(name).map(type -> new Resolution.APIClassResolution(type.fqn));
    }

    private void resolveType(TypeReference type) {
        if (type == null || BuiltInType.contains(type.name)) return;
        ProjectClassEntry entry = projectIndex.get(type.name);
        if (entry != null) {
            type.resolution = new Resolution.ProjectClassResolution(entry.fqn());
            return;
        }
        Optional<APIType> apiType = APIManifest.findClassBySimpleName(type.name);
        if (apiType.isPresent()) {
            type.resolution = new Resolution.APIClassResolution(apiType.get().fqn);
            return;
        }
        error(type, "Cannot resolve type '" + type.name + "'");
    }

    private void error(AstNode node, String message) {
        SourcePosition position = node.position;
        errors.add(new SemanticError(position.file(), position.line(), position.column(), message));
    }

    private static Optional<String> methodReturnType(List<String> signatures) {
        String returnType = null;
        for (String descriptor : signatures) {
            Optional<String> parsed = parseReturnType(descriptor);
            if (parsed.isEmpty()) return Optional.empty();
            if (returnType == null) returnType = parsed.get();
            else if (!returnType.equals(parsed.get())) return Optional.empty();
        }
        return returnType == null ? Optional.empty() : knownApiFqn(returnType);
    }

    private static Optional<String> parseReturnType(String descriptor) {
        int close = descriptor.lastIndexOf(')');
        if (close < 0) return Optional.empty();
        String returnDescriptor = descriptor.substring(close + 1);
        if (!returnDescriptor.startsWith("L") || !returnDescriptor.endsWith(";")) return Optional.empty();
        return Optional.of(returnDescriptor.substring(1, returnDescriptor.length() - 1).replace('/', '.'));
    }

    private static Optional<String> knownApiFqn(String fqn) {
        return APIManifest.findClassByFQN(fqn).map(apiType -> apiType.fqn);
    }

    private static boolean anyOverride(MethodDeclaration method, List<String> signatures) {
        for (String descriptor : signatures) {
            if (matchingSignature(method, descriptor)) return true;
        }
        return false;
    }

    private static boolean matchingSignature(MethodDeclaration method, String descriptor) {
        List<String> expected = parseParameterTypes(descriptor);
        if (expected == null || expected.size() != method.parameters.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            if (!expected.get(i).equals(declaredTypeName(method.parameters.get(i).type))) return false;
        }
        return true;
    }

    private static List<String> parseParameterTypes(String descriptor) {
        int open = descriptor.indexOf('(');
        int close = descriptor.indexOf(')');
        if (open < 0 || close < open) return null;
        List<String> types = new ArrayList<>();
        int index = open + 1;
        while (index < close) {
            int arrayDepth = 0;
            while (descriptor.charAt(index) == '[') {
                arrayDepth++;
                index++;
            }
            String base;
            if (descriptor.charAt(index) == 'L') {
                int semicolon = descriptor.indexOf(';', index);
                base = simpleNameOf(descriptor.substring(index + 1, semicolon));
                index = semicolon + 1;
            } else {
                base = primitiveName(descriptor.charAt(index));
                index++;
            }
            types.add(base + "[]".repeat(arrayDepth));
        }
        return types;
    }

    private static String simpleNameOf(String internalName) {
        String afterPackage = internalName.substring(internalName.lastIndexOf('/') + 1);
        return afterPackage.substring(afterPackage.lastIndexOf('$') + 1);
    }

    private static String primitiveName(char code) {
        return switch (code) {
            case 'I' -> "int";
            case 'F' -> "float";
            case 'Z' -> "bool";
            case 'D' -> "double";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'B' -> "byte";
            case 'C' -> "char";
            default -> String.valueOf(code);
        };
    }

    private static String declaredTypeName(TypeReference type) {
        return type.name + "[]".repeat(type.arrayDepth);
    }
}
