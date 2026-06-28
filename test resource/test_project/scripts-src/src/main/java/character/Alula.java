package character;

import components.AnimatedSpriteRenderer;
import physic2d.CharacterBody2D;
import scripting.Export;
import scripting.RegisterGameObject;
import states.AlulaEngine;
import utility.HierarchyPath;
import utility.HierarchyPaths;

@RegisterGameObject(description = "Player controlled object")
public class Alula extends CharacterBody2D {
    public enum Direction {
        Up,
        Down,
        Left,
        Right
    }

    @Export(label = "Direction")
    public Direction direction = Direction.Down;
    public final String AnimationPath = HierarchyPath.ComponentDelimiter + "Animation";
    public AnimatedSpriteRenderer animation;

    public final String WalkUp = "walk_up";
    public final String WalkDown = "walk_down";
    public final String WalkLeft = "walk_left";
    public final String WalkRight = "walk_right";

    @Override
    protected void onStart() {
        addComponent(new AlulaEngine());
    }

    @Override
    protected void onReady() {
        animation = (AnimatedSpriteRenderer) HierarchyPaths.toComponent(AnimationPath, this);
        updateAnimation();
    }

    @Override
    protected void onPhysicUpdate(float dt) {
        driveDirection();
        moveAndSlide();
    }

    public void updateAnimation() {
        if (animation == null) return;
        animation.setCurrentAnimation(switch (direction) {
            case Up -> WalkUp;
            case Down -> WalkDown;
            case Left -> WalkLeft;
            case Right -> WalkRight;
        });
    }

    private void driveDirection() {
        if (velocity.y < 0.0f) {
            direction = Direction.Down;
            return;
        }
        if (velocity.y > 0.0f) {
            direction = Direction.Up;
            return;
        }
        if (velocity.x < 0.0f) {
            direction = Direction.Left;
            return;
        }
        if (velocity.x > 0.0f) direction = Direction.Right;
    }
}
