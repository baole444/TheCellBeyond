package TheCellBeyond;

import TheCellBeyond.internal.ResourceID;
import TheCellBeyond.internal.ResourceStatus;
import TheCellBeyond.internal.ResourceStatusCallback;
import org.lwjgl.system.MemoryStack;
import utility.AssetReference;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class Sound {
    public final ResourceID RID = new ResourceID(AudioResourceType.Clip);
    private int bufferId;
    private int sourceId;
    private final AssetReference assetReference;
    private transient boolean isPlaying = false;

    public Sound(String filepath, boolean isLoop) {
        assetReference = new AssetReference(filepath);
        if (Window.noAudioSupport()) {
            ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);
            ShortBuffer rawSoundBuffer = stb_vorbis_decode_filename(filepath, channelBuffer, sampleRateBuffer);
            if (rawSoundBuffer == null) {
                System.out.println("Error: failed to load sound file '" + filepath + "'");
                ResourceStatusCallback.emit(RID, ResourceStatus.FAILED);
                return;
            }
            int channels = channelBuffer.get();
            int sampleRate = sampleRateBuffer.get();
            int format = -1;
            if (channels == 1) format = AL_FORMAT_MONO16;
            else if (channels == 2) format = AL_FORMAT_STEREO16;
            bufferId = alGenBuffers();
            alBufferData(bufferId, format, rawSoundBuffer, sampleRate);
            sourceId = alGenSources();
            alSourcei(sourceId, AL_BUFFER, bufferId);
            alSourcei(sourceId, AL_LOOPING, isLoop ? 1 : 0);
            alSourcei(sourceId, AL_POSITION, 0);
            alSourcef(sourceId, AL_GAIN, 1f);
            free(rawSoundBuffer);
        }
        ResourceStatusCallback.emit(RID, ResourceStatus.READY);
    }

    public void dispose() {
        if (Window.noAudioSupport()) return;
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
        ResourceStatusCallback.emit(RID, ResourceStatus.DISPOSED);
        RID.release();
    }

    public void play() {
        if (Window.noAudioSupport()) return;
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
            alSourcei(sourceId, AL_POSITION, 0);
        }
        if (isPlaying) return;
        alSourcePlay(sourceId);
        isPlaying = true;
    }

    public void stop() {
        if (Window.noAudioSupport() || !isPlaying) return;
        alSourceStop(sourceId);
        isPlaying = false;
    }

    public String getFilepath() {
        return assetReference != null ? assetReference.canonicalPath() : null;
    }

    public boolean isPlaying() {
        if (Window.noAudioSupport()) return;
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) isPlaying = false;
        return isPlaying;
    }
}
