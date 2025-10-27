package dev.omialien.voicechatrecording.api.events;

import dev.omialien.voicechatrecording.api.IRecordedAudio;

public class AudioRecordedEvent extends AudioEvent {
    public AudioRecordedEvent(IRecordedAudio audio){
        super(audio);
    }
}
