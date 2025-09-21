package dev.omialien.voicechat_recording.voicechat;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording_api.AudioEffect;
import dev.omialien.voicechat_recording_api.IRecordedAudio;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class RecordedAudio implements IRecordedAudio {
    // TODO find a way to prevent or discourage modifying this
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

    @Override
    public short[] getAudio(){
        return audio;
    }

    @Override
    public UUID getPlayerUUID(){
        return this.player;
    }

    @Override
    public UUID getId(){
        return this.id;
    }

    public boolean wasSaved(){
        return this.saved;
    }

    @Override
    public void saveAudio(String namespace){
        if(!VoiceChatRecording.recordingApi.getPrivacy(this.player) && !saved){ // This method should only ever happen once per RecordedPlayer, no more no less
            VoiceChatRecording.recordingApi.saveAudio(namespace, this);
            this.saved = true;
        } else if(saved){
            VoiceChatRecording.LOGGER.warn("Tried to save already-saved audio! {} by {}", getId(), getPlayerUUID());
        }
    }

    @Override
    public short[] applyEffects(@Nullable AudioEffect effect){
        return effect == null ? getAudio() : effect.applyEffects(getAudio().clone());
    }

    @Override
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

    @Override
    public double getDuration(){
        return (double) audio.length / SAMPLE_RATE;
    }

    @Override
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

    @Override
    public int getActiveSamples(){
        if(!calculatedCaches){ calculateCaches(); }
        return this.activeSamplesCache;
    }

    @Override
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

    public String fileName() {
        return RecordedAudio.getFileName(this.getPlayerUUID(), this.getId());
    }

    public static String getFileName(UUID playerUuid, UUID audioId) {
        return String.format("%s+%s.pcm", playerUuid.toString(), audioId.toString());
    }
}
