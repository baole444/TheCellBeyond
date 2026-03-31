package editor.template;

import components.*;
import physic2d.collider.BoxCollider2D;
import physic2d.collider.CapsuleCollider2D;
import physic2d.collider.CircleCollider2D;

final class ComponentTemplateHierarchy {
    static void render(Component c) {
        if (c == null || c.isDestroyed() || c instanceof NotSerializeComponent) return;
        switch (c) {
            case AnimatedSpriteRenderer animatedSpriteRenderer -> AnimatedSpriteRendererTemplate.render(animatedSpriteRenderer);
            case BoxCollider2D boxCollider2D -> BoxCollider2DTemplate.render(boxCollider2D);
            case CircleCollider2D circleCollider2D -> CircleCollider2DTemplate.render(circleCollider2D);
            case CapsuleCollider2D capsuleCollider2D -> CapsuleCollider2DTemplate.render(capsuleCollider2D);
            case RemoteTransform2D remoteTransform2D -> RemoteTransform2DTemplate.render(remoteTransform2D);
            case SpriteRenderer spriteRenderer -> SpriteRendererTemplate.render(spriteRenderer);
            case TextRenderer textRenderer -> TextRendererTemplate.render(textRenderer);
            case Component2D component2D -> Component2DTemplate.render(component2D);
            case Controller2D controller2D -> Controller2DTemplate.render(controller2D);
            default -> {}
        }
    }
}
