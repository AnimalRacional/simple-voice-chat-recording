package dev.omialien.voicechat_recording.voicechat;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.audio.AudioDirectoryReader;
import dev.omialien.voicechat_recording.voicechat.events.AudioLoadedEvent;
import dev.omialien.voicechat_recording.voicechat.events.MicPacketReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

@ForgeVoicechatPlugin
public class VoiceChatRecordingPlugin implements VoicechatPlugin {
    private static Map<UUID, RecordedPlayer> recordedPlayers;
    private static Map<UUID, Boolean> privacyMode;
    private static Queue<VolumeCategory> categories;
    private static ExecutorService audioLoader;

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return VoiceChatRecording.MOD_ID;
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        audioLoader = Executors.newFixedThreadPool(4);
        VoiceChatRecording.LOGGER.info("Voice chat recording plugin initialized!");
        if(api instanceof VoicechatServerApi napi){
            VoiceChatRecording.LOGGER.info("Server Voice Chat API");
            VoiceChatRecording.vcApi = napi;
        } else {
            VoiceChatRecording.LOGGER.info("Client Voice Chat API");
        }
        categories = new LinkedList<>();
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket, 100);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted, 100);
        registration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnected, 100);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected, 100);
    }

    private void onMicrophonePacket(MicrophonePacketEvent e){
        if (e.getSenderConnection() != null){ // If it's a player and not an entity
            RecordedPlayer recordedPlayer = recordedPlayers.get(e.getSenderConnection().getPlayer().getUuid());
            recordedPlayer.recordPacket(e.getPacket().getOpusEncodedData());
            MicPacketReceivedEvent ev = new MicPacketReceivedEvent(e);
            NeoForge.EVENT_BUS.post(ev);
        }
    }

    public enum LoadType {
        SINGLE,
        ALL_FROM_USER
    }

    /**
     * Loads an audio and gives it to a consumer
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @param reaction the consumer to receive the loaded audio (or null)
     */
    public void loadAudio(UUID playerUuid, UUID audioId, Consumer<RecordedAudio> reaction){
        Path audioPath = RecordedAudio.audiosPath.resolve(playerUuid.toString()).resolve(audioId.toString());
        if(Files.exists(audioPath)){
            VoiceChatRecording.LOGGER.debug("Loading audio {} from {}", audioId, playerUuid);
            audioLoader.submit(() -> {
                short[] audio = AudioDirectoryReader.getFile(audioPath);
                if(audio != null){
                    RecordedAudio recAudio = new RecordedAudio(audio, playerUuid, audioId);
                    NeoForge.EVENT_BUS.post(new AudioLoadedEvent(recAudio, LoadType.SINGLE));
                    reaction.accept(recAudio);
                } else {
                    VoiceChatRecording.LOGGER.error("Error loading audio {} from {}", audioId, playerUuid);
                    reaction.accept(null);
                }
            });
        } else {
            VoiceChatRecording.LOGGER.error("Tried to load nonexisting audio {} from {}", audioId, playerUuid);
        }
        reaction.accept(null);
    }

    /**
     * Loads an audio and returns a Future which will return the audio
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @return A future which will return the loaded audio (or null)
     */
    public Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId){
        Path audioPath = RecordedAudio.audiosPath.resolve(playerUuid.toString()).resolve(audioId.toString());
        if(Files.exists(audioPath)){
            VoiceChatRecording.LOGGER.debug("Loading audio {} from {}", audioId, playerUuid);
            return audioLoader.submit(() -> {
                short[] audio = AudioDirectoryReader.getFile(audioPath);
                if(audio != null){
                    RecordedAudio recAudio = new RecordedAudio(audio, playerUuid, audioId);
                    NeoForge.EVENT_BUS.post(new AudioLoadedEvent(recAudio, LoadType.SINGLE));
                    return recAudio;
                } else {
                    VoiceChatRecording.LOGGER.error("Error loading audio {} from {}", audioId, playerUuid);
                    return null;
                }
            });
        } else {
            VoiceChatRecording.LOGGER.error("Tried to load nonexisting audio {} from {}", audioId, playerUuid);
        }
        return audioLoader.submit(() -> null);
    }

    public static void loadAudioToEvent(UUID playerUuid, UUID audioId){
        Path audioPath = RecordedAudio.audiosPath.resolve(playerUuid.toString()).resolve(audioId.toString());
        if(Files.exists(audioPath)){
            VoiceChatRecording.LOGGER.debug("Loading audio {} from {}", audioId, playerUuid);
            audioLoader.execute(() -> {
                short[] audio = AudioDirectoryReader.getFile(audioPath);
                if(audio != null){
                    NeoForge.EVENT_BUS.post(new AudioLoadedEvent(new RecordedAudio(audio, playerUuid, audioId), LoadType.SINGLE));
                } else {
                    VoiceChatRecording.LOGGER.error("Error loading audio {} from {}", audioId, playerUuid);
                }
            });
        } else {
            VoiceChatRecording.LOGGER.error("Tried to load nonexisting audio {} from {}", audioId, playerUuid);
        }
    }

    public static void loadPlayerAudios(UUID playerUuid){
        Path userPath = RecordedAudio.audiosPath.resolve(playerUuid.toString());
        if(Files.exists(userPath)){
            VoiceChatRecording.LOGGER.info("Loading audios for {}", playerUuid);
            new AudioDirectoryReader(userPath, true, (audio, path) -> {
                UUID id = UUID.fromString(FilenameUtils.getBaseName(path.getFileName().toString()));
                VoiceChatRecording.LOGGER.debug("str -> UUID: {} vs {}",FilenameUtils.getBaseName(path.getFileName().toString()), id);
                NeoForge.EVENT_BUS.post(new AudioLoadedEvent(new RecordedAudio(audio, playerUuid, id), LoadType.ALL_FROM_USER));
            },
                    (name) -> {
                        boolean f=name.getFileName().toString().endsWith(".pcm");
                        if(!f){
                            VoiceChatRecording.LOGGER.error("Unknown file in audio folder (unknown extension): {}", name);
                            return false;
                        }
                        try{
                            String n = FilenameUtils.getBaseName(name.getFileName().toString());
                            VoiceChatRecording.LOGGER.debug("File basename: {}", n);
                            UUID id = UUID.fromString(n);
                            return true;
                        } catch(IllegalArgumentException ex){
                            VoiceChatRecording.LOGGER.error("Unknown file in audio folder (not uuid): {}", name);
                            return false;
                        }
                    }).start();
        } else {
            VoiceChatRecording.LOGGER.warn("No audios found for {}", playerUuid);
        }
    }

    private void onPlayerConnected(PlayerConnectedEvent e){
        UUID playerUuid = e.getConnection().getPlayer().getUuid();
        RecordedPlayer player = new RecordedPlayer(playerUuid);
        recordedPlayers.put(playerUuid, player);
        startRecording(playerUuid);
    }

    public static void addCategory(String id, String name, String description, @Nullable int[][] icon){
            categories.add(VoiceChatRecording.vcApi.volumeCategoryBuilder()
                .setId(id)
                .setName(name)
                .setDescription(description)
                .setIcon(icon)
                .build());
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent e){
        UUID pid = e.getPlayerUuid();
        stopRecording(pid);
        if(getPrivacy(pid)){
            privacyMode.remove(pid);
        }
        recordedPlayers.remove(pid);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();

        for(VolumeCategory cat : categories){
            api.registerVolumeCategory(cat);
        }
        categories.clear();

        recordedPlayers = new ConcurrentHashMap<>();
        privacyMode = new ConcurrentHashMap<>();
        VoiceChatRecording.LOGGER.debug("STARTING SCHEDULER");
        VoiceChatRecording.TASKS.schedule(VoiceChatRecordingPlugin::checkForSilence, 20);
    }

    public static void stopRecording(UUID uuid) {
        recordedPlayers.get(uuid).saveCurrentRecording();
        VoiceChatRecording.LOGGER.debug("Stopped recording for player: " + uuid.toString());

    }

    public static void startRecording(UUID uuid) {
        recordedPlayers.get(uuid).startRecording();
        VoiceChatRecording.LOGGER.debug("Recording started for player: " + uuid.toString());
    }

    public static IRecordedPlayer getRecordedPlayer(UUID uuid) {
        return recordedPlayers.get(uuid);
    }

    private static Map<UUID, RecordedPlayer> getRecordedPlayers() {
        return recordedPlayers;
    }

    public static boolean getPrivacy(UUID uuid){
        return privacyMode.getOrDefault(uuid, true);
    }
    public static void setPrivacy(UUID uuid, boolean state){
        privacyMode.put(uuid, state);
    }

    private static void checkForSilence() {
        for (RecordedPlayer player : VoiceChatRecordingPlugin.getRecordedPlayers().values()) {
            if(player.isSpeaking()) continue;
            if (player.isSilent()) continue;
            VoiceChatRecording.LOGGER.debug("Stopped Speaking!");
            VoiceChatRecordingPlugin.stopRecording(player.getUuid());
            player.setSilent(true);
        }
        VoiceChatRecording.TASKS.schedule(VoiceChatRecordingPlugin::checkForSilence, 25);
    }
}
