package dev.omialien.voicechat_recording.events;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.commands.*;
import dev.omialien.voicechat_recording.networking.PrivacyModePacket;
import dev.omialien.voicechat_recording.networking.ServerPayloadHandler;
import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechat_recording.voicechat.events.AudioEvent;
import dev.omialien.voicechat_recording.voicechat.events.AudioLoadedEvent;
import dev.omialien.voicechat_recording.voicechat.events.AudioRecordedEvent;
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
        VoiceChatRecordingPlugin.addCategory(VoiceChatRecording.CATEGORY_ID, "Recording Plugin", "The volume of recorded voices", null);
        VoiceChatRecording.LOGGER.debug("Server starting");
        RecordedAudio.audiosPath = event.getServer().getWorldPath(VoiceChatRecording.AUDIO_DIRECTORY);
        if(!Files.exists(RecordedAudio.audiosPath)){
            try {
                Files.createDirectory(RecordedAudio.audiosPath);
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
        IsRecordingCommand.register(event.getDispatcher());
        ScheduleLogCommand.register(event.getDispatcher());
        RememberAudiosCommand.register(event.getDispatcher());
        ListAudiosCommand.register(event.getDispatcher());
        SaveAudioCommand.register(event.getDispatcher());
        AudioInfoCommand.register(event.getDispatcher());
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

    @SubscribeEvent
    public static void onRecordedAudio(AudioRecordedEvent event){
        RecordedAudio audio = event.getAudio();

        if(RememberAudiosCommand.shouldRemember && audio.getFilterResult() == RecordedAudio.FilterResult.PASSED){
            VoiceChatRecording.LOGGER.debug("EVENT: Audio recorded! Filter result: {}", audio.getFilterResult());
            VoiceChatRecording.storedAudios.add(audio);
        }
    }

    @SubscribeEvent
    public static void onLoadedAudio(AudioLoadedEvent event){
        VoiceChatRecording.LOGGER.debug("EVENT: Audio loaded! {} {}", event.getAudio().getPlayerUUID(), event.getAudio().getId());
        // TODO add shouldRemember check
        if(event.getAudio().getFilterResult() == RecordedAudio.FilterResult.PASSED){
            VoiceChatRecording.LOGGER.debug("EVENT: Audio recorded! Filter result: {}", event.getAudio().getFilterResult());
            VoiceChatRecording.storedAudios.add(event.getAudio());
        }
    }

    @SubscribeEvent
    public static void onGenericAudio(AudioEvent event){
        VoiceChatRecording.LOGGER.debug("GENERIC EVENT: audio happened {} {}", event.getAudio().getPlayerUUID(), event.getAudio().getId());
    }
}
