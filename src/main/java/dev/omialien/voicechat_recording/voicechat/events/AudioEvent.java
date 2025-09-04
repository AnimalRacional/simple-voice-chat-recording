package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import net.neoforged.bus.api.Event;

public class AudioEvent extends Event {
    private final RecordedAudio audio;
    public AudioEvent(RecordedAudio audio){
        this.audio = audio;
    }
    public RecordedAudio getAudio(){
        return this.audio;
    }
}
