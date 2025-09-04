package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.RecordedAudio;

public class AudioLoadedEvent extends AudioEvent {
    public AudioLoadedEvent(RecordedAudio audio){
        super(audio);
    }
}
