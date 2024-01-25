package TCB_Field;

import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;

public class ImGuiLayer {

    public ImGuiLayer() {}
    public void guiFont(ImGuiIO io) {
        final ImFontAtlas fontAtlas = io.getFonts();

        // Font config must be destroyed after call
        final ImFontConfig fontConfig = new ImFontConfig();


        // glyphs range
        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesDefault());

        //Merge font
        //fontConfig.setMergeMode(true); //For multiple font, turn this back on
        fontConfig.setPixelSnapH(true);

        fontAtlas.addFontFromFileTTF("assets/fonts/Consola.ttf", 20, fontConfig);

        fontConfig.destroy();
    }

    public void update(float dt, Scene currentScene, ImGuiIO io, ImGuiImplGlfw imGuiGlfw, ImGuiImplGl3 imGuiGl3) {
        io.setDeltaTime(dt);
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        currentScene.sceneImGui();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
    }

}
