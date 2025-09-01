package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import net.neoforged.bus.api.Event;

public class AudioLoadedEvent extends Event {
    private RecordedAudio audio;
    public AudioLoadedEvent(RecordedAudio audio){
        this.audio = audio;
    }

    public RecordedAudio getAudio(){
        return audio;
    }
}
