package dev.omialien.voicechatrecording_api.events;

import dev.omialien.voicechatrecording_api.IRecordedAudio;
import net.neoforged.bus.api.Event;

public class AudioEvent extends Event {
    private final IRecordedAudio audio;
    public AudioEvent(IRecordedAudio audio){
        this.audio = audio;
    }
    public IRecordedAudio getAudio(){
        return this.audio;
    }
}
