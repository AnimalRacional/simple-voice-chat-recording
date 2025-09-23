package dev.omialien.voicechatrecording_api.events;

import dev.omialien.voicechatrecording_api.IRecordedAudio;

public class AudioRecordedEvent extends AudioEvent {
    public AudioRecordedEvent(IRecordedAudio audio){
        super(audio);
    }
}
