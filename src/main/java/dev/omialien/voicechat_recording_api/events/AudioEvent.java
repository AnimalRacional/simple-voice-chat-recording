package dev.omialien.voicechat_recording_api.events;

import dev.omialien.voicechat_recording_api.IRecordedAudio;
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
