package dev.omialien.voicechat_recording.events;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.commands.*;
import dev.omialien.voicechat_recording.networking.PrivacyModePacket;
import dev.omialien.voicechat_recording.networking.ServerPayloadHandler;
import dev.omialien.voicechat_recording.voicechat.RecordedPlayer;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Files;

@EventBusSubscriber(modid = VoiceChatRecording.MOD_ID)
public class CommonEventBus {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        VoiceChatRecordingPlugin.addCategory(VoiceChatRecording.CATEGORY_ID, "Recording Plugin", "The volume of recorded voices", null, (VoicechatServerApi) VoiceChatRecording.vcApi);
        VoiceChatRecording.LOGGER.debug("Server starting");
        RecordedPlayer.audiosPath = event.getServer().getWorldPath(VoiceChatRecording.AUDIO_DIRECTORY);
        if(!Files.exists(RecordedPlayer.audiosPath)){
            try {
                Files.createDirectory(RecordedPlayer.audiosPath);
            } catch (IOException e) {
                VoiceChatRecording.LOGGER.error("Error creating audios directory: " + e.getMessage());
            }
        }
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NearestEntityPlayVoiceCommand.register(event.getDispatcher());
        StartRecordingCommand.register(event.getDispatcher());
        StopRecordingCommand.register(event.getDispatcher());
        isRecordingCommand.register(event.getDispatcher());
        ScheduleLogCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void tickEvent(ServerTickEvent.Post event){
        VoiceChatRecording.TASKS.tick();
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent e){
        final PayloadRegistrar registrar = e.registrar("1");
        registrar.playToServer(
                PrivacyModePacket.TYPE,
                PrivacyModePacket.STREAM_CODEC,
                ServerPayloadHandler::handlePrivacy
        );
    }
}
