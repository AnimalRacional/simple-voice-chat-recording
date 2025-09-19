package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.IRecordedAudio;

public class AudioRecordedEvent extends AudioEvent {
    public AudioRecordedEvent(IRecordedAudio audio){
        super(audio);
    }
}
