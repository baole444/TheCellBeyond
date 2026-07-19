package render.text;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import TheCellBeyond.internal.ResourceStatusListener;
import org.junit.jupiter.api.Test;
import utility.AssetManager;
import utility.Settings;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FontUnloadTest {
    private static final String Face = Settings.FontPath.NotoSansRegular;
    private static final GlyphRange Range = GlyphRange.ASCII_EXTENDED;
    /**
     * Wait time of up to 20 seconds.
     */
    private static final long TimeoutMillis = 20000;

    @Test
    public void unloadingAFontDisposesTheAtlasOnlyOnceNoSizeUsesIt() throws InterruptedException {
        TCBFont small = loadAndAwait(12);
        ResourceID smallRID = small.RID;
        ResourceID atlasRID = small.atlasRID();
        assertNotNull(AssetManager.getFontAtlas(atlasRID));
        TCBFont large = loadAndAwait(16);
        ResourceID largeRID = large.RID;
        assertEquals(atlasRID, large.atlasRID(), "point sizes of one face should share one atlas");
        AssetManager.unloadFont(Face, Range, 12);
        assertNull(AssetManager.getFont(smallRID), "the unloaded font should no longer resolve");
        assertNotNull(AssetManager.getFontAtlas(atlasRID), "the atlas should survive while another size uses it");
        assertNotNull(large.charUV('A'), "the remaining size should stay usable");
        AssetManager.unloadFont(Face, Range, 16);
        assertNull(AssetManager.getFont(largeRID));
        assertNull(AssetManager.getFontAtlas(atlasRID), "the atlas should be disposed with its last font");
        TCBFont reloaded = loadAndAwait(20);
        assertNotEquals(atlasRID, reloaded.atlasRID(), "a reload must not reuse the disposed atlas");
        assertNotNull(AssetManager.getFontAtlas(reloaded.atlasRID()), "the reloaded font's atlas should resolve");
        assertNotNull(reloaded.charUV('A'));
    }

    private static TCBFont loadAndAwait(float points) throws InterruptedException {
        AtomicReference<ResourceStatus> result = new AtomicReference<>();
        AtomicReference<ResourceID> RIDHolder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ResourceStatusListener listener = (RID, status) -> {
            if (RID.equals(RIDHolder.get()) && result.compareAndSet(null, status)) latch.countDown();
        };
        ResourceStatusCallback.register(listener);
        try {
            ResourceID RID = AssetManager.loadFont(Face, Range, points);
            RIDHolder.set(RID);
            TCBFont font = AssetManager.getFont(RID);
            assertNotNull(font, "the font should be registered as soon as its RID is issued");

            if (font.loaded() && result.compareAndSet(null, ResourceStatus.Ready)) latch.countDown();
            assertTrue(latch.await(TimeoutMillis, TimeUnit.MILLISECONDS), "font " + RID + " did not load in time");
            assertEquals(ResourceStatus.Ready, result.get(), "font " + RID + " ended in a non-ready status");
            return font;
        } finally {
            ResourceStatusCallback.unregister(listener);
        }
    }
}
