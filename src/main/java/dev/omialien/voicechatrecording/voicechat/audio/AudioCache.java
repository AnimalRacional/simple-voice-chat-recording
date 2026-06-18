package dev.omialien.voicechatrecording.voicechat.audio;

import com.mojang.datafixers.util.Pair;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.api.IRecordedAudio;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class AudioCache {
    static class CacheEntry {
        private final Future<IRecordedAudio> audio;
        private long addedTime;
        public CacheEntry(Future<IRecordedAudio> audio, long addedTime) {
            this.audio = audio;
            this.addedTime = addedTime;
        }
        public void refresh() {
            this.addedTime = System.nanoTime();
        }

        public long getTime() { return this.addedTime; }
        public Future<IRecordedAudio> getAudio() { return this.audio; }
    }
    // This should be fine to not reset between worlds, as even in different worlds
    // there shouldn't be 2 audios with the same UUIDs and this is only used to
    // quickly retrieve already-loaded audios, not actually load them
    private final Map<Pair<UUID, UUID>, CacheEntry> audioLoadingCache;
    private Thread removalThread;
    public AudioCache() {
        audioLoadingCache = new ConcurrentHashMap<>();
        restartThread();
    }

    public void interruptThread() {
        if(this.removalThread.isAlive()) {
            this.removalThread.interrupt();
        } else {
            VoiceChatRecording.LOGGER.error("Tried to interrupt cache removal thread while not alive!");
            VoiceChatRecording.LOGGER.error("interruption stacktrace: {}", (Object[]) Thread.currentThread().getStackTrace());
        }
    }

    private void restartThread() {
        if(removalThread != null && removalThread.isAlive()) return;
        removalThread = new Thread(() -> {
            while(true) {
                long time = System.nanoTime();
                // TODO maybe make this like the task scheduler works, keeping each entry sorted by time and only going through the ones that are to be removed
                Set<Pair<UUID, UUID>> toRemove = new HashSet<>();
                int audiosRemoved = 0;
                for(Pair<UUID, UUID> curKey : audioLoadingCache.keySet()) {
                    CacheEntry cur = audioLoadingCache.get(curKey);
                    if(time - cur.getTime() >= ((long)RecordingCommonConfig.CACHE_REMOVAL_TIME.get()*1000000)) {
                        VoiceChatRecording.LOGGER.debug("Removing {}", curKey.getSecond());
                        audiosRemoved++;
                        toRemove.add(curKey);
                    }
                }
                toRemove.forEach(audioLoadingCache::remove);
                if ( audiosRemoved > 0 ) {
                    VoiceChatRecording.LOGGER.info("Finished removing {} old audios from cache", audiosRemoved);
                }
                try {
                    Thread.sleep(Duration.of(RecordingCommonConfig.CACHE_CHECK_INTERVAL.get(), ChronoUnit.SECONDS).toMillis());
                } catch (InterruptedException e) {
                    VoiceChatRecording.LOGGER.warn("Interrupted cache removal thread, finishing");
                    return;
                }
            }
        });
        removalThread.setName("AudioCacheRemovalThread");
        removalThread.start();
    }

    public Optional<Future<IRecordedAudio>> get(Pair<UUID, UUID> ids) {
        if(audioLoadingCache.containsKey(ids)) {
            CacheEntry entry = audioLoadingCache.get(ids);
            entry.refresh();
            return Optional.of(entry.getAudio());
        }
        return Optional.empty();
    }

    public boolean isCached(Pair<UUID, UUID> ids) { return audioLoadingCache.containsKey(ids); }

    public void add(Pair<UUID, UUID> ids, Future<IRecordedAudio> audio) {
        if(audioLoadingCache.containsKey(ids)) {
            // Refresh the audio time because it's still in use
            audioLoadingCache.get(ids).refresh();
            return;
        }
        audioLoadingCache.put(ids, new CacheEntry(audio, System.nanoTime()));
    }
}
