package dev.omialien.voicechatrecording_api.events;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording_api.IRecordedAudio;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;

public class AudioLoadedEvent extends AudioEvent {
    private final VoiceChatRecordingPlugin.LoadType reason;
    private final String namespace;
    public AudioLoadedEvent(IRecordedAudio audio, VoiceChatRecordingPlugin.LoadType loadType, String namespace){
        super(audio);
        this.reason = loadType;
        this.namespace = namespace;
    }

    public AudioLoadedEvent(IRecordedAudio audio, VoiceChatRecordingPlugin.LoadType loadType) {
        this(audio, loadType, "");
        if(loadType == VoiceChatRecordingPlugin.LoadType.NAMESPACE) {
            VoiceChatRecording.LOGGER.error("Firing AudioLoadedEvent with type namespace without specifying namespace");
        }
    }

    public VoiceChatRecordingPlugin.LoadType getLoadReason(){
        return this.reason;
    }

    public String getNamespace() { return this.namespace; }
}
