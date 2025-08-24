package dev.omialien.voicechat_recording.events;

import dev.omialien.voicechat_recording.RecordingSimpleVoiceChat;
import dev.omialien.voicechat_recording.configs.RecordingClientConfig;
import dev.omialien.voicechat_recording.networking.RecordingPacketHandler;
import dev.omialien.voicechat_recording.networking.packets.PrivacyModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientForgeEventBus {
    @SubscribeEvent
    public void clientJoinEvent(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide() &&
                event.getEntity() instanceof LocalPlayer){
            Minecraft.getInstance().submit(() -> {
                RecordingSimpleVoiceChat.LOGGER.debug("Client Sending EntityJoinLevelEvent Packet!");
                RecordingPacketHandler.INSTANCE.sendToServer(new PrivacyModePacket(RecordingClientConfig.PRIVACY_MODE.get()));
            });
        }
    }
}
