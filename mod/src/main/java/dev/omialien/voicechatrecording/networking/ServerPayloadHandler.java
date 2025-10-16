package dev.omialien.voicechatrecording.networking;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handlePrivacy(final PrivacyModePacket data, final IPayloadContext ctx){
        VoiceChatRecording.LOGGER.info("Setting {}'s privacy mode to {}", ctx.player().getName(), data.state());
        ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).setPrivacy(ctx.player().getUUID(), data.state());
    }
}
