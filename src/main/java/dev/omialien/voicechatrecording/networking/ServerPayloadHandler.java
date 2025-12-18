package dev.omialien.voicechatrecording.networking;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ServerPayloadHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VoiceChatRecording.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            ServerPayloadHandler::checkVersion,
            ServerPayloadHandler::checkVersion
    );

    private static boolean checkVersion(String v) {
        return v.equals(PROTOCOL_VERSION) || v.startsWith("ALLOWVANILLA") || v.startsWith("ABSENT");
    }

    public static void registerPackets(){
        int id = 0;
        INSTANCE.registerMessage(
                id++,
                PrivacyModePacket.class,
                PrivacyModePacket::encode,
                PrivacyModePacket::decode,
                PrivacyModePacket::handle
        );
    }

}
