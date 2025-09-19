package dev.omialien.voicechat_recording.voicechat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Pair;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording.taskscheduler.TaskScheduler;
import dev.omialien.voicechat_recording.voicechat.events.AudioLoadedEvent;
import dev.omialien.voicechat_recording.voicechat.events.MicPacketReceivedEvent;
import dev.omialien.voicechat_recording.voicechat.events.RecordingSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@ForgeVoicechatPlugin
public class VoiceChatRecordingPlugin implements VoicechatPlugin {
    private static Map<UUID, RecordedPlayer> recordedPlayers;
    private static Map<UUID, Boolean> privacyMode;
    private static Queue<VolumeCategory> categories;
    private static ExecutorService audioLoader;
    private static final Gson gson = new Gson();
    private static Map<String, Set<Pair<UUID, UUID>>> savedAudios;
    private static Map<String, Set<RecordedAudio>> savedAudiosCache;
    // TODO is this queue really needed? check if the concurrenthashmap can handle both the saving thread removing audios and adding new audios
    private static Queue<Pair<String, RecordedAudio>> audiosToSave;
    public static TaskScheduler audioSavingTask;
    private static boolean audioSavingTaskScheduled = false;
    // TODO audio saving cooldown config
    private static final long audioSaveCooldown = 15 * 20;
    private static Thread audioSavingThread;

    private static void createThread() {
        // TODO maybe instead of this make a separate thread that is always running while in a server
        VoiceChatRecording.LOGGER.debug("Recreating saving thread");
        audioSavingThread = new Thread(() -> {
            VoiceChatRecording.LOGGER.debug("Running saving thread");
            // TODO change config name, or create 2 different configs for reader and saver
            ExecutorService savePool = Executors.newFixedThreadPool(RecordingCommonConfig.AUDIO_READER_THREAD_COUNT.get());
            Path basePath = RecordedAudio.audiosPath;
            long start = System.nanoTime();
            for(String namespace : savedAudios.keySet()) {
                VoiceChatRecording.LOGGER.debug("Saving audios for namespace {}", namespace);
                // Write the JSON file of the namespace
                Set<Pair<UUID, UUID>> audioIds = savedAudios.get(namespace);

                try {
                    Path namespacePath = basePath.resolve(String.format("%s.json", namespace));
                    Files.deleteIfExists(namespacePath);
                    PrintWriter writer = new PrintWriter(namespacePath.toFile());
                    writer.println(gson.toJson(audioIds.stream().map((audio) -> new Pair<>(audio.getFirst(), audio.getSecond())).collect(Collectors.toSet())));
                    writer.close();
                    VoiceChatRecording.LOGGER.debug("Wrote {}.json", namespace);
                } catch (IOException e) {
                    VoiceChatRecording.LOGGER.error("Couldn't save json file for namespace {}!", namespace);
                    VoiceChatRecording.LOGGER.error("{}", e.getMessage());
                }
                // Save the audio files
                if(!savedAudiosCache.containsKey(namespace)){
                    VoiceChatRecording.LOGGER.debug("No new audios to save for {}", namespace);
                    continue;
                }
                Set<RecordedAudio> audios = savedAudiosCache.get(namespace);
                for(RecordedAudio audio : audios) {
                    Path audioPath = basePath.resolve(audio.fileName());
                    if(!Files.exists(audioPath)) {
                        try {
                            Files.createFile(audioPath);
                            savePool.submit(() -> {
                                VoiceChatRecording.LOGGER.debug("saving {}", audioPath);
                                try {
                                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(audioPath.toFile()));
                                    for(short cur : audio.getAudio()) {
                                        dos.writeShort(cur);
                                    }
                                    dos.close();
                                    VoiceChatRecording.LOGGER.debug("Finished writing {} to file", audioPath);
                                } catch (FileNotFoundException e) {
                                    VoiceChatRecording.LOGGER.error("Couldn't find newly created file? {}", audioPath);
                                    VoiceChatRecording.LOGGER.error("{}", e.getMessage());
                                } catch (IOException e) {
                                    VoiceChatRecording.LOGGER.error("Error writing file! {}", audioPath);
                                    VoiceChatRecording.LOGGER.error("{}", e.getMessage());
                                }
                            });
                        } catch (IOException e) {
                            VoiceChatRecording.LOGGER.error("Error creating audio file {} !", audioPath);
                            VoiceChatRecording.LOGGER.error("{}", e.getMessage());
                        }
                    } else {
                        VoiceChatRecording.LOGGER.debug("Audio {} already exists, UUIDs are the same so content must be the same", audioPath);
                    }
                }
            }

            savePool.close();
            long end = System.nanoTime();
            VoiceChatRecording.LOGGER.info("Finished saving audios in {}ms", (double)(end-start)/1000000);
            savedAudiosCache.clear();
            audioSavingTaskScheduled = false;
            while(!audiosToSave.isEmpty()) {
                if(!audioSavingTaskScheduled){
                    audioSavingTaskScheduled = true;
                    audioSavingTask.schedule(VoiceChatRecordingPlugin::saveAudios, audioSaveCooldown);
                }
                Pair<String, RecordedAudio> pair = audiosToSave.remove();
                if(!savedAudiosCache.containsKey(pair.getFirst())) {
                    savedAudiosCache.put(pair.getFirst(), new HashSet<>());
                }
                savedAudiosCache.get(pair.getFirst()).add(pair.getSecond());
            }
        });
        audioSavingThread.setName("Audio Saving Thread");
    }

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return VoiceChatRecording.MOD_ID;
    }

    @ApiStatus.Internal
    public static void shutdownSaving() throws InterruptedException {
        audioSavingTask = new TaskScheduler();
        audioSavingTaskScheduled = false;
        if(audioSavingThread == null || !audioSavingThread.isAlive()){
            // Move all queued audios to the main map and run the thread on the main thread
            while(!audiosToSave.isEmpty()) {
                Pair<String, RecordedAudio> pair = audiosToSave.remove();
                if(!savedAudiosCache.containsKey(pair.getFirst())) {
                    savedAudiosCache.put(pair.getFirst(), new HashSet<>());
                }
                savedAudiosCache.get(pair.getFirst()).add(pair.getSecond());
            }
            createThread();
            audioSavingThread.start();
        }
        // Thread is running, wait for it to finish
        audioSavingThread.join();
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        categories = new LinkedList<>();
        if(api instanceof VoicechatServerApi napi){
            VoiceChatRecording.LOGGER.info("Server Voice Chat API");
            VoiceChatRecording.vcApi = napi;
        } else {
            VoiceChatRecording.LOGGER.info("Client Voice Chat API");
        }
        savedAudiosCache = new HashMap<>();
        savedAudios = new HashMap<>();
        audiosToSave = new ConcurrentLinkedQueue<>();
        audioSavingTask = new TaskScheduler();
        audioSavingTaskScheduled = false;
        audioLoader = Executors.newFixedThreadPool(4);
        createThread();
        if(audioSavingThread != null && audioSavingThread.isAlive()){
            try {
                VoiceChatRecording.LOGGER.debug("joining saving thread");
                audioSavingThread.join();
                VoiceChatRecording.LOGGER.debug("finished join");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        VoiceChatRecording.LOGGER.info("Voice chat recording plugin initialized!");
    }

    private static void saveAudios() {
        if(audioSavingThread.isAlive()) {
            VoiceChatRecording.LOGGER.warn("Tried to save audios while thread was started: trying again in 5 minutes");
            audioSavingTask.schedule(VoiceChatRecordingPlugin::saveAudios, audioSaveCooldown);
        } else {
            createThread();
            audioSavingThread.start();
        }
    }

    public static void saveAudio(String namespace, RecordedAudio audio){
        if(audioSavingThread.isAlive()) {
            // Thread is running, so we shouldn't mess with the hashmap, or we risk blocking here until it finishes saving
            VoiceChatRecording.LOGGER.debug("thread is alive, adding to queue");
            audiosToSave.add(new Pair<>(namespace, audio));
        } else {
            VoiceChatRecording.LOGGER.debug("adding saved audio to map");
            if(!savedAudiosCache.containsKey(namespace)) {
                savedAudiosCache.put(namespace, new HashSet<>());
            }
            savedAudiosCache.get(namespace).add(audio);
            if(!savedAudios.containsKey(namespace)) {
                savedAudios.put(namespace, new HashSet<>());
            }
        }
        if(!audioSavingTaskScheduled){
            audioSavingTaskScheduled = true;
            VoiceChatRecording.LOGGER.info("Audios will be saved in 5 minutes");
            audioSavingTask.schedule(VoiceChatRecordingPlugin::saveAudios, audioSaveCooldown);
        }
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

    private static void loadNamespaceFiles() throws IOException {
        Path basePath = RecordedAudio.audiosPath;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(basePath)) {
            for(Path curNamespace : directoryStream) {
                String filename = curNamespace.getFileName().toString();
                if(filename.endsWith(".json")) {
                    String namespace = filename.substring(0, filename.lastIndexOf('.'));
                    VoiceChatRecording.LOGGER.info("Loading namespace {}", namespace);
                    JsonReader reader = new JsonReader(new FileReader(curNamespace.toFile()));
                    Set<Pair<UUID, UUID>> audioIds = gson.fromJson(reader, new TypeToken<Set<Pair<UUID, UUID>>>(){}.getType());
                    if(!savedAudios.containsKey(namespace)) { savedAudios.put(namespace, new HashSet<>()); }
                    for(Pair<UUID, UUID> id : audioIds){
                        savedAudios.get(namespace).add(id);
                        VoiceChatRecording.LOGGER.debug("{}: {} {}", namespace, id.getFirst(), id.getSecond());
                    }
                }
            }
        }
    }

    public enum LoadType {
        SINGLE,
        ALL_FROM_USER,
        ALL,
        // TODO allow events to identify from which namespace the audio was loaded
        NAMESPACE
    }

    // This should be fine to not reset between worlds, as even in different worlds
    // there shouldn't be 2 audios with the same UUIDs and this is only used to
    // quickly retrieve already-loaded audios, not actually load them
    private static Map<Pair<UUID, UUID>, Future<RecordedAudio>> audioLoadingCache = new ConcurrentHashMap<>();
    // TODO this is currently tied to the game TPS, maybe make this a thread that's always running
    // and just sleep it to act as the cooldown
    // Ending it early because of game shutdown is no problem as loading won't be needed in that case anyway
    public static TaskScheduler audioLoadingCacheRemovalTasks = new TaskScheduler();
    private static Future<RecordedAudio> loadRawAudio(Pair<UUID, UUID> ids, LoadType type) {
        Path audioPath = RecordedAudio.audiosPath.resolve(ids.getFirst().toString() + "+" + ids.getSecond().toString() + ".pcm");
        if(!Files.exists(audioPath)) {
            VoiceChatRecording.LOGGER.error("Tried to load non-existent audio {}", audioPath);
            return null;
        }
        // Check the cache immediately before filling it
        if(audioLoadingCache.containsKey(ids)) {
            return audioLoadingCache.get(ids);
        }
        audioLoadingCache.put(ids, null);
        Future<RecordedAudio> res = audioLoader.submit(() -> {
            File audioFile = audioPath.toFile();
            try (DataInputStream dis = new DataInputStream(new FileInputStream(audioFile))) {
                short[] audio = new short[(int)(audioFile.length() / 2)];
                for(int i = 0; i < audioFile.length() / 2; i++) {
                    audio[i] = dis.readShort();
                }
                RecordedAudio recordedAudio = new RecordedAudio(audio, ids.getFirst(), ids.getSecond());
                NeoForge.EVENT_BUS.post(new AudioLoadedEvent(recordedAudio, type));
                return recordedAudio;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                VoiceChatRecording.LOGGER.error("Error loading audio: {}", audioPath);
                VoiceChatRecording.LOGGER.error("{}", e.getMessage());
            }
            return null;
        });
        audioLoadingCache.put(ids, res);
        return res;
    }

    /**
     * Loads all audios from the given namespace from disk
     * @param namespace the namespace to identify loaded audios
     * @return a list of futures of the loaded audios
     */
    public static List<Future<RecordedAudio>> loadNamespaceAudios(String namespace) {
        if(!savedAudios.containsKey(namespace)) {
            VoiceChatRecording.LOGGER.warn("Tried to load from non-existent namespace {}", namespace);
            return Collections.emptyList();
        }
        Set<Pair<UUID, UUID>> toLoad = savedAudios.get(namespace);
        List<Future<RecordedAudio>> loadedAudios = new ArrayList<>(toLoad.size());
        for(Pair<UUID, UUID> cur : toLoad) {
            loadedAudios.add(loadRawAudio(cur, LoadType.NAMESPACE));
        }
        return loadedAudios;
    }

    /**
     * Gets the identifiers of all the audios saved by a specific namespace
     * This doesn't actually load the audios from disk, use
     * {@link VoiceChatRecordingPlugin#loadNamespaceAudios(String)} or {@link VoiceChatRecordingPlugin#loadAudio(UUID, UUID)}
     * @param namespace the namespace to retrieve audios from
     * @return a set of all the identifiers of the audios from the given namespace
     */
    public static Set<Pair<UUID, UUID>> getNamespaceAudios(String namespace) {
        return Collections.unmodifiableSet(savedAudios.getOrDefault(namespace, Collections.emptySet()));
    }

    /**
     * Loads an audio and returns a Future which will return the audio
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @return A future which will return the loaded audio (or null)
     */
    public Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId){
        // TODO, remember to check and add to cache
        throw new NotImplementedException();
    }

    public static void loadPlayerAudios(UUID playerUuid){
        // TODO, remember to check and add to cache
        throw new NotImplementedException();
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

        try {
            loadNamespaceFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for(VolumeCategory cat : categories){
            VoiceChatRecording.LOGGER.debug("Registering category {}", cat.getName());
            api.registerVolumeCategory(cat);
        }
        categories.clear();

        recordedPlayers = new ConcurrentHashMap<>();
        privacyMode = new ConcurrentHashMap<>();
        VoiceChatRecording.LOGGER.debug("STARTING SCHEDULER");
        VoiceChatRecording.TASKS.schedule(VoiceChatRecordingPlugin::checkForSilence, 20);
        NeoForge.EVENT_BUS.post(new RecordingSetupEvent());
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
