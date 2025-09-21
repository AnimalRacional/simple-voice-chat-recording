package dev.omialien.voicechat_recording_api;

import java.util.UUID;

public interface IRecordedPlayer {
    boolean isSilent();
    long getLastSpoke();
    boolean isSpeaking();
    boolean isRecording();
    UUID getUuid();
}
