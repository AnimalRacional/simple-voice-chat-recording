package dev.omialien.voicechatrecording_api;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

public interface IRecordedAudio {
    enum FilterResult {
        PASSED,
        TOO_LONG,
        TOO_SHORT,
        NO_ACTIVE_AUDIO,
        LOW_RMS
    }

    /**
     * Returns the underlying audio:
     * do not modify this! It will
     * also be modified for all other mods, instead
     * use {@link Arrays#clone()} if you must modify the audio.
     * @return the underlying audio
     */
    short[] getAudio();

    /**
     * Gets the player this audio was recorded by
     * @return the UUID of the player this audio was recorded by
     */
    UUID getPlayerUUID();

    /**
     * Returns the ID of this audio, which, in conjunction with the player's UUID, identifies it
     * @return the ID of this audio
     */
    UUID getId();

    // TODO better explanation
    /**
     * Marks this audio as persistent through server shutdowns
     * @param namespace this should usually be your mod id
     */
    boolean saveAudio(String namespace);
    // TODO command for this
    /**
     * Marks this audio as no longer persistent through server shutdowns
     * @param namespace this should usually be your mod id
     */
    void unsaveAudio(String namespace);
    /**
     * Gets the underlying audio with the applied effects
     * @param effects the effects applied to the audio
     * @return the unchanged audio if effect is null, a copy of the audio with the effects applied otherwise
     */
    short[] applyEffects(@Nullable AudioEffect effects);

    /**
     * Gets whether this audio was filtered
     * @return a FilterResult specifying if the audio was filtered, and if so, why
     */
    FilterResult getFilterResult();

    /**
     * Gets the duration, in seconds, of the audio
     * @return the duration, in seconds, of the audio
     */
    double getDuration();

    /**
     * Gets the duration, in seconds, of active audio (audio with non-negligible volume)
     * @return the duration, in seconds, of active audio
     */
    double getActiveDuration();

    /**
     * Gets the amount of active samples this audio has, which, with the sample rate, can be used to get the duration of active audio
     * @return the amount of active samples in the audio
     */
    int getActiveSamples();

    /**
     * Gets the root mean square of the audio, which can be used to represent its loudness
     * @return the RMS of the audio
     */
    double getRms();
}
