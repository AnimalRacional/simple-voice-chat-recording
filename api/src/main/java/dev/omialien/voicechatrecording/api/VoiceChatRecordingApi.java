package dev.omialien.voicechatrecording.api;

import com.mojang.datafixers.util.Pair;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface VoiceChatRecordingApi {
    /**
     * Loads all audios of the given namespace from disk, passing them to {@param reaction}
     * @param namespace the namespace to identify audios to load
     * @param reaction a consumer to react to the loaded {@link IRecordedAudio}; if an error occurs, the audio will be null
     * @return a set of futures of the loaded audios
     */
    Set<Future<IRecordedAudio>> loadNamespaceAudios(String namespace, Consumer<IRecordedAudio> reaction);
    /**
     * Loads all audios of the given namespace from disk
     * @param namespace the namespace to identify audios to load
     * @return a set of futures of the loaded audios. If an error occurs, the audio will be null
     * Also see {@link VoiceChatRecordingApi#loadNamespaceAudios(String, Consumer)}
     */
    Set<Future<IRecordedAudio>> loadNamespaceAudios(String namespace);

    /**
     * Gets the identifiers of all the audios saved by a specific namespace
     * This doesn't actually load the audios from disk, use
     * {@link VoiceChatRecordingApi#loadNamespaceAudios(String, Consumer)} or {@link VoiceChatRecordingApi#loadAudio(UUID, UUID)} for that
     * @param namespace the namespace to retrieve audios from
     * @return a set of all the identifiers of the audios saved to the given namespace
     */
    Set<Pair<UUID, UUID>> getNamespaceAudios(String namespace);
    /**
     * Loads an audio and returns a {@link Future} which will return the audio, also passing the loaded audio to {@param reaction}
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @param reaction a consumer that will receive the loaded audio
     * @return a future which will return the loaded audio or null if an error occurs
     */
    Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId, Consumer<IRecordedAudio> reaction);
    /**
     * Loads an audio and returns a Future which will return the audio
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @return a future which will return the loaded audio or null if an error occurs
     */
    Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId);
    // TODO add loadPlayerAudios with a namespace argument
    /**
     * Loads all available audios of a given player and passes them to the given consumer
     * @param playerUuid The UUID of the player to load audios of
     * @param reaction The reaction to pass the audio to
     * @return a set of futures which will return either the loaded audios or null if an error occurred
     */
    Set<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid, Consumer<IRecordedAudio> reaction);
    /**
     * Loads all available audios of a given player
     * @param playerUuid the UUID of the player to get audios of
     * @return a set of futures which will return either the player's audio or null if an error occured
     */
    Set<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid);

    /**
     * Unsaves an audio: when all namespaces that saved an audio do this, its file will be deleted
     * @param namespace the namespace from which to unsave the audio
     * @param audio the audio to unsave
     */
    void unsaveAudio(String namespace, IRecordedAudio audio);
    IRecordedPlayer getRecordedPlayer(UUID uuid);
    boolean getPrivacy(UUID uuid);

}
