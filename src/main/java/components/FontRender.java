package components;

import TCB_Field.Component;

public class FontRender extends Component {

    @Override
    public void start() {
        if (gameObject.getComponent(SpriteRender.class) != null) {
            System.out.println("Loading font.");

        }
    }
    @Override
    public void update(float dt) {

    }
}
