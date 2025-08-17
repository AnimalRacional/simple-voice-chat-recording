package dev.omialien.voicechat_recording.events;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.configs.RecordingClientConfig;
import dev.omialien.voicechat_recording.networking.PrivacyModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value=VoiceChatRecording.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = VoiceChatRecording.MOD_ID, value = Dist.CLIENT)
public class ClientEventBus {
    public ClientEventBus(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
    private static void sendPrivacyPacket(){
        if(Minecraft.getInstance().getConnection() != null){
            PrivacyModePacket packet = new PrivacyModePacket(RecordingClientConfig.PRIVACY.get());
            VoiceChatRecording.LOGGER.debug("Sending privacy mode packet {}", packet.state());
            PacketDistributor.sendToServer(packet);
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
