package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.RecordedAudio;

public class AudioRecordedEvent extends AudioEvent {
    public AudioRecordedEvent(RecordedAudio audio){
        super(audio);
    }
}
