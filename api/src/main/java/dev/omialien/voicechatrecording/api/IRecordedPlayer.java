package dev.omialien.voicechatrecording.api;

import java.util.UUID;

public interface IRecordedPlayer {
    /**
     * Checks if the player hasn't recently spoken
     * @return false if the player has recently (~1s) spoken
     */
    boolean isSilent();

    /**
     * Gets the time at which the player last spoke
     * @return the time at which the player last spoke, gotten through {@link System#currentTimeMillis()} at the time of speaking
     */
    long getLastSpoke();

    /**
     * Checks if the player has recently spoken
     * @return whether the player has recently (~1.2s) spoken
     */
    boolean isSpeaking();

    /**
     * Checks if the player is currently being recorded by the mod
     * As of right now, this will always return true for a connected player
     * @return whether the player is currently being recorded by the mod
     */
    boolean isRecording();

    /**
     * @return the UUID of the player
     */
    UUID getUuid();
}
