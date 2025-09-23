package dev.omialien.voicechatrecording.voicechat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Pair;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.taskscheduler.TaskScheduler;
import dev.omialien.voicechatrecording.voicechat.audio.AudioCache;
import dev.omialien.voicechatrecording_api.IRecordedPlayer;
import dev.omialien.voicechatrecording_api.VoiceChatRecordingApi;
import dev.omialien.voicechatrecording_api.events.AudioLoadedEvent;
import dev.omialien.voicechatrecording_api.events.MicPacketReceivedEvent;
import dev.omialien.voicechatrecording_api.events.RecordingSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO check if leaving and rejoining world or servers messes up things since they stopped being static
@ForgeVoicechatPlugin
public class VoiceChatRecordingPlugin implements VoicechatPlugin, VoiceChatRecordingApi {
    private static final Gson gson = new Gson();
    private Map<UUID, RecordedPlayer> recordedPlayers;
    private Map<UUID, Boolean> privacyMode;
    private ExecutorService audioLoader;
    private Map<String, Set<Pair<UUID, UUID>>> savedAudios;
    private Map<String, Set<RecordedAudio>> savedAudiosCache;
    // TODO is this queue really needed? check if the concurrenthashmap can handle both the saving thread removing audios, and mods adding new audios
    private Queue<Pair<String, RecordedAudio>> audiosToSave;
    public TaskScheduler audioSavingTask;
    private boolean audioSavingTaskScheduled = false;
    private Thread audioSavingThread;
    private AudioCache audioCache;

    // TODO delete audio files that aren't saved by any namespace
    private void createAudioSavingThread() {
        // TODO maybe instead of creating a thread every time make a separate thread that is always running while in a server
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
                    Set<Pair<UUID, UUID>> audios = audioIds.stream().map((audio) -> new Pair<>(audio.getFirst(), audio.getSecond())).collect(Collectors.toSet());
                    writer.println(gson.toJson(audios));
                    writer.close();
                    VoiceChatRecording.LOGGER.debug("Wrote namespace file {}.json with {} audios", namespace, audios.size());
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
                    audioSavingTask.schedule(this::saveAudios, RecordingCommonConfig.AUDIO_SAVING_COOLDOWN.get());
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

    public void shutdownSaving() throws InterruptedException {
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
            createAudioSavingThread();
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
        VoiceChatRecording.recordingApi = this;
        if(audioCache != null) audioCache.interruptThread();
        audioCache = new AudioCache();
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
        createAudioSavingThread();
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

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();

        try {
            loadNamespaceFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        recordedPlayers = new ConcurrentHashMap<>();
        privacyMode = new ConcurrentHashMap<>();
        VoiceChatRecording.LOGGER.debug("STARTING SCHEDULER");
        VoiceChatRecording.TASKS.schedule(this::checkForSilence, 20);
        RecordingSetupEvent eventResult = NeoForge.EVENT_BUS.post(new RecordingSetupEvent(this));
        Iterator<VolumeCategory> it = eventResult.getCategories();
        while(it.hasNext()) {
            VolumeCategory cat = it.next();
            VoiceChatRecording.LOGGER.debug("Registering category {}", cat.getName());
            api.registerVolumeCategory(cat);
        }
    }

    private void saveAudios() {
        if(audioSavingThread.isAlive()) {
            VoiceChatRecording.LOGGER.warn("Tried to save audios while thread was started: trying again in 5 minutes");
            audioSavingTask.schedule(this::saveAudios, RecordingCommonConfig.AUDIO_SAVING_COOLDOWN.get());
        } else {
            createAudioSavingThread();
            audioSavingThread.start();
        }
    }

    public void saveAudio(String namespace, RecordedAudio audio){
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
            audioSavingTask.schedule(this::saveAudios, RecordingCommonConfig.AUDIO_SAVING_COOLDOWN.get());
        }
    }

    public void unsaveAudio(String namespace, RecordedAudio audio) {
        if(savedAudios.containsKey(namespace)) {
            savedAudios.get(namespace).remove(new Pair<>(audio.getPlayerUUID(), audio.getId()));
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

    private void loadNamespaceFiles() throws IOException {
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
        // TODO allow events to identify from which namespace the audio was loaded
        NAMESPACE
    }

    private RecordedAudio readAudioFromFile(Path audioPath, Consumer<RecordedAudio> reaction, Pair<UUID, UUID> ids, LoadType type) {
        File audioFile = audioPath.toFile();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(audioFile))) {
            short[] audio = new short[(int)(audioFile.length() / 2)];
            for(int i = 0; i < audioFile.length() / 2; i++) {
                audio[i] = dis.readShort();
            }
            RecordedAudio recordedAudio = new RecordedAudio(audio, ids.getFirst(), ids.getSecond());
            NeoForge.EVENT_BUS.post(new AudioLoadedEvent(recordedAudio, type));
            reaction.accept(recordedAudio);
            return recordedAudio;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            VoiceChatRecording.LOGGER.error("Error loading audio: {}", audioPath);
            VoiceChatRecording.LOGGER.error("{}", e.getMessage());
            reaction.accept(null);
        }
        return null;
    }

    @Nullable
    private Future<RecordedAudio> loadRawAudio(Pair<UUID, UUID> ids, LoadType type, Consumer<RecordedAudio> reaction) {
        Path audioPath = RecordedAudio.audiosPath.resolve(RecordedAudio.getFileName(ids.getFirst(), ids.getSecond()));
        if(!Files.exists(audioPath)) {
            VoiceChatRecording.LOGGER.error("Tried to load non-existent audio {}", audioPath);
            return null;
        }
        // Check the cache immediately before filling it
        VoiceChatRecording.LOGGER.debug("Checking cache...");
        if(audioCache.isCached(ids)) {
            return audioCache.get(ids);
        }
        audioCache.add(ids, audioLoader.submit(() -> this.readAudioFromFile(audioPath, reaction, ids, type)));
        VoiceChatRecording.LOGGER.debug("Not in cache, added");
        return audioCache.get(ids);
    }

    private Future<RecordedAudio> loadRawAudio(Pair<UUID, UUID> ids, LoadType type){
        return loadRawAudio(ids, type, (audio) -> {});
    }

    /**
     * Loads all audios of the given namespace from disk, passing them to {@param reaction}
     * @param namespace the namespace to identify audios to load
     * @param reaction a consumer to react to the loaded {@link RecordedAudio}; if an error occurs, the audio will be null
     * @return a list of futures of the loaded audios
     */
    @Override
    public List<Future<RecordedAudio>> loadNamespaceAudios(String namespace, Consumer<RecordedAudio> reaction) {
        if(!savedAudios.containsKey(namespace)) {
            VoiceChatRecording.LOGGER.warn("Tried to load from non-existent namespace {}", namespace);
            return Collections.emptyList();
        }
        Set<Pair<UUID, UUID>> toLoad = savedAudios.get(namespace);
        List<Future<RecordedAudio>> loadedAudios = new ArrayList<>(toLoad.size());
        for(Pair<UUID, UUID> cur : toLoad) {
            loadedAudios.add(loadRawAudio(cur, LoadType.NAMESPACE, reaction));
        }
        return loadedAudios;
    }

    /**
     * Loads all audios of the given namespace from disk
     * @param namespace the namespace to identify audios to load
     * @return a list of futures of the loaded audios. If an error occurs, the audio will be null
     * Also see {@link VoiceChatRecordingPlugin#loadNamespaceAudios(String, Consumer)}
     */
    @Override
    public List<Future<RecordedAudio>> loadNamespaceAudios(String namespace) {
        return loadNamespaceAudios(namespace, (audio) -> {});
    }

    /**
     * Gets the identifiers of all the audios saved by a specific namespace
     * This doesn't actually load the audios from disk, use
     * {@link VoiceChatRecordingPlugin#loadNamespaceAudios(String, Consumer)} or {@link VoiceChatRecordingPlugin#loadAudio(UUID, UUID)} for that
     * @param namespace the namespace to retrieve audios from
     * @return a set of all the identifiers of the audios saved to the given namespace
     */
    @Override
    public Set<Pair<UUID, UUID>> getNamespaceAudios(String namespace) {
        return Collections.unmodifiableSet(savedAudios.getOrDefault(namespace, Collections.emptySet()));
    }

    /**
     * Loads an audio and returns a {@link Future} which will return the audio, also passing the loaded audio to {@param reaction}
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @param reaction a consumer that will receive the loaded audio
     * @return a future which will return the loaded audio or null if an error occurs
     */
    @Override
    public Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId, Consumer<RecordedAudio> reaction) {
        // TODO, remember to check and add to cache
        return loadRawAudio(new Pair<>(playerUuid, audioId), LoadType.SINGLE, reaction);
    }

    /**
     * Loads an audio and returns a Future which will return the audio
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @return a future which will return the loaded audio or null if an error occurs
     */
    @Override
    public Future<RecordedAudio> loadAudio(UUID playerUuid, UUID audioId){
        return loadAudio(playerUuid, audioId, (audio) -> {});
    }

    /**
     * Loads all available audios of a given player and passes them to the given consumer
     * @param playerUuid The UUID of the player to load audios of
     * @param reaction The reaction to pass the audio to
     * @return a list of futures which will return either the loaded audios or null if an error occurred
     */
    @Override
    public List<Future<RecordedAudio>> loadPlayerAudios(UUID playerUuid, Consumer<RecordedAudio> reaction) {
        // Assume 50 audios per namespace for pre-allocating memory
        List<Future<RecordedAudio>> loadedAudios = new ArrayList<>(savedAudios.keySet().size() * 50);
        for(Set<Pair<UUID, UUID>> audios : savedAudios.values()) {
            for(Pair<UUID, UUID> audio : audios) {
                if(audio.getFirst().equals(playerUuid)) {
                    loadedAudios.add(loadRawAudio(audio, LoadType.ALL_FROM_USER, reaction));
                }
            }
        }
        return loadedAudios;
    }

    /**
     * Loads all available audios of a given player
     * @param playerUuid the UUID of the player to get audios of
     * @return a list of futures which will return either the player's audio or null if an error occured
     */
    @Override
    public List<Future<RecordedAudio>> loadPlayerAudios(UUID playerUuid){
        return loadPlayerAudios(playerUuid, (audio) -> {});
    }

    private void onPlayerConnected(PlayerConnectedEvent e){
        UUID playerUuid = e.getConnection().getPlayer().getUuid();
        RecordedPlayer player = new RecordedPlayer(playerUuid);
        recordedPlayers.put(playerUuid, player);
        startRecording(playerUuid);
        // TODO remove this, it's for debug
        loadPlayerAudios(e.getConnection().getPlayer().getUuid(), (audio) -> {
            VoiceChatRecording.LOGGER.debug("REACTION:{} {}", audio.getId(), audio.getDuration());
        });
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent e){
        UUID pid = e.getPlayerUuid();
        stopRecording(pid);
        if(getPrivacy(pid)){
            privacyMode.remove(pid);
        }
        recordedPlayers.remove(pid);
    }

    public void stopRecording(UUID uuid) {
        recordedPlayers.get(uuid).saveCurrentRecording();
        VoiceChatRecording.LOGGER.debug("Stopped recording for player: " + uuid.toString());

    }

    public void startRecording(UUID uuid) {
        recordedPlayers.get(uuid).startRecording();
        VoiceChatRecording.LOGGER.debug("Recording started for player: " + uuid.toString());
    }

    @Override
    public IRecordedPlayer getRecordedPlayer(UUID uuid) {
        return recordedPlayers.get(uuid);
    }

    @Override
    public boolean getPrivacy(UUID uuid){
        return privacyMode.getOrDefault(uuid, true);
    }
    public void setPrivacy(UUID uuid, boolean state){
        privacyMode.put(uuid, state);
    }

    private void checkForSilence() {
        for (RecordedPlayer player : recordedPlayers.values()) {
            if(player.isSpeaking()) continue;
            if (player.isSilent()) continue;
            VoiceChatRecording.LOGGER.debug("Stopped Speaking!");
            this.stopRecording(player.getUuid());
            player.setSilent(true);
        }
        VoiceChatRecording.TASKS.schedule(this::checkForSilence, 25);
    }
}
