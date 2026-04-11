package editor.template;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.joml.Vector2f;
import org.joml.Vector4f;
import scripting.Export;
import scripting.TypeHint;
import utility.log.EngineLog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ExportBlockBuilder {
    private static final EngineLog Logger = new EngineLog(ExportBlockBuilder.class);
    private ExportBlockBuilder() {}

    static Consumer<Object> build(Class<?> c) {
        List<Consumer<Object>> fieldEditors = new ArrayList<>();
        for (Field field : c.getDeclaredFields()) {
            Export export = field.getAnnotation(Export.class);
            if (export == null) continue;
            TypeHint hint = resolveHint(export.type(), field.getType());
            if (hint == null) {
                Logger.warning(String.format("Cannot export field %s in %s: unsupported type %s", field.getName(), c.getSimpleName(), field.getType().getSimpleName()));
                continue;
            }
            field.setAccessible(true);
            String label = export.label().isEmpty() ? field.getName() : export.label();
            fieldEditors.add(buildFieldEditor(field, hint, label));
        }
        if (fieldEditors.isEmpty()) return null;
        String headerLabel = c.getSimpleName();
        String headerId = "##ScriptExport_" +  c.getName() + "_Properties";
        return instance -> {
            if (!ImGui.collapsingHeader(headerLabel + headerId, ImGuiTreeNodeFlags.DefaultOpen)) return;
            for (Consumer<Object> editors : fieldEditors) editors.accept(instance);
        };
    }

    private static TypeHint resolveHint(TypeHint declared, Class<?> fieldType) {
        if (declared != TypeHint.Auto) return isCompatible(declared, fieldType) ? declared : null;
        for (TypeHint hint : TypeHint.noneAutoTypes) {
            if (isCompatible(hint, fieldType)) return hint;
        }
        return null;
    }

    private static boolean isCompatible(TypeHint hint, Class<?> fieldType) {
        if (hint.dataType == null) return false;
        if (hint.dataType.isAssignableFrom(fieldType)) return true;
        return wrappedOf(hint.dataType) == fieldType || hint.dataType == wrappedOf(fieldType);
    }

    private static Class<?> wrappedOf(Class<?> t) {
        if (t == boolean.class) return Boolean.class;
        if (t == int.class) return Integer.class;
        if (t == float.class) return Float.class;
        return null;
    }

    private static Consumer<Object> buildFieldEditor(Field field, TypeHint hint, String label) {
        return switch (hint) {
            case Color -> instance -> {
                try {
                    Vector4f vec = (Vector4f) field.get(instance);
                    if (vec != null) EditorWidget.colorCtrl(label, vec, instance);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case Vector2 -> instance -> {
                try {
                    Vector2f vec = (Vector2f) field.get(instance);
                    if (vec != null) EditorWidget.dragVec2Ctrl(label, vec, 0.0f, instance);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case Boolean -> instance -> {
                try {
                    boolean current = (boolean) field.get(instance);
                    boolean next = EditorWidget.checkboxCtrl(label, current, instance);
                    if (next != current) field.set(instance, next);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case Integer -> instance -> {
                try {
                    int current = (int) field.get(instance);
                    int next = EditorWidget.dragIntCtrl(label, current, instance);
                    if (next != current) field.set(instance, next);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case Float -> instance -> {
                try {
                    float current = (float) field.get(instance);
                    float next = EditorWidget.dragFloatCtrl(label, current, instance);
                    if (next != current) field.set(instance, next);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case String -> instance -> {
                try {
                    String current = (String) field.get(instance);
                    String next = EditorWidget.inputTextWithIME(label, current != null ? current : "", 256,instance);
                    if (!next.equals(current)) field.set(instance, next);
                } catch (IllegalAccessException e) {
                    Logger.error(String.format("Cannot access field '%s': %s", field.getName(), e.getMessage()));
                }
            };
            case Auto -> _ -> {};
        };
    }
}
