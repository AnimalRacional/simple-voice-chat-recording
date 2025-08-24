package dev.omialien.voicechat_recording.networking.packets;

import dev.omialien.voicechat_recording.RecordingSimpleVoiceChat;
import dev.omialien.voicechat_recording.voicechat.RecordingSimpleVoiceChatPlugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PrivacyModePacket {
    private final boolean state;

    public PrivacyModePacket(boolean s){
        RecordingSimpleVoiceChat.LOGGER.debug("Created PrivacyModePacket");
        this.state = s;
    }

    public void encode(FriendlyByteBuf buffer){
        // Fill the buffer with packet
        RecordingSimpleVoiceChat.LOGGER.debug("Encoded PrivacyModePacket");
        buffer.writeBoolean(this.state);
    }

    public static PrivacyModePacket decode(FriendlyByteBuf buffer){
        // Create the packet from the buffer
        RecordingSimpleVoiceChat.LOGGER.debug("Decoded PrivacyModePacket");
        return new PrivacyModePacket(buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx){
        // Handle the packet
        RecordingSimpleVoiceChat.LOGGER.debug("Handling PrivacyModePacket");
        ctx.get().enqueueWork(() -> {
            RecordingSimpleVoiceChat.LOGGER.debug("PrivacyModePacket received!");
            ServerPlayer sender = ctx.get().getSender();
            if(sender != null){
                RecordingSimpleVoiceChat.LOGGER.debug("Setting privacy mode for {} to {}", sender.getName(), state);
                RecordingSimpleVoiceChatPlugin.setPrivacy(sender.getUUID(), state);
            } else {
                RecordingSimpleVoiceChat.LOGGER.warn("Received PrivacyModePacket without sender?");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}