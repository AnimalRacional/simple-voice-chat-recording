package dev.omialien.voicechatrecording_api;

import com.mojang.datafixers.util.Pair;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface VoiceChatRecordingApi {
    List<Future<IRecordedAudio>> loadNamespaceAudios(String namespace, Consumer<IRecordedAudio> reaction);
    List<Future<IRecordedAudio>> loadNamespaceAudios(String namespace);
    Set<Pair<UUID, UUID>> getNamespaceAudios(String namespace);
    Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId, Consumer<IRecordedAudio> reaction);
    Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId);
    List<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid, Consumer<IRecordedAudio> reaction);
    List<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid);
    IRecordedPlayer getRecordedPlayer(UUID uuid);
    boolean getPrivacy(UUID uuid);

}
