package editor.dialogs;

import TheCellBeyond.InputKey;

import java.util.Set;

@FunctionalInterface
interface KeyComboCallback {
    void onResult(Set<InputKey> resultKeyCombo, boolean accepted);
}
