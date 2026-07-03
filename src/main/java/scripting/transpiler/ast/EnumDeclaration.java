package scripting.transpiler.ast;

import java.util.List;

/**
 * An {@code enum Name:} declaration, followed by its constants and optional trailing field section.
 * <p>
 * The fields for each Enum constant are declared in order, base on the synthesized constructor's parameters.
 * Each constant's argument list lines up positionally with the field section.
 */
public final class EnumDeclaration extends TypeDeclaration {
    public final List<EnumConstant> constants;
    public final List<FieldDeclaration> fields;

    public EnumDeclaration(SourcePosition position, String name, List<EnumConstant> constants, List<FieldDeclaration> fields) {
        super(position, name);
        this.constants = constants;
        this.fields = fields;
    }
}
