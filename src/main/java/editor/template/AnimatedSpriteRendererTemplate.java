package editor.template;

import components.AnimatedSpriteRenderer;
import components.Component2D;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Template for {@link AnimatedSpriteRenderer}'s editor UI.
 */
final class AnimatedSpriteRendererTemplate implements IComponentTemplate<AnimatedSpriteRenderer> {
    private static final AnimatedSpriteRendererTemplate instance = new AnimatedSpriteRendererTemplate();
    private AnimatedSpriteRendererTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(AnimatedSpriteRenderer component) {
        ImGui.spacing();
        UUID uuid = component.getUUID();
        boolean openAnimatedSprite = ImGui.collapsingHeader("AnimatedSpriteRenderer##Animated_Sprite_Renderer_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openAnimatedSprite) return;
        List<String> animationList = component.animations().keySet().stream().toList();
        String selectedAni = component.currentAnimationName();
        ImGui.text("Animation:");
        if (ImGui.beginCombo("##Select_Current_AnimatedSprite_Animation_Combo_" + uuid, selectedAni == null ? "Select an animation..." : selectedAni)) {
            for (String name : animationList) {
                String label = name + "##Select_" + name + "_AnimatedSprite_Selectable_" + uuid;
                if (ImGui.selectable(label, Objects.equals(name, selectedAni))) component.setCurrentAnimation(name);
            }
            if (!animationList.isEmpty()) ImGui.separator();
            if (ImGui.selectable("New animation...##New_Animation_AnimatedSprite_Selectable_" + uuid, false)) component.newAnimation();
            ImGui.endCombo();
        }
        ImGui.spacing();
        ImBoolean flipHState = new ImBoolean(component.flipHorizontally());
        ImBoolean flipVState = new ImBoolean(component.flipVertically());
        String compositeID = "Flip axis##Flip_Axis_AnimatedSprite_Header" + uuid;
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openFlip = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (!openFlip) return;
        if (ImGui.checkbox("Horizontal##HorizontalFlip_" + uuid, flipHState)) component.flipHorizontally(flipHState.get());
        if (ImGui.checkbox("Vertical##VerticalFlip_" + uuid, flipVState)) component.flipVertically(flipVState.get());
    }

    /**
     * Render the content of {@link #editorUI(AnimatedSpriteRenderer)} and call {@link Component2DTemplate#render(Component2D)}.
     * @param animatedSpriteRenderer the context component
     */
    static void render(AnimatedSpriteRenderer animatedSpriteRenderer) {
        if (animatedSpriteRenderer == null) return;
        instance.editorUI(animatedSpriteRenderer);
        Component2DTemplate.render(animatedSpriteRenderer);
    }
}
