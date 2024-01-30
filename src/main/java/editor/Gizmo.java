package editor;

import TCB_Field.GameObject;
import TCB_Field.Prefab;
import TCB_Field.Window;
import components.Component;
import components.Sprite;
import components.SpriteRender;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class Gizmo extends Component {
    private Vector4f xAxisColor = new Vector4f(0.091f, 0.206f, 0.223f ,1.0f);
    private Vector4f xHover = new Vector4f(0.102f, 0.227f, 0.246f, 1.0f);
    private Vector4f yAxisColor = new Vector4f(0.223f,0.091f,0.091f,1.0f);
    private Vector4f yHover = new Vector4f(0.246f, 0.102f, 0.102f, 1.0f);

    private GameObject xAxisObj;
    private GameObject yAxisObj;
    private SpriteRender xAxisSpr;
    private SpriteRender yAxisSpr;
    private GameObject activeGameObj = null;
    private Properties properties;
    private Vector2f xOffset = new Vector2f(45.0f, -2.0f);
    private Vector2f yOffset = new Vector2f(14.0f, 45.0f);

    public Gizmo(Sprite arrowSprite, Properties properties) {
        this.xAxisObj = Prefab.genSpsObj(arrowSprite, 16, 32);
        this.yAxisObj = Prefab.genSpsObj(arrowSprite, 16, 32);
        this.xAxisSpr = this.xAxisObj.getComponent(SpriteRender.class);
        this.yAxisSpr = this.yAxisObj.getComponent(SpriteRender.class);
        this.properties = properties;

        Window.getScene().addObjToScene(this.xAxisObj);
        Window.getScene().addObjToScene(this.yAxisObj);

    }

    @Override
    public void start() {
        this.xAxisObj.transform.rotate = 90;
        this.yAxisObj.transform.rotate = 180;
        this.xAxisObj.isNotSerialize();
        this.yAxisObj.isNotSerialize();
    }

    @Override
    public void update(float dt) {
        if (this.activeGameObj != null) {
            this.xAxisObj.transform.position.set(this.activeGameObj.transform.position);
            this.yAxisObj.transform.position.set(this.activeGameObj.transform.position);
            this.xAxisObj.transform.position.add(this.xOffset);
            this.yAxisObj.transform.position.add(this.yOffset);
        }

        this.activeGameObj = this.properties.loadActiveObj();

        if (this.activeGameObj != null) {
            this.setActiveObj();
        } else {
            this.setInactiveObj();
        }
    }

    private void setActiveObj() {
        this.xAxisSpr.setColor(xAxisColor);
        this.yAxisSpr.setColor(yAxisColor);
    }

    private void setInactiveObj() {
        this.activeGameObj = null;
        this.xAxisSpr.setColor(new Vector4f(0, 0, 0 , 0));
        this.yAxisSpr.setColor(new Vector4f(0, 0, 0 , 0));
    }
}
