package dev.omialien.voicechat_recording.networking;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handlePrivacy(final PrivacyModePacket data, final IPayloadContext ctx){
        VoiceChatRecording.LOGGER.info("Setting {}'s privacy mode to {}", ctx.player().getName(), data.state());
        VoiceChatRecordingPlugin.setPrivacy(ctx.player().getUUID(), data.state());
    }
}
