package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.IRecordedAudio;
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
