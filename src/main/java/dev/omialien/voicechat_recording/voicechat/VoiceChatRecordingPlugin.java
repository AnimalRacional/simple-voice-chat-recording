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
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class VoiceChatRecordingPlugin implements VoicechatPlugin {
    private static Map<UUID, RecordedPlayer> recordedPlayers;
    private static Map<UUID, Boolean> privacyMode;
    private static Queue<VolumeCategory> categories;

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

    private void loadPlayerAudios(UUID playerUuid){
        Path userPath = RecordedAudio.audiosPath.resolve(playerUuid.toString());
        if(Files.exists(userPath)){
            VoiceChatRecording.LOGGER.info("Loading audios for {}", playerUuid);
            new AudioDirectoryReader(userPath, true, (audio, path) -> {
                UUID id = UUID.fromString(FilenameUtils.getBaseName(path.getFileName().toString()));
                VoiceChatRecording.LOGGER.debug("str -> UUID: {} vs {}",FilenameUtils.getBaseName(path.getFileName().toString()), id);
                NeoForge.EVENT_BUS.post(new AudioLoadedEvent(new RecordedAudio(audio, playerUuid, id)));
            },
                    (name) -> {
                        boolean f=name.getFileName().toString().endsWith(".pcm");
                        if(!f){
                            VoiceChatRecording.LOGGER.error("Unknown file in audio folder ext: {}", name);
                            return false;
                        }
                        try{
                            String n = FilenameUtils.getBaseName(name.getFileName().toString());
                            VoiceChatRecording.LOGGER.debug("File basename: {}", n);
                            UUID id = UUID.fromString(n);
                            return true;
                        } catch(IllegalArgumentException ex){
                            VoiceChatRecording.LOGGER.error("Unkown file in audio folder uuid: {}", name);
                            return false;
                        }
                    }).start();
        } else {
            VoiceChatRecording.LOGGER.debug("No audios found for {}", playerUuid);
        }
    }

    private void onPlayerConnected(PlayerConnectedEvent e){
        UUID playerUuid = e.getConnection().getPlayer().getUuid();
        RecordedPlayer player = new RecordedPlayer(playerUuid);
        recordedPlayers.put(playerUuid, player);
        loadPlayerAudios(playerUuid);
        startRecording(playerUuid);
    }

    public static void addCategory(String id, String name, String description, @Nullable int[][] icon, VoicechatServerApi api){
            categories.add(api.volumeCategoryBuilder()
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
