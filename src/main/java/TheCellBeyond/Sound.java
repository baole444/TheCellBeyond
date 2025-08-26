package TheCellBeyond;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class Sound {
    private int bufferId;
    private int sourceId;
    private final String filepath;

    private boolean isPlaying = false;

    public Sound(String filepath, boolean isLoop) {
        this.filepath = filepath;

        // push to allocate memory for values from stb
        stackPush();
        IntBuffer channelBuffer = stackMallocInt(1);
        stackPush();
        IntBuffer sampleRateBuffer = stackMallocInt(1);

        ShortBuffer rawSoundBuffer = stb_vorbis_decode_filename(filepath, channelBuffer, sampleRateBuffer);

        if (rawSoundBuffer == null) {
            System.out.println("Error: failed to load sound file '" + filepath + "'");
            stackPop();
            stackPop();
            return;
        }

        // Collect information from buffer
        int channels = channelBuffer.get();
        int sampleRate = sampleRateBuffer.get();

        // Free stack
        stackPop();
        stackPop();

        // Determine a correct AL format from channels;
        int format = -1;
        if (channels == 1) {
            format = AL_FORMAT_MONO16;
        } else if (channels == 2) {
            format = AL_FORMAT_STEREO16;
        }

        bufferId = alGenBuffers();
        alBufferData(bufferId, format, rawSoundBuffer, sampleRate);

        // Create the sound source
        sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcei(sourceId, AL_LOOPING, isLoop ? 1 : 0);
        alSourcei(sourceId, AL_POSITION, 0);
        alSourcef(sourceId, AL_GAIN, 1f);

        // Free raw audio buffer
        free(rawSoundBuffer);
    }

    public void delete() {
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    public void play() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
            alSourcei(sourceId, AL_POSITION, 0);
        }

        if (!isPlaying) {
            alSourcePlay(sourceId);
            isPlaying = true;
        }
    }

    public void stop() {
        if (isPlaying) {
            alSourceStop(sourceId);
            isPlaying = false;
        }
    }

    public String getFilepath() {
        return this.filepath;
    }

    public boolean isPlaying() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
        }
        return  isPlaying;
    }
}
