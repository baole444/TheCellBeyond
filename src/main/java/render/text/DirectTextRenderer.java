package render.text;

import components.TextComponent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import utility.Settings;

import java.io.IOException;
import java.util.*;

public class DirectTextRenderer {
    private static DirectTextRenderer instance;

    private List<TextBatch> textBatches = new ArrayList<>();

    private Map<String, List<TextComponent>> textGroups = new HashMap<>();

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

    public TextComponent drawText(String text, float x, float y, int fontSize, Vector4f color, String fontPath, int zIndex) {
        if (fontPath == null) {
            fontPath = Settings.PATH.CONSOLA;
        }

        try {
            TCBFont font = FontManager.get().loadFont(fontPath, fontSize, false);

            TextComponent textComponent = new TextComponent(text, fontPath, fontSize, color, new Vector2f(x, y));

            String groupKey = zIndex + "_" + fontPath + "_" + fontSize;
            textGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(textComponent);

            return textComponent;
        } catch (IOException e) {
            System.err.println("Failed to render text: " + e.getMessage());
            return null;
        }
    }

    public TextComponent drawText(String text, float x, float y, int fontSize, Vector4f color) {
        return drawText(text, x, y, fontSize, color, null, 0);
    }

    public TextComponent drawText(String text, float x, float y, int fontSize) {
        return drawText(text, x, y, fontSize, new Vector4f(1, 1, 1, 1));
    }

    public void render() {
        for (Map.Entry<String, List<TextComponent>> entry : textGroups.entrySet()) {
            String[] parts = entry.getKey().split("_");
            int zIndex = Integer.parseInt(parts[0]);

            List<TextComponent> components = entry.getValue();
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

            for (TextComponent component : components) {
                batch.add(component);
            }
        }

        for (TextBatch batch : textBatches) {
            batch.render();
        }

        textGroups.clear();
    }

    public void cleanup() {
        textGroups.clear();
        textBatches.clear();
    }
}
