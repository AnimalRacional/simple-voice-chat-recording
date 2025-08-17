package dev.omialien.voicechat_recording.networking;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record PrivacyModePacket(boolean state) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PrivacyModePacket> TYPE =
            new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(VoiceChatRecording.MOD_ID,
                        "privacymode"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PrivacyModePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PrivacyModePacket::state,
            PrivacyModePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
