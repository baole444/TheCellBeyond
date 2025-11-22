package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import physic2d.Physic2D;
import physic2d.PhysicLayer;

import java.util.ArrayList;
import java.util.List;

public record PhysicLayerName(List<String> layerNames) {
    @JsonIgnore
    public PhysicLayerName() {
        this(initDefaultLayer());
    }

    public PhysicLayerName {
        if (layerNames == null) {
            layerNames = initDefaultLayer();
        } else {
            List<String> normalized = new ArrayList<>(layerNames);
            while (normalized.size() < Physic2D.MaxLayer) {
                normalized.add("Layer " + normalized.size());
            }

            if (normalized.size() > Physic2D.MaxLayer) {
                normalized = normalized.subList(0, Physic2D.MaxLayer);
            }

            layerNames = List.copyOf(normalized);
        }
    }

    @JsonIgnore
    public String layerName(int layerIndex) {
        if (!PhysicLayer.isLayerIndexValid(layerIndex)) return "Invalid Layer";

        return layerNames.get(layerIndex);
    }

    @JsonIgnore
    public PhysicLayerName updateLayerName(int layerIndex, String newName) {
        if (!PhysicLayer.isLayerIndexValid(layerIndex)) return this;

        List<String> updated = new ArrayList<>(layerNames);
        if (newName == null || newName.isBlank()) newName = "Layer " + layerIndex;

        updated.set(layerIndex, newName.trim());
        return new PhysicLayerName(updated);
    }

    private static List<String> initDefaultLayer() {
        List<String> defaults = new ArrayList<>(Physic2D.MaxLayer);
        for (int i = 0; i < Physic2D.MaxLayer; i++) {
            defaults.add("Layer " + i);
        }

        return defaults;
    }
}
