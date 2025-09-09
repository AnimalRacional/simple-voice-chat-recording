package dev.omialien.voicechat_recording.voicechat;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording.voicechat.audio.AudioEffect;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RecordedAudio {
    // TODO find a way to prevent or discourage modifying this
    private static final ExecutorService audioSaver = Executors.newFixedThreadPool(4);
    public final int SAMPLE_RATE = 48000;
    public static Path audiosPath;
    private final short[] audio;
    private final UUID id;
    private final UUID player;
    private boolean saved;
    public RecordedAudio(short[] audio, UUID player){
        this(audio, player, UUID.randomUUID());
    }

    public RecordedAudio(short[] audio, UUID player, UUID id){
        this.audio = audio;
        this.player = player;
        this.id = id;
        this.saved = false;
    }

    @ApiStatus.Internal
    public static boolean shutdown() throws InterruptedException {
        audioSaver.shutdown();
        return audioSaver.awaitTermination(60, TimeUnit.SECONDS);
    }

    /**
     * Returns the underlying audio:
     * do not modify this! It will
     * also be modified for all other mods, instead
     * use {@link Arrays#clone()} if you must modify the audio.
     * @return the underlying audio
     */
    public short[] getAudio(){
        return audio;
    }

    public UUID getPlayerUUID(){
        return this.player;
    }

    public UUID getId(){
        return this.id;
    }

    public boolean wasSaved(){
        return this.saved;
    }

    public void saveAudio(){
        if(!VoiceChatRecordingPlugin.getPrivacy(this.player) && !saved){ // This method should only ever happen once per RecordedPlayer, no more no less
            Path userPath = audiosPath.resolve(this.player.toString());
            audioSaver.execute(() -> {
                try{
                    if(!Files.exists(userPath)){
                        Files.createDirectory(userPath);
                    }
                    Path filePath = userPath.resolve(getId() + ".pcm");
                    Files.deleteIfExists(filePath);
                    Files.createFile(filePath);
                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath.toString()));
                    for (Short cur : audio) {
                        dos.writeShort(cur);
                    }
                    dos.close();
                    saved = true;
                    VoiceChatRecording.LOGGER.info("Wrote recording to file {}", filePath);
                } catch(IOException e){
                    VoiceChatRecording.LOGGER.error("Error saving audios for {}:\r\n{}\r\n{}", getPlayerUUID(), e.getMessage(), e.getStackTrace());
                }
            });
        } else if(saved){
            VoiceChatRecording.LOGGER.warn("Tried to save already-saved audio! {} by {}", getId(), getPlayerUUID());
        }
    }

    /**
     * Gets the underlying audio with the applied effects
     * @param effect the effects applied to the audio
     * @return the unchanged audio if effect is null, a copy of the audio with the effects applied otherwise
     */
    public short[] applyEffects(@Nullable AudioEffect effect){
        return effect == null ? getAudio() : effect.applyEffects(getAudio().clone());
    }

    public enum FilterResult {
        PASSED,
        TOO_LONG,
        TOO_SHORT,
        NO_ACTIVE_AUDIO,
        LOW_RMS
    }

    public FilterResult getFilterResult(){
        final double MIN_DURATION = 0.9;
        final double MAX_DURATION = 10;
        final double MIN_RMS = 500;      // loudness threshold
        double duration = getDuration();
        if(duration <= MIN_DURATION){
            return FilterResult.TOO_SHORT;
        }
        if(duration > MAX_DURATION){
            return FilterResult.TOO_LONG;
        }
        if(getActiveSamples() <= 0){
            return FilterResult.NO_ACTIVE_AUDIO;
        }
        return getRms() >= MIN_RMS ? FilterResult.PASSED : FilterResult.LOW_RMS;
    }

    public double getDuration(){
        return (double) audio.length / SAMPLE_RATE;
    }

    public double getActiveDuration(){
        return (double) getActiveSamples() / SAMPLE_RATE;
    }

    public String getAudioInfo(){
        return (String.format(
                "Audio duration: %.3fs, Active region: %.3fs, RMS: %.1f",
                getDuration(),
                (double) getActiveSamples() / SAMPLE_RATE,
                getRms()
        ));
    }

    private int activeSamplesCache;
    private double rmsCache;
    private boolean calculatedCaches = false;
    private void calculateCaches(){
        calculatedCaches = true;
        int start = 0;
        while (start < audio.length &&
                Math.abs(audio[start]) < RecordingCommonConfig.SILENCE_THRESHOLD.get()) {
            start++;
        }

        int end = audio.length - 1;
        while (end > start &&
                Math.abs(audio[end]) < RecordingCommonConfig.SILENCE_THRESHOLD.get()) {
            end--;
        }

        int activeSamples = end - start + 1;

        // RMS on trimmed region
        long sumSquares = 0;
        for (int i = start; i <= end; i++) {
            int sample = audio[i];
            sumSquares += sample * sample;
        }
        this.rmsCache = Math.sqrt(sumSquares / (double) activeSamples);
        this.activeSamplesCache = activeSamples;
    }

    public int getActiveSamples(){
        if(!calculatedCaches){ calculateCaches(); }
        return this.activeSamplesCache;
    }

    public double getRms(){
        if(!calculatedCaches){ calculateCaches(); }
        return this.rmsCache;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RecordedAudio other){
            return other.getId().equals(this.getId()) && other.getPlayerUUID().equals(this.getPlayerUUID());
        } else { return super.equals(obj); }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getPlayerUUID());
    }
}
