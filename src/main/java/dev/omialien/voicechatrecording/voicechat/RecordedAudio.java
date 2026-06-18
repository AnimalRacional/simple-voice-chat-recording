package dev.omialien.voicechatrecording.voicechat;

import com.mojang.datafixers.util.Pair;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.api.AudioEffect;
import dev.omialien.voicechatrecording.api.IRecordedAudio;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class RecordedAudio implements IRecordedAudio {
    public final int SAMPLE_RATE = 48000;
    public static Path audiosPath;
    private final short[] audio;
    private final UUID id;
    private final UUID player;
    public RecordedAudio(short[] audio, UUID player){
        this(audio, player, UUID.randomUUID());
    }

    public RecordedAudio(short[] audio, UUID player, UUID id){
        this.audio = audio;
        this.player = player;
        this.id = id;
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

    @Override
    public boolean saveAudio(String namespace){
        boolean privacy = VoiceChatRecording.recordingApi.getPrivacy(this.player);
        if(!privacy){
            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).saveAudio(namespace, this);
            return true;
        }
        return false;
    }

    @Override
    public void unsaveAudio(String namespace) {
        VoiceChatRecording.recordingApi.unsaveAudio(namespace, this.player, this.id);
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

    @Nullable
    public static Pair<UUID, UUID> getIdFromFile(Path path) {
        String name = path.getFileName().toString();
        if(!name.endsWith(".pcm")) { return null; }
        if(name.length() != 77) return null;
        String[] split = name.split(Pattern.quote("+"));
        UUID player;
        try{
            player = UUID.fromString(split[0]);
            VoiceChatRecording.LOGGER.debug("player id: {}", player);
        } catch(IllegalArgumentException e){
            return null;
        }
        UUID audio;
        try {
            audio = UUID.fromString(split[1].split(Pattern.quote(".pcm"))[0]);
            VoiceChatRecording.LOGGER.debug("audio: {}", audio);
        } catch(IllegalArgumentException e){
            return null;
        }
        return new Pair<>(player, audio);
    }

    // Makes a RecordedAudio that can be used to find a recorded audio with the specified ID
    public static RecordedAudio makeIdentificationAudio(UUID player, UUID audioId) {
        return new RecordedAudio(null, player, audioId);
    }
}
