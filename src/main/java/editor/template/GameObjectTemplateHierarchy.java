package editor.template;

import TheCellBeyond.*;
import physic2d.*;

final class GameObjectTemplateHierarchy {
    /**
     * Handle switching to correct template path for the object.
     * IF the object is null or is removed, this return early.
     * <p>
     * The order of the cases are from subclasses on same inheritance tier to super classes.
     * @param go the context object
     */
    static void render(GameObject go) {
        if (go == null || go.isDestroyed()) return;
        switch (go) {
            case Area2D area2D -> Area2DTemplate.render(area2D);
            case Camera2D camera2D -> Camera2DTemplate.render(camera2D);
            case Parallax2D parallax2D -> Parallax2DTemplate.render(parallax2D);
            case TileMap tileMap -> TileMapTemplate.render(tileMap);
            case RayCast2D rayCast2D -> RayCast2DTemplate.render(rayCast2D);
            case CharacterBody2D characterBody2D -> CharacterBody2DTemplate.render(characterBody2D);
            case KinematicBody2D kinematicBody2D -> KinematicBody2DTemplate.render(kinematicBody2D);
            case RigidBody2D rigidBody2D -> RigidBody2DTemplate.render(rigidBody2D);
            case PhysicBody2D physicBody2D -> PhysicBody2DTemplate.render(physicBody2D);
            case CollisionObject2D collisionObject2D -> CollisionObject2DTemplate.render(collisionObject2D);
            case GameObject2D gameObject2D -> GameObject2DTemplate.render(gameObject2D);
            case RenderableObject renderableObject -> RenderableObjectTemplate.render(renderableObject);
            default -> {}
        }
    }
}
