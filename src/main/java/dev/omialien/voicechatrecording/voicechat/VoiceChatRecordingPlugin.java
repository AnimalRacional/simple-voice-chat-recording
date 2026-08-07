package dev.omialien.voicechatrecording.voicechat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import dev.omialien.voicechatrecording.api.AudioId;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.api.IRecordedPlayer;
import dev.omialien.voicechatrecording.api.VoiceChatRecordingApi;
import dev.omialien.voicechatrecording.api.events.AudioLoadedEvent;
import dev.omialien.voicechatrecording.api.events.MicPacketReceivedEvent;
import dev.omialien.voicechatrecording.api.events.RecordingSetupEvent;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@ForgeVoicechatPlugin
public class VoiceChatRecordingPlugin implements VoicechatPlugin, VoiceChatRecordingApi {
    private static final Gson gson = new Gson();
    private Map<UUID, RecordedPlayer> recordedPlayers;
    private Map<UUID, Boolean> privacyMode;
    private ExecutorService audioSaver;
    private ExecutorService audioLoader;
    public Map<String, Set<AudioId>> savedAudios;
    private Cache<AudioId, Future<IRecordedAudio>> audioCache;
    private final Set<String> dirtyNamespaces = Collections.synchronizedSet(new HashSet<>());
    private int ticksToSaveNamespaces;

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return VoiceChatRecording.MOD_ID;
    }

    public void shutdownAudioSaving() throws InterruptedException {
        long start = System.nanoTime();
        VoiceChatRecording.LOGGER.info("Shutting down audio saving");
        saveNamespaceFiles();
        audioSaver.shutdown();
        try {
            if (!audioSaver.awaitTermination(20, TimeUnit.SECONDS)) {
                VoiceChatRecording.LOGGER.error("Shutting down audio saving took too long! Data may be lost");
            }
        } catch (InterruptedException e) {
            VoiceChatRecording.LOGGER.error("Audio saving was unexpectedly interrupted:", e);
        }
        long elapsed = System.nanoTime() - start;
        VoiceChatRecording.LOGGER.info("Shut down audio saving in {}ms", TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS));
    }

    public void shutdownAudioLoading() {
        VoiceChatRecording.LOGGER.info("Shutting down audio loading...");
        int count = audioLoader.shutdownNow().size();
        if (count > 0) {
            VoiceChatRecording.LOGGER.warn("{} audios were not loaded due to shutdown", count);
        }
        VoiceChatRecording.LOGGER.info("Shut down audio loading");
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        VoiceChatRecording.recordingApi = this;
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoiceChatRecording.LOGGER.debug("Initializing Recording API");
        VoicechatServerApi api = event.getVoicechat();
        VoiceChatRecording.vcApi = api;
        VoiceChatRecording.recordingApi = this;
        audioCache = CacheBuilder.newBuilder().expireAfterAccess(RecordingCommonConfig.CACHE_REMOVAL_TIME.get(), TimeUnit.MILLISECONDS).build();
        savedAudios = new ConcurrentHashMap<>();
        audioLoader = Executors.newFixedThreadPool(RecordingCommonConfig.AUDIO_READER_THREAD_COUNT.get());
        audioSaver = Executors.newFixedThreadPool(RecordingCommonConfig.AUDIO_SAVER_THREAD_COUNT.get());
        recordedPlayers = new ConcurrentHashMap<>();
        privacyMode = new ConcurrentHashMap<>();
        dirtyNamespaces.clear();
        ticksToSaveNamespaces = RecordingCommonConfig.NAMESPACE_SAVE_TICKS.get();

        try {
            loadNamespaceFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VoiceChatRecording.LOGGER.debug("STARTING SCHEDULER");
        VoiceChatRecording.TASKS.schedule(this::checkForSilence, 20);
        RecordingSetupEvent recordingSetupEvent = new RecordingSetupEvent(this);
        //TODO verificar se isto está bem, antes usava se o eventResult mas o metodo retorna um boolean em forge
        MinecraftForge.EVENT_BUS.post(recordingSetupEvent);
        Iterator<VolumeCategory> it = recordingSetupEvent.getCategories();
        while(it.hasNext()) {
            VolumeCategory cat = it.next();
            VoiceChatRecording.LOGGER.debug("Registering category {}", cat.getName());
            api.registerVolumeCategory(cat);
        }
    }

    private void writeAudio(RecordedAudio audio, Path basePath) {
        Path path = basePath.resolve(audio.fileName());
        try {
            Path result = Files.createFile(path);
            long start = System.nanoTime();
            try (FileOutputStream fos = new FileOutputStream(result.toFile())) {
                FileChannel out = fos.getChannel();
                short[] audioData = audio.getAudio();
                ByteBuffer buffer = ByteBuffer.allocate(audioData.length * 2);
                buffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(audioData);
                long written = 0;
                int count = 0;
                while(written < audioData.length * 2L) {
                    written += out.write(buffer);
                    count += 1;
                }
                VoiceChatRecording.LOGGER.debug("Took {} writes to write to file", count);
            }
            long elapsed = System.nanoTime() - start;
            VoiceChatRecording.LOGGER.debug("Finished writing {} to file in {}ms", path, TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS));
        } catch (FileAlreadyExistsException e){
            VoiceChatRecording.LOGGER.debug("Saved already-existing audio {} {}", audio.getPlayerUUID(), audio.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveNamespaceFiles() {
        if (dirtyNamespaces.isEmpty()) {
            return;
        }
        Path basePath = RecordedAudio.audiosPath;
        long start = System.nanoTime();
        synchronized (dirtyNamespaces) {
            for (String namespace : dirtyNamespaces) {
                audioSaver.submit(() -> writeNamespaceFile(namespace, basePath));
            }
            dirtyNamespaces.clear();
        }
        long elapsed = System.nanoTime() - start;
        VoiceChatRecording.LOGGER.debug("Finished saving namespace files in {}ms", TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS));
    }

    public void writeNamespaceFile(String namespace, Path basePath) {
        Path path = basePath.resolve(namespace + ".json");
        Set<AudioId> audios = savedAudios.get(namespace);
        try {
            PrintWriter writer = new PrintWriter(path.toFile());
            writer.println(gson.toJson(audios));
            writer.close();
            VoiceChatRecording.LOGGER.debug("Wrote namespace file {}.json with {} audios", namespace, audios.size());
        } catch (IOException e) {
            VoiceChatRecording.LOGGER.error("Couldn't save json file for namespace {}!", namespace, e);
        }
    }

    public void saveAudio(String namespace, RecordedAudio audio){
        VoiceChatRecording.LOGGER.debug("PLUGIN saving audio {} {}", namespace, audio.getId());
        savedAudios.computeIfAbsent(namespace, (k) -> ConcurrentHashMap.newKeySet());
        AudioId ids = new AudioId(audio.getPlayerUUID(), audio.getId());
        Set<AudioId> namespaceAudios = savedAudios.get(namespace);
        boolean updateNamespace = !namespaceAudios.contains(ids);
        if (updateNamespace) {
            ticksToSaveNamespaces = RecordingCommonConfig.NAMESPACE_SAVE_TICKS.get();
            dirtyNamespaces.add(namespace);
            namespaceAudios.add(ids);
        }
        audioCache.put(ids, audioSaver.submit(() -> audio));
        audioSaver.submit(() -> writeAudio(audio, RecordedAudio.audiosPath));
    }

    @Override
    public void unsaveAudio(String namespace, UUID playerUUID, UUID audioUUID) {
        // TODO if this is called before the audio gets written to disk, since it is still in audiosToWriteToDisk it will still be written to disk,
        //  although it'll be deleted right afterwards if no other namespace saves it
        //  We can't just remove it from there since it's possible some other namespace also saved it, so is it worth it dealing with this edge case?
        if(savedAudios.containsKey(namespace)) {
            AudioId id = new AudioId(playerUUID, audioUUID);
            savedAudios.get(namespace).remove(id);
            ticksToSaveNamespaces = RecordingCommonConfig.NAMESPACE_SAVE_TICKS.get();
            dirtyNamespaces.add(namespace);
            boolean stillSaved = savedAudios.values().stream().anyMatch((p) -> p.contains(id));
            if(!stillSaved) {
                try {
                    Files.delete(RecordedAudio.audiosPath.resolve(RecordedAudio.getFileName(playerUUID, audioUUID)));
                } catch (IOException e) {
                    VoiceChatRecording.LOGGER.error("Error deleting audio file for {} {}: {}", playerUUID, audioUUID, e);
                }
            }
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

    private long lastMessage = 0;
    private void onMicrophonePacket(MicrophonePacketEvent e){
        if (e.getSenderConnection() != null){ // If it's a player and not an entity
            RecordedPlayer recordedPlayer = recordedPlayers.get(e.getSenderConnection().getPlayer().getUuid());
            if ( recordedPlayer != null ) {
                recordedPlayer.recordPacket(e.getPacket().getOpusEncodedData());
                MicPacketReceivedEvent ev = new MicPacketReceivedEvent(e);
                MinecraftForge.EVENT_BUS.post(ev);
            } else {
                long curTime = System.currentTimeMillis();
                if ( curTime - lastMessage > 10000 ) {
                    VoiceChatRecording.LOGGER.error("{} sent packet without being recorded!", e.getSenderConnection().getPlayer());
                    lastMessage = curTime;
                }
            }
        }
    }

    private void loadNamespaceFiles() throws IOException {
        long start = System.nanoTime();
        Path basePath = RecordedAudio.audiosPath;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(basePath)) {
            for(Path curNamespace : directoryStream) {
                String filename = curNamespace.getFileName().toString();
                if(filename.endsWith(".json")) {
                    VoiceChatRecording.LOGGER.debug("Checking potential namespace file {}", filename);
                    String namespace = filename.substring(0, filename.lastIndexOf('.'));
                    JsonReader reader = new JsonReader(new FileReader(curNamespace.toFile()));
                    Set<AudioId> audioIds = gson.fromJson(reader, new TypeToken<Set<AudioId>>(){}.getType());
                    VoiceChatRecording.LOGGER.info("Loading namespace {}: {} audios", namespace, audioIds.size());
                    if(!savedAudios.containsKey(namespace)) { savedAudios.put(namespace, ConcurrentHashMap.newKeySet()); }
                    for(AudioId id : audioIds){
                        savedAudios.get(namespace).add(id);
                    }
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        VoiceChatRecording.LOGGER.info("Loaded namespaces in {}ms", TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS));
    }

    private IRecordedAudio readAudioFromFile(Path audioPath, AudioId ids) {
        short[] audio;
        try {
            byte[] byts = Files.readAllBytes(audioPath);
            short[] shrts = new short[byts.length / 2];
            ByteBuffer.wrap(byts).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shrts);
            audio = shrts;
        }  catch (FileNotFoundException e) {
            VoiceChatRecording.LOGGER.error("Tried to load non-existent audio {}", audioPath);
            return null;
        } catch (IOException e) {
            VoiceChatRecording.LOGGER.error("Error loading audio: {}", audioPath, e);
            return null;
        }
        return new RecordedAudio(audio, ids.first(), ids.second());
    }

    @Nullable
    private Future<IRecordedAudio> loadRawAudio(AudioId ids, AudioLoadedEvent.LoadType type, Consumer<IRecordedAudio> reaction, String namespace) {
        try {
            Future<IRecordedAudio> cached = audioCache.get(ids, () -> {
                Path audioPath = RecordedAudio.audiosPath.resolve(RecordedAudio.getFileName(ids.player(), ids.audio()));
                return audioLoader.submit(() -> {
                    IRecordedAudio res = this.readAudioFromFile(audioPath, ids);
                    MinecraftForge.EVENT_BUS.post(new AudioLoadedEvent(res, type, namespace));
                    return res;
                });
            });
            audioLoader.submit(() -> {
                try {
                    reaction.accept(cached.get());
                } catch (InterruptedException | ExecutionException e) {
                    VoiceChatRecording.LOGGER.error("Error loading audio {}, {}", ids.player(), ids.audio(), e);
                }
            });
            return cached;
        } catch (ExecutionException e) {
            VoiceChatRecording.LOGGER.error("Error loading audio {}, {}", ids.player(), ids.audio(), e);
            return null;
        }
    }

    private Future<IRecordedAudio> loadRawAudio(AudioId ids, AudioLoadedEvent.LoadType type, Consumer<IRecordedAudio> reaction){
        return loadRawAudio(ids, type, reaction, "");
    }

    private Future<IRecordedAudio> loadRawAudio(AudioId ids, AudioLoadedEvent.LoadType type, String namespace){
        return loadRawAudio(ids, type, (audio) -> {}, namespace);
    }

    private Future<IRecordedAudio> loadRawAudio(AudioId ids, AudioLoadedEvent.LoadType type) {
        return loadRawAudio(ids, type, "");
    }

    // TODO move all javadoc comments like this from this class and RecordedPlayer to the api interfaces
    /**
     * Loads all audios of the given namespace from disk, passing them to {@param reaction}
     * @param namespace the namespace to identify audios to load
     * @param reaction a consumer to react to the loaded {@link IRecordedAudio}; if an error occurs, the audio will be null
     * @return a set of futures of the loaded audios
     */
    @Override
    public Set<Future<IRecordedAudio>> loadNamespaceAudios(String namespace, Consumer<IRecordedAudio> reaction) {
        if(!savedAudios.containsKey(namespace)) {
            VoiceChatRecording.LOGGER.warn("Tried to load from non-existent namespace {}", namespace);
            return Collections.emptySet();
        }
        Set<AudioId> toLoad = savedAudios.getOrDefault(namespace, Collections.emptySet());
        Set<Future<IRecordedAudio>> loadedAudios = new HashSet<>(toLoad.size());
        for(AudioId cur : toLoad) {
            loadedAudios.add(loadRawAudio(cur, AudioLoadedEvent.LoadType.NAMESPACE, reaction, namespace));
        }
        return loadedAudios;
    }

    /**
     * Loads all audios of the given namespace from disk
     * @param namespace the namespace to identify audios to load
     * @return a set of futures of the loaded audios. If an error occurs, the audio will be null
     * Also see {@link VoiceChatRecordingPlugin#loadNamespaceAudios(String, Consumer)}
     */
    @Override
    public Set<Future<IRecordedAudio>> loadNamespaceAudios(String namespace) {
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
    public Set<AudioId> getNamespaceAudios(String namespace) {
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
    public Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId, Consumer<IRecordedAudio> reaction) {
        return loadRawAudio(AudioId.of(playerUuid, audioId), AudioLoadedEvent.LoadType.SINGLE, reaction);
    }

    /**
     * Loads an audio and returns a Future which will return the audio
     * @param playerUuid the uuid of the player to load the audio for
     * @param audioId the uuid of the audio to load
     * @return a future which will return the loaded audio or null if an error occurs
     */
    @Override
    public Future<IRecordedAudio> loadAudio(UUID playerUuid, UUID audioId){
        return loadAudio(playerUuid, audioId, (audio) -> {});
    }

    /**
     * Loads all available audios of a given player and passes them to the given consumer
     * @param playerUuid The UUID of the player to load audios of
     * @param reaction The reaction to pass the audio to
     * @return a set of futures which will return either the loaded audios or null if an error occurred
     */
    @Override
    public Set<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid, Consumer<IRecordedAudio> reaction) {
        // Assume 10 audios per namespace per player for pre-allocating memory
        Set<Future<IRecordedAudio>> loadedAudios = new HashSet<>(savedAudios.size() * 50);
        savedAudios.values().stream().flatMap(Set::stream).forEach((audio) ->
                loadedAudios.add(loadRawAudio(audio, AudioLoadedEvent.LoadType.ALL_FROM_USER, reaction))
        );
        return loadedAudios;
    }

    /**
     * Loads all available audios of a given player
     * @param playerUuid the UUID of the player to get audios of
     * @return a set of futures which will return either the player's audio or null if an error occured
     */
    @Override
    public Set<Future<IRecordedAudio>> loadPlayerAudios(UUID playerUuid){
        return loadPlayerAudios(playerUuid, (audio) -> {});
    }

    private void onPlayerConnected(PlayerConnectedEvent e){
        UUID playerUuid = e.getConnection().getPlayer().getUuid();
        RecordedPlayer player = new RecordedPlayer(playerUuid);
        recordedPlayers.put(playerUuid, player);
        startRecording(playerUuid);
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
        RecordedPlayer p = recordedPlayers.get(uuid);
        if ( p != null ) {
            p.saveCurrentRecording();
            VoiceChatRecording.LOGGER.debug("Stopped recording for player: {}", uuid.toString());
        } else {
            VoiceChatRecording.LOGGER.warn("Stopped recording for non-recording player {}", uuid.toString());
        }

    }

    public void startRecording(UUID uuid) {
        RecordedPlayer p = recordedPlayers.get(uuid);
        if ( p != null ) {
            p.startRecording();
            VoiceChatRecording.LOGGER.debug("Recording started for player: {}", uuid.toString());
        } else {
            VoiceChatRecording.LOGGER.warn("Tried to start recording non-recording player {}", uuid.toString());
        }
    }

    @Override
    public IRecordedPlayer getRecordedPlayer(UUID uuid) {
        return recordedPlayers.getOrDefault(uuid, null);
    }

    // TODO offline players who haven't joined the game since the
    //  last server restart will always have privacy mode on;
    //  it's a weird edge case, but a mod unsaving and then saving an
    //  audio of one of those players will lose that audio as privacy mode
    //  will prevent it from being saved
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

    public void tick() {
        if (!dirtyNamespaces.isEmpty() && --ticksToSaveNamespaces <= 0) {
            ticksToSaveNamespaces = RecordingCommonConfig.NAMESPACE_SAVE_TICKS.get();
            saveNamespaceFiles();
        }
    }
}
