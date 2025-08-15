package render.text;

import components.TextRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.Settings;

import java.util.*;

public class DirectTextRenderer {
    private static DirectTextRenderer instance;

    private final List<TextBatch> textBatches = new ArrayList<>();

    private final Map<String, List<TextRenderer>> textGroups = new HashMap<>();

    private Matrix4f projectionMatrix, viewMatrix;

    private DirectTextRenderer() {}

    public static DirectTextRenderer get() {
        if (instance == null) {
            instance = new DirectTextRenderer();
        }

        return instance;
    }

    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;

        for (TextBatch batch : textBatches) {
            batch.setProjectionMatrix(projectionMatrix);
        }
    }

    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix = viewMatrix;

        for (TextBatch batch : textBatches) {
            batch.setViewMatrix(viewMatrix);
        }
    }

    public TextRenderer drawText(String text, float x, float y, float point, Vector4f color, String fontPath, int zIndex, GlyphRange glyphRange) {
        if (fontPath == null) {
            fontPath = Settings.PATH.CONSOLA;
        }

        TextRenderer textRenderer = new TextRenderer(text, fontPath, point, color, new Vector2f(x, y), glyphRange);
        textRenderer.start();

        String groupKey = zIndex + "_" + fontPath + "_" + point;
        textGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(textRenderer);

        return textRenderer;
    }

    public TextRenderer drawText(String text, float x, float y, float point, Vector4f color, String fontPath, GlyphRange glyphRange) {
        return drawText(text, x, y, point, color,fontPath, 0, glyphRange);
    }

    public void render() {
        FontManager.get().updateFontTextures();

        for (List<TextRenderer> components : textGroups.values()) {
            for (TextRenderer component : components) {
                component.update(0.0f);
            }
        }

        for (Map.Entry<String, List<TextRenderer>> entry : textGroups.entrySet()) {
            String[] parts = entry.getKey().split("_");
            int zIndex = Integer.parseInt(parts[0]);

            List<TextRenderer> components = entry.getValue();
            if (components.isEmpty()) continue;

            TextBatch batch = null;
            for (TextBatch existingBatch : textBatches) {
                if (existingBatch.getzIndex() == zIndex && existingBatch.hasRoom()) {
                    batch = existingBatch;
                    break;
                }
            }

            if (batch == null) {
                batch = new TextBatch(1000, zIndex);
                batch.start();
                if (projectionMatrix != null) {
                    batch.setProjectionMatrix(projectionMatrix);
                }

                if (viewMatrix != null) {
                    batch.setViewMatrix(viewMatrix);
                }

                textBatches.add(batch);
                Collections.sort(textBatches);
            }

            for (TextRenderer component : components) {
                batch.add(component);
            }
        }

        for (TextBatch batch : textBatches) {
            batch.render();
        }
    }

    public void removeText(TextRenderer component) {
        for (List<TextRenderer> components : textGroups.values()) {
            components.remove(component);
        }

        for (TextBatch batch : textBatches) {
            batch.removeComponent(component);
        }
    }

    public void cleanup() {
        textGroups.clear();
        textBatches.clear();
    }
}
