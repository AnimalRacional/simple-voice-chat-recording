package dev.omialien.voicechatrecording.api;

import java.util.Optional;
import java.util.UUID;

public interface IRecordedPlayer {
    boolean isSilent();
    long getLastSpoke();
    boolean isSpeaking();
    boolean isRecording();
    UUID getUuid();
    Optional<IRecordedAudio> forceFinishRecording();
}
