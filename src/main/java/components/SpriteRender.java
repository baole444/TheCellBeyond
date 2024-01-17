package components;

import TCB_Field.Component;

public class SpriteRender extends Component {
    private boolean isFirst = false;
    @Override
    public void start() {
        System.out.println("Loading sprite.");

    }
    @Override
    public void update(float dt) {
        if (!isFirst) {
            System.out.println("Sprite updating.");
            isFirst = true;
        }

    }
}
