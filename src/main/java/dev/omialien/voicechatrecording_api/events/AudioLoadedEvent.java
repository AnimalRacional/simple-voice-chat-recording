package dev.omialien.voicechatrecording_api.events;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording_api.IRecordedAudio;

public class AudioLoadedEvent extends AudioEvent {
    public enum LoadType {
        SINGLE,
        ALL_FROM_USER,
        NAMESPACE
    }
    private final LoadType reason;
    private final String namespace;
    public AudioLoadedEvent(IRecordedAudio audio, AudioLoadedEvent.LoadType loadType, String namespace){
        super(audio);
        this.reason = loadType;
        this.namespace = namespace;
    }

    public AudioLoadedEvent(IRecordedAudio audio, AudioLoadedEvent.LoadType loadType) {
        this(audio, loadType, "");
        if(loadType == AudioLoadedEvent.LoadType.NAMESPACE) {
            VoiceChatRecording.LOGGER.error("Firing AudioLoadedEvent with type namespace without specifying namespace");
        }
    }

    public AudioLoadedEvent.LoadType getLoadReason(){
        return this.reason;
    }

    public String getNamespace() { return this.namespace; }
}
