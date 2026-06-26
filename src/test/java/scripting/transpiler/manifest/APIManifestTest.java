package scripting.transpiler.manifest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class APIManifestTest {
    @Test
    public void loadsBundledManifest() {
        assertEquals(APIManifest.CurrentManifestVersion, APIManifest.manifestVersion());
        assertNotNull(APIManifest.apiVersion(), "apiVersion should be populated");
    }

    @Test
    public void resolvesClassBySimpleName() {
        Optional<APIType> found = APIManifest.findClassBySimpleName("CharacterBody2D");
        assertTrue(found.isPresent(), "CharacterBody2D should resolve by simple name");
        assertEquals("physic2d.CharacterBody2D", found.get().fqn);
        assertEquals(APIType.Kind.Class, found.get().kind, "kind token should deserialize to the enum");
    }

    @Test
    public void detectsLineage() {
        assertTrue(APIManifest.inGameObjectLineage("physic2d.CharacterBody2D"));
        assertTrue(APIManifest.inComponentLineage("components.AnimatedSpriteRenderer"));
        assertFalse(APIManifest.inComponentLineage("physic2d.CharacterBody2D"));
        assertFalse(APIManifest.inGameObjectLineage("components.AnimatedSpriteRenderer"));
    }

    @Test
    public void recordsFinalModifier() {
        assertTrue(APIManifest.findClassBySimpleName("Input").orElseThrow().isFinal, "Input is a final class");
        assertFalse(APIManifest.findClassBySimpleName("CharacterBody2D").orElseThrow().isFinal, "CharacterBody2D is extendable");
    }

    @Test
    public void recordsAbstractModifier() {
        assertTrue(APIManifest.findClassBySimpleName("PhysicBody2D").orElseThrow().isAbstract, "PhysicBody2D is an abstract class");
        assertFalse(APIManifest.findClassBySimpleName("CharacterBody2D").orElseThrow().isAbstract, "CharacterBody2D is concrete");
    }

    @Test
    public void resolvesSnakeAliasedMember() {
        Optional<MemberInfo> member = APIManifest.findAPIMember("physic2d.CharacterBody2D", "move_and_slide");
        assertTrue(member.isPresent(), "move_and_slide should resolve to moveAndSlide");
        assertEquals("moveAndSlide", member.get().javaName());
    }
}
