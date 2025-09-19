package dev.omialien.voicechat_recording.voicechat.events;

import net.neoforged.bus.api.Event;

/**
 * This event is fired after a server is started and the setup for recording audios is finished
 * Load your startup audios here
 */
// TODO make mods not use VoiceChatRecordingPlugin directly and instead send it as an interface here
// If/when that's done, remember to register the categories after this event, as mods should register them here
// since they don't have access to the API before this
public class RecordingSetupEvent extends Event {
}
