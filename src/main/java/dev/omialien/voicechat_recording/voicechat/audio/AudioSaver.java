package dev.omialien.voicechat_recording.voicechat.audio;

import dev.omialien.voicechat_recording.VoiceChatRecording;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioSaver extends Thread{
    private final Path path;
    private final int index;
    private final short[] recording;

    public AudioSaver(Path path, int index, short[] recording) {
        this.path = path;
        this.index = index;
        this.recording = recording;
    }
    @Override
    public void run() {
        try{
            Files.deleteIfExists(path);
            Files.createFile(path);

            DataOutputStream dos = new DataOutputStream(new FileOutputStream(path.toString()));
            for (int i=0; i< index; i++) {
                dos.writeShort(recording[i]);
            }
            dos.close();

            VoiceChatRecording.LOGGER.debug("Wrote recording to file {}", path);
        } catch (IOException e){
            VoiceChatRecording.LOGGER.error(e.getMessage());
        }
    }
}
