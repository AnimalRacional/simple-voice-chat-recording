package dev.omialien.voicechat_recording.voicechat;

import java.util.UUID;

public interface IRecordedPlayer {
    boolean isSilent();
    long getLastSpoke();
    boolean isSpeaking();
    boolean isRecording();
    UUID getUuid();
}
