package dev.omialien.voicechat_recording.voicechat;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording.voicechat.audio.AudioEffect;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public class RecordedAudio {
    // TODO find a way to prevent or discourage modifying this
    public static Path audiosPath;
    private final short[] audio;
    private final UUID id;
    private final UUID player;
    private final FilterResult filtered;
    private boolean saved;
    public RecordedAudio(short[] audio, UUID player){
        this(audio, player, UUID.randomUUID());
    }

    public RecordedAudio(short[] audio, UUID player, UUID id){
        this.audio = audio;
        this.player = player;
        this.id = id;
        this.saved = false;
        this.filtered = filterAudio();
    }

    public FilterResult getFilterResult(){
        return this.filtered;
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

    public void saveAudio(){
        if(!VoiceChatRecordingPlugin.getPrivacy(this.player) && !saved){ // This method should only ever happen once per RecordedPlayer, no more no less
            saved = true;
            Path userPath = audiosPath.resolve(this.player.toString());
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
                VoiceChatRecording.LOGGER.info("Wrote recording to file {}", filePath);
            } catch(IOException e){
                VoiceChatRecording.LOGGER.error("Error saving audios for {}:\r\n{}\r\n{}", getPlayerUUID(), e.getMessage(), e.getStackTrace());
            }
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
        BELOW_THRESHOLD,
        LOW_RMS
    }

    private FilterResult filterAudio() {
        final int SAMPLE_RATE = 48000;
        final double MIN_DURATION = 0.9;
        final double MAX_DURATION = 10;
        final double MIN_RMS = 500;      // loudness threshold

        double durationSeconds = (double) audio.length / SAMPLE_RATE;
        if (durationSeconds <= MIN_DURATION) {
            VoiceChatRecording.LOGGER.debug("Audio too short: " + durationSeconds + "s");
            return FilterResult.TOO_SHORT;
        }
        if (durationSeconds > MAX_DURATION) {
            VoiceChatRecording.LOGGER.debug("Audio too long: " + durationSeconds + "s");
            return FilterResult.TOO_LONG;
        }

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
        if (activeSamples <= 0) {
            VoiceChatRecording.LOGGER.debug("No active audio found above silence threshold");
            return FilterResult.BELOW_THRESHOLD;
        }

        // RMS on trimmed region
        long sumSquares = 0;
        for (int i = start; i <= end; i++) {
            int sample = audio[i];
            sumSquares += sample * sample;
        }
        double rms = Math.sqrt(sumSquares / (double) activeSamples);

        VoiceChatRecording.LOGGER.debug(String.format(
                "Audio duration: %.3fs, Active region: %.3fs, RMS: %.1f",
                durationSeconds,
                (double) activeSamples / SAMPLE_RATE,
                rms
        ));

        return rms >= MIN_RMS ? FilterResult.PASSED : FilterResult.LOW_RMS;
    }
}
