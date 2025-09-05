package dev.omialien.voicechat_recording.voicechat.events;

import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;

public class AudioLoadedEvent extends AudioEvent {
    private final VoiceChatRecordingPlugin.LoadType reason;
    public AudioLoadedEvent(RecordedAudio audio, VoiceChatRecordingPlugin.LoadType loadType){
        super(audio);
        this.reason = loadType;
    }

    public VoiceChatRecordingPlugin.LoadType getLoadReason(){
        return this.reason;
    }
}
