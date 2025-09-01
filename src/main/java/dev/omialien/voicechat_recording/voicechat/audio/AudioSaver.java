package dev.omialien.voicechat_recording.voicechat.audio;

import dev.omialien.voicechat_recording.VoiceChatRecording;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AudioSaver extends Thread{
    private final Path path;
    private final int index;
    private final List<Short> recording;

    public AudioSaver(Path path, int index, List<Short> recording) {
        this.path = path;
        this.index = index;
        this.recording = recording;
    }
    @Override
    public void run() {

    }
}
