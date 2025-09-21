package dev.omialien.voicechat_recording_api.events;

import de.maxhenkel.voicechat.api.VolumeCategory;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording_api.VoiceChatRecordingApi;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This event is fired after a server is started and the setup for recording audios is finished
 * Load your startup audios here
 */
// TODO make mods not use VoiceChatRecordingPlugin directly and instead send it as an interface here
// If/when that's done, remember to register the categories after this event, as mods should register them here
// since they don't have access to the API before this
public class RecordingSetupEvent extends Event {
    private VoiceChatRecordingApi api;
    private Collection<VolumeCategory> categories;
    public RecordingSetupEvent(VoiceChatRecordingApi api) {
        this.api = api;
        this.categories = new LinkedList<>();
    }

    public VoiceChatRecordingApi getApi() {
        return api;
    }

    public Iterator<VolumeCategory> getCategories() {
        return this.categories.iterator();
    }

    public void addCategory(String id, String name, String description, @Nullable int[][] icon){
        categories.add(VoiceChatRecording.vcApi.volumeCategoryBuilder()
                .setId(id)
                .setName(name)
                .setDescription(description)
                .setIcon(icon)
                .build());
    }
}
