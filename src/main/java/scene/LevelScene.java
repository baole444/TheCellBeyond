package scene;

import TCB_Field.Window;

public class LevelScene extends Scene {
    public LevelScene() {
        System.out.println("Scene loaded");
        Window.get().r = 1;
        Window.get().g = 1;
        Window.get().b = 1;
    }

    @Override
    public void update(float dt) {

    }
    @Override
    public void render() {
    }
}
