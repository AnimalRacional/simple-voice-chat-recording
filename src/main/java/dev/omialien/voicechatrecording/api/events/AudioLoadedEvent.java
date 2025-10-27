package dev.omialien.voicechatrecording.api.events;

import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.VoiceChatRecording;

public class AudioLoadedEvent extends AudioEvent {
    public enum LoadType {
        SINGLE,
        ALL_FROM_USER,
        NAMESPACE
    }
    private final LoadType reason;
    private final String namespace;
    public AudioLoadedEvent(IRecordedAudio audio, LoadType loadType, String namespace){
        super(audio);
        this.reason = loadType;
        this.namespace = namespace;
    }

    public AudioLoadedEvent(IRecordedAudio audio, LoadType loadType) {
        this(audio, loadType, "");
        if(loadType == LoadType.NAMESPACE) {
            VoiceChatRecording.LOGGER.error("Firing AudioLoadedEvent with type namespace without specifying namespace");
        }
    }

    public LoadType getLoadReason(){
        return this.reason;
    }

    public String getNamespace() { return this.namespace; }
}
