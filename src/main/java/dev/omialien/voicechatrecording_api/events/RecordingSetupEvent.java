package dev.omialien.voicechatrecording_api.events;

import de.maxhenkel.voicechat.api.VolumeCategory;
import dev.omialien.voicechatrecording_api.VoiceChatRecordingApi;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This event is fired after a server is started and the setup for recording audios is finished
 * Load your startup audios here
 */
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
