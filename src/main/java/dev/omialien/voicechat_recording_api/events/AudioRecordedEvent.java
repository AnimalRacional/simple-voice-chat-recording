package dev.omialien.voicechat_recording_api.events;

import dev.omialien.voicechat_recording_api.IRecordedAudio;

public class AudioRecordedEvent extends AudioEvent {
    public AudioRecordedEvent(IRecordedAudio audio){
        super(audio);
    }
}
