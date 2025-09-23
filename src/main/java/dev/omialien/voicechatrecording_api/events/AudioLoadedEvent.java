package dev.omialien.voicechatrecording_api.events;

import dev.omialien.voicechatrecording_api.IRecordedAudio;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;

public class AudioLoadedEvent extends AudioEvent {
    private final VoiceChatRecordingPlugin.LoadType reason;
    public AudioLoadedEvent(IRecordedAudio audio, VoiceChatRecordingPlugin.LoadType loadType){
        super(audio);
        this.reason = loadType;
    }

    public VoiceChatRecordingPlugin.LoadType getLoadReason(){
        return this.reason;
    }
}
