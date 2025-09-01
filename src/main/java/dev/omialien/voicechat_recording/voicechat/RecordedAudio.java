package dev.omialien.voicechat_recording.voicechat;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording.voicechat.audio.AudioSaver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RecordedAudio {
    // TODO find a way to prevent or discourage modifying this
    public static Path audiosPath;
    private final List<Short> audio;
    private final UUID id;
    private final UUID player;
    private boolean filtered;
    private boolean saved;
    public RecordedAudio(short[] audio, UUID player){
        this(audio, player, UUID.randomUUID());
    }

    public RecordedAudio(short[] audio, UUID player, UUID id){
        this.audio = IntStream.range(0, audio.length)
                .mapToObj(s -> audio[s])
                .collect(Collectors.toList());
        this.player = player;
        this.id = id;
        this.saved = false;
        this.filtered = filterAudio();
    }

    public boolean isFiltered(){
        return this.filtered;
    }

    public List<Short> getAudio(){
        return this.audio;
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

    private boolean filterAudio() {
        final int SAMPLE_RATE = 48000;
        final double MIN_DURATION = 0.9;
        final double MAX_DURATION = 10;
        final double MIN_RMS = 500;      // loudness threshold

        double durationSeconds = (double) audio.size() / SAMPLE_RATE;
        if (durationSeconds <= MIN_DURATION) {
            VoiceChatRecording.LOGGER.debug("Audio too short: " + durationSeconds + "s");
            return false;
        }
        if (durationSeconds > MAX_DURATION) {
            VoiceChatRecording.LOGGER.debug("Audio too long: " + durationSeconds + "s");
            return false;
        }

        int start = 0;
        while (start < audio.size() &&
                Math.abs(audio.get(start)) < RecordingCommonConfig.SILENCE_THRESHOLD.get()) {
            start++;
        }

        int end = audio.size() - 1;
        while (end > start &&
                Math.abs(audio.get(end)) < RecordingCommonConfig.SILENCE_THRESHOLD.get()) {
            end--;
        }

        int activeSamples = end - start + 1;
        if (activeSamples <= 0) {
            VoiceChatRecording.LOGGER.debug("No active audio found above silence threshold");
            return false;
        }

        // RMS on trimmed region
        long sumSquares = 0;
        for (int i = start; i <= end; i++) {
            int sample = audio.get(i);
            sumSquares += sample * sample;
        }
        double rms = Math.sqrt(sumSquares / (double) activeSamples);

        VoiceChatRecording.LOGGER.debug(String.format(
                "Audio duration: %.3fs, Active region: %.3fs, RMS: %.1f",
                durationSeconds,
                (double) activeSamples / SAMPLE_RATE,
                rms
        ));

        return rms >= MIN_RMS;
    }
}
