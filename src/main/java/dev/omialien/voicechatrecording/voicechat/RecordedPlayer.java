package dev.omialien.voicechatrecording.voicechat;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.api.events.AudioRecordedEvent;
import dev.omialien.voicechatrecording.api.IRecordedPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

public class RecordedPlayer implements IRecordedPlayer {
    public static final int RECORDING_SIZE = 1024*1024;
    private OpusDecoder decoder = null;
    private final short[] recording;
    private int currentRecordingIndex; // The total recording size including pauses between words
    private int recordingSize; // The recording size only including until the last active packet
    private boolean isRecording = false;
    private final UUID uuid;
    private long lastSpoke;
    private static final long NOT_SPOKEN_YET = -1;
    private boolean isSilent = false;
    public RecordedPlayer(UUID uuid) {
        this.uuid = uuid;
        this.recording = new short[RECORDING_SIZE];
        this.lastSpoke = NOT_SPOKEN_YET;
        VoiceChatRecording.LOGGER.debug("Created RecordedPlayer {}", uuid);
    }

    public void saveCurrentRecording() {
        if (isRecording){
            short[] savedRecording = new short[recordingSize];
            System.arraycopy(recording, 0, savedRecording, 0, recordingSize);
            RecordedAudio recAudio = new RecordedAudio(savedRecording, this.getUuid());
            NeoForge.EVENT_BUS.post(new AudioRecordedEvent(recAudio));
            currentRecordingIndex = 0;
            recordingSize = 0;
        }
    }

    public void recordPacket(byte[] packet) {
        if (isRecording) {
            if (decoder == null) {
                VoiceChatRecording.LOGGER.warn("Decoder is not initialized!");
                return;
            }
            try {
                short[] decodedPacket = decoder.decode(packet);
                if (decodedPacket.length + currentRecordingIndex < RECORDING_SIZE){
                    boolean active = false;
                    for (short value : decodedPacket) {
                        if (Math.abs(value) >= RecordingCommonConfig.SILENCE_THRESHOLD.get()) {
                            setLastSpoke(System.currentTimeMillis());
                            setSilent(false);
                            active = true;
                            break;
                        }
                    }
                    if(!active && currentRecordingIndex < 5){
                        return;
                    }
                    System.arraycopy(decodedPacket, 0, recording, currentRecordingIndex, decodedPacket.length);
                    currentRecordingIndex += decodedPacket.length;
                    if(active){
                        recordingSize = currentRecordingIndex;
                    }
                } else {
                    VoiceChatRecording.LOGGER.warn("Recording buffer full!");
                    saveCurrentRecording();
                }
            } catch (Exception e) {
                VoiceChatRecording.LOGGER.error("Error decoding packet:", e);
            }
        }
    }

    public void startRecording() {
        if (!isRecording) {
            decoder = VoiceChatRecording.vcApi.createDecoder();
            isRecording = true;
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isSpeaking() {
        return getLastSpoke() != NOT_SPOKEN_YET && System.currentTimeMillis() - getLastSpoke() < 1200;
    }

    public long getLastSpoke() {
        return lastSpoke;
    }

    private void setLastSpoke(long lastSpoke) {
        this.lastSpoke = lastSpoke;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilent(boolean silent) {
        isSilent = silent;
    }
}
