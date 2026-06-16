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


public class TCBFontTest {
    @Test
    public void loadFontCorrectly() throws InterruptedException {
        ResourceStatus status = awaitFont(Settings.FontPath.Caudex, GlyphRange.ASCII, 16, 16);
        assertEquals(ResourceStatus.READY, status);
    }

    @Test
    public void loadNoneExistingFontThrowIOException() throws InterruptedException {
        ResourceStatus status = awaitFont("engine://assets/fonts/NoneExistenceFile.ttf", GlyphRange.ASCII, 12, 10);
        assertEquals(ResourceStatus.FAILED, status);
    }

    @Test
    public void loadFontWithGlyphASCII_EXTENDED() throws InterruptedException {
        ResourceStatus status = awaitFont(Settings.FontPath.NotoSansMono, GlyphRange.ASCII_EXTENDED, 13, 16);
        assertEquals(ResourceStatus.READY, status);
    }

    private static ResourceStatus awaitFont(String path, GlyphRange range, float point, long timeOut) throws InterruptedException {
        AtomicReference<ResourceStatus> result = new AtomicReference<>();
        AtomicReference<ResourceID> RIDHolder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ResourceStatusListener listener = (RID, status) -> {
            if (RID.equals(RIDHolder.get()) && result.compareAndSet(null, status)) latch.countDown();
        };
        ResourceStatusCallback.register(listener);
        ResourceID RID = AssetManager.loadFont(path, range, point);
        RIDHolder.set(RID);
        TCBFont font = AssetManager.getFont(RID);
        if (font != null && font.loaded() && result.compareAndSet(null, ResourceStatus.READY)) latch.countDown();
        latch.await(timeOut, TimeUnit.SECONDS);
        ResourceStatusCallback.unregister(listener);
        return result.get();
    }
}
