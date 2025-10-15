package dev.omialien.voicechatrecording.networking;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PrivacyModePacket {
    private final boolean state;

    public PrivacyModePacket(boolean s){
        VoiceChatRecording.LOGGER.debug("Created PrivacyModePacket");
        this.state = s;
    }

    public void encode(FriendlyByteBuf buffer){
        // Fill the buffer with packet
        VoiceChatRecording.LOGGER.debug("Encoded PrivacyModePacket");
        buffer.writeBoolean(this.state);
    }

    public static PrivacyModePacket decode(FriendlyByteBuf buffer){
        // Create the packet from the buffer
        VoiceChatRecording.LOGGER.debug("Decoded PrivacyModePacket");
        return new PrivacyModePacket(buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx){
        // Handle the packet
        VoiceChatRecording.LOGGER.debug("Handling PrivacyModePacket");
        ctx.get().enqueueWork(() -> {
            VoiceChatRecording.LOGGER.debug("PrivacyModePacket received!");
            ServerPlayer sender = ctx.get().getSender();
            if(sender != null){
                VoiceChatRecording.LOGGER.debug("Setting privacy mode for {} to {}", sender.getName(), state);
                ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).setPrivacy(sender.getUUID(), state);
            } else {
                VoiceChatRecording.LOGGER.warn("Received PrivacyModePacket without sender?");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}