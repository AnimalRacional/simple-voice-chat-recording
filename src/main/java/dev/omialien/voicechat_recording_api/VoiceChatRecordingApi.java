package dev.omialien.voicechat_recording_api;

import com.mojang.datafixers.util.Pair;
import dev.omialien.voicechat_recording.voicechat.RecordedAudio;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface VoiceChatRecordingApi {
    void saveAudio(String namespace, RecordedAudio audio);
    List<Future<RecordedAudio>> loadNamespaceAudios(String namespace, Consumer<RecordedAudio> reaction);
    List<Future<RecordedAudio>> loadNamespaceAudios(String namespace);
    Set<Pair<UUID, UUID>> getNamespaceAudios(String namespace);
    Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId, Consumer<RecordedAudio> reaction);
    Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId);
    List<Future<RecordedAudio>> loadPlayerAudios(UUID playerUuid, Consumer<RecordedAudio> reaction);
    List<Future<RecordedAudio>> loadPlayerAudios(UUID playerUuid);
    IRecordedPlayer getRecordedPlayer(UUID uuid);
    boolean getPrivacy(UUID uuid);


}
