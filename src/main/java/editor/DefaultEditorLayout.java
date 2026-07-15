package editor;

import imgui.flag.ImGuiDir;
import imgui.internal.ImGui;
import imgui.internal.ImGuiDockNode;
import imgui.internal.flag.ImGuiDockNodeFlags;
import imgui.type.ImInt;

final class DefaultEditorLayout {
    private static final float SIDE_COLUMNS = 0.2f;
    private static final float BOTTOM_LEFT_ROW = 0.4f;
    private static final float BOTTOM_CENTRE_ROW = 0.3f;

    static void resetLayout(int dockID) {
        ImGui.dockBuilderRemoveNode(dockID);
        ImGui.dockBuilderAddNode(dockID, ImGuiDockNodeFlags.DockSpace);
        ImGui.dockBuilderSetNodeSize(dockID, ImGui.getMainViewport().getSize());

        ImInt mainDock = new ImInt(dockID);
        ImInt leftDock = new ImInt();
        ImInt rightDock = new ImInt();
        ImInt bottomLeftDock = new ImInt();
        ImInt bottomMainDock = new ImInt();

        ImGui.dockBuilderSplitNode(mainDock.get(), ImGuiDir.Left, SIDE_COLUMNS, leftDock, mainDock);
        ImGui.dockBuilderSplitNode(mainDock.get(), ImGuiDir.Right, SIDE_COLUMNS / (1.f - SIDE_COLUMNS), rightDock, mainDock);
        ImGui.dockBuilderSplitNode(leftDock.get(), ImGuiDir.Down, BOTTOM_LEFT_ROW, bottomLeftDock, leftDock);
        ImGui.dockBuilderSplitNode(mainDock.get(), ImGuiDir.Down, BOTTOM_CENTRE_ROW, bottomMainDock, mainDock);

        ImGuiDockNode bottomPanel = ImGui.dockBuilderGetNode(bottomMainDock.get());
        bottomPanel.addLocalFlags(ImGuiDockNodeFlags.NoTabBar);
        ImGuiDockNode resourcePanel = ImGui.dockBuilderGetNode(bottomLeftDock.get());
        resourcePanel.addLocalFlags(ImGuiDockNodeFlags.NoTabBar);

        ImGui.dockBuilderDockWindow(SceneTree.WindowID, leftDock.get());
        ImGui.dockBuilderDockWindow(ResourcePanel.WindowID, bottomLeftDock.get());
        ImGui.dockBuilderDockWindow(SceneEditorViewport.WindowID, mainDock.get());
        ImGui.dockBuilderDockWindow(BottomPanel.WindowID, bottomMainDock.get());
        ImGui.dockBuilderDockWindow(Properties.WindowID, rightDock.get());

        ImGui.dockBuilderFinish(dockID);
    }

    public static boolean dockingValid(int dockID) {
        ImGuiDockNode dock = ImGui.dockBuilderGetNode(dockID);
        return !dock.isEmpty();
    }
}
