package editor.dialog;

import TheCellBeyond.*;
import editor.ImGuiLayer;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ListenForInputDialog {
    private static final String POPUP_ID = "Listen For Input";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(400.0f, 160.0f);

    private static boolean showDialog = false;
    private static boolean listeningInput = false;

    private static final List<Integer> recordedKeys = new ArrayList<>();
    private static final Set<Integer> currentMods = new HashSet<>();
    private static KeyComboCallback callback = null;

    static void show(KeyComboCallback resultCallback) {
        show(resultCallback, null);
    }

    static void show(KeyComboCallback resultCallback, Set<InputKey> displayCurrentKeyCombo) {
        showDialog = true;
        callback = resultCallback;
        recordedKeys.clear();
        currentMods.clear();
        listeningInput = false;

        if (displayCurrentKeyCombo == null || displayCurrentKeyCombo.isEmpty()) return;
        for (InputKey key : displayCurrentKeyCombo) {
            recordedKeys.add(key.code());
        }
    }

    static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);
        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.spacing();
            ImGui.text("Click to record input:");
            ImGui.spacing();

            renderInputField();

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            renderButtons();

            ImGui.endPopup();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) closeDialog(false);
    }

    private static void renderInputField() {
        String displayText = getRecordedKeyName();
        String hint = listeningInput ? "Listening for input..." : "Click to record input...";

        ImString display = new ImString(displayText, 128);
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##KeyCombo_Input", hint, display, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        boolean isActive = ImGui.isItemActive();
        if (isActive && !listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(true);
            listeningInput = true;
            currentMods.clear();
        }

        if (!isActive && listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(false);
            listeningInput = false;
        }

        if (listeningInput) recordInput();
    }

    private static void renderButtons() {
        float buttonReserveY = ImGui.getFrameHeightWithSpacing();
        ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserveY - ImGui.getStyle().getWindowPaddingY());

        float buttonWidth = 120.0f;
        float buttonPivotX = buttonWidth * 0.5f;
        float availX = ImGui.getContentRegionAvailX();
        float confirmX = (availX * 0.25f) - buttonPivotX;
        float cancelX = (availX* 0.75f) - buttonPivotX;

        ImGui.setCursorPosX(confirmX);
        boolean hasKeys = !recordedKeys.isEmpty();
        if (!hasKeys) ImGui.beginDisabled();
        if (ImGui.button("Confirm##KeyCombo_Confirm", buttonWidth, 0.0f)) closeDialog(true);
        if (!hasKeys) ImGui.endDisabled();

        ImGui.sameLine();
        ImGui.setCursorPosX(cancelX);
        if (ImGui.button("Cancel##KeyCombo_Cancel", buttonWidth, 0.0f)) closeDialog(false);
    }

    private static void recordInput() {
        Set<Integer> newMods = new HashSet<>();
        for (int keyCode : Input.getModifierKeysCodes()) {
            if (KeyListener.isKeyPressed(keyCode)) {
                newMods.add(keyCode);
            }
        }

        for (int keyCode : KeyListener.getTappedKeyCode()) {
            if (Input.isModifierKey(keyCode)) continue;

            recordedKeys.clear();
            recordedKeys.addAll(newMods);
            recordedKeys.add(keyCode);
            return;
        }

        for (int keyCode : MouseListener.getPressedButtons()) {
            if (MouseListener.isDragging()) return;

            recordedKeys.clear();
            recordedKeys.addAll(newMods);
            recordedKeys.add(keyCode);
            return;
        }

        if (newMods.equals(currentMods)) return;
        currentMods.clear();
        currentMods.addAll(newMods);

        if (newMods.isEmpty()) return;
        recordedKeys.clear();
        recordedKeys.addAll(newMods);
    }

    private static String getRecordedKeyName() {
        if (recordedKeys.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < recordedKeys.size(); i++) {
            if (i > 0) builder.append(" + ");
            int keyCode = recordedKeys.get(i);
            String name = Input.getKeyName(keyCode);
            builder.append(name != null ? name : "Unknown");
        }

        return builder.toString();
    }

    private static void closeDialog(boolean confirmed) {
        if (listeningInput) {
            ImGuiLayer.prioritizeEngineInputCallback(false);
            listeningInput = false;
        }

        if (callback != null) {
            Set<InputKey> result = confirmed && !recordedKeys.isEmpty() ? recordedKeys.stream()
                    .map(code -> Input.isMouseButton(code) ? new InputKey(InputType.Mouse, code) : new InputKey(InputType.Keyboard, code))
                    .collect(Collectors.toSet()) : null;

            callback.onResult(result, confirmed);
        }

        showDialog = false;
        callback = null;
        recordedKeys.clear();
        currentMods.clear();
        ImGui.closeCurrentPopup();
        EditProjectPreferencesDialog.closeListForInputDialog();
    }
}
