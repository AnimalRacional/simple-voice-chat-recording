package dev.omialien.voicechatrecording.events;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.configs.RecordingClientConfig;
import dev.omialien.voicechatrecording.networking.PrivacyModePacket;
import dev.omialien.voicechatrecording.networking.ServerPayloadHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = VoiceChatRecording.MOD_ID, value = Dist.CLIENT)
public class ClientEventBus {
    private static void sendPrivacyPacket(){
        if(Minecraft.getInstance().getConnection() != null){
            PrivacyModePacket packet = new PrivacyModePacket(RecordingClientConfig.PRIVACY.get());
            VoiceChatRecording.LOGGER.debug("Client Sending privacy mode packet");
            ServerPayloadHandler.INSTANCE.sendToServer(packet);
        } else {
            VoiceChatRecording.LOGGER.debug("Tried to send packet without connection");
        }
    }
    @SubscribeEvent
    public static void onClientJoinLevel(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof LocalPlayer){
            sendPrivacyPacket();
        }
    }

    @SubscribeEvent
    public static void onConfigChange(ModConfigEvent.Reloading e){
        sendPrivacyPacket();
    }
}
