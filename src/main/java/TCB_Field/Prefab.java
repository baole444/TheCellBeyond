package TCB_Field;

import components.*;
import physic_2d.components.FlatPhysicBody;
import physic_2d.components.collider.PillBoxCollider;
import physic_2d.enums.ObjectClassification;
import utility.AssetsPool;

public class Prefab {
    public static GameObject genSpsObj(Sprite sprite, float sizeX, float sizeY) {
        GameObject block = Window.getScene().generateObject("Sprite_object_gen");
        block.transform.scale.x = sizeX;
        block.transform.scale.y = sizeY;

        SpriteRender render = new SpriteRender();
        render.setSprite(sprite);
        block.addComponent(render);

        return block;
    }
    // TODO: a universal animation generator, which read from a separated yml for the animation.
    public static GameObject genWheelSpin() {
        SpriteSheet wheelSprites = AssetsPool.loadSpSheet("assets/texture/animation_test.png");
        GameObject wheelSpin = genSpsObj(wheelSprites.spriteIndex(0), 0.32f, 0.32f);

        AnimationState spin = new AnimationState();

        spin.title = "Spin";
        float defaultFrameTime = 0.2f;
        spin.addFrame(wheelSprites.spriteIndex(0), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(1), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(2), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(3), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(4), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(5), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(6), defaultFrameTime);
        spin.addFrame(wheelSprites.spriteIndex(7), defaultFrameTime);

        spin.setLoop(true);

        StateEngine stateEngine = new StateEngine();
        stateEngine.addState(spin);
        stateEngine.setDefaultState(spin.title);

        wheelSpin.addComponent(stateEngine);

        PillBoxCollider pillBoxCollider = new PillBoxCollider();
        pillBoxCollider.setWidth(0.32f);
        pillBoxCollider.setHeight(0.32f);
        FlatPhysicBody flatPhysicBody = new FlatPhysicBody();
        flatPhysicBody.setObjectClassification(ObjectClassification.Dynamic);
        flatPhysicBody.setNoneStopCollision(false);
        flatPhysicBody.setMass(10.0f);

        wheelSpin.addComponent(flatPhysicBody);
        wheelSpin.addComponent(pillBoxCollider);
        wheelSpin.addComponent(new CharacterControl());

        return wheelSpin;
    }
}
