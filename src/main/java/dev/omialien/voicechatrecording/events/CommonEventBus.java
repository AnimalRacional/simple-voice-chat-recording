package dev.omialien.voicechatrecording.events;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.commands.*;
import dev.omialien.voicechatrecording.networking.PrivacyModePacket;
import dev.omialien.voicechatrecording.networking.ServerPayloadHandler;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.voicechat.RecordedAudio;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechatrecording.api.events.AudioEvent;
import dev.omialien.voicechatrecording.api.events.AudioLoadedEvent;
import dev.omialien.voicechatrecording.api.events.RecordingSetupEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

@EventBusSubscriber(modid = VoiceChatRecording.MOD_ID)
public class CommonEventBus {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        VoiceChatRecording.storedAudios = new ArrayList<>();
        VoiceChatRecording.LOGGER.debug("Server starting");
        RecordedAudio.audiosPath = event.getServer().getWorldPath(VoiceChatRecording.AUDIO_DIRECTORY);
        if(!Files.exists(RecordedAudio.audiosPath)){
            try {
                Files.createDirectory(RecordedAudio.audiosPath);
            } catch (IOException e) {
                VoiceChatRecording.LOGGER.error("Error creating audios directory", e);
            }
        }
    }

    @SubscribeEvent
    public static void onRecordingSetup(RecordingSetupEvent event) {
        event.addCategory(VoiceChatRecording.CATEGORY_ID, "Recording Plugin", "The volume of recorded voices", null);
    }

    // FIXME when leaving a single player game, there's a delay in saving the world, even if no audios get saved
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerClosed(ServerStoppedEvent event){
        try{
            VoiceChatRecording.LOGGER.info("Shutting down audio saving...");
            RememberAudiosCommand.shouldRemember = false;
            VoiceChatRecording.storedAudios.clear();
            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).shutdownAudioSaving();
            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).shutdownAudioLoading();
        } catch(InterruptedException e){
            VoiceChatRecording.LOGGER.error("Audio saving shutdown interrupted!", e);
        }
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PlayVoiceCommand.register(event.getDispatcher());
        StartRecordingCommand.register(event.getDispatcher());
        StopRecordingCommand.register(event.getDispatcher());
        IsRecordingCommand.register(event.getDispatcher());
        ScheduleLogCommand.register(event.getDispatcher());
        RememberAudiosCommand.register(event.getDispatcher());
        ListAudiosCommand.register(event.getDispatcher());
        SaveAudioCommand.register(event.getDispatcher());
        AudioInfoCommand.register(event.getDispatcher());
        LoadAudioCommand.register(event.getDispatcher());
        UnsaveAudioCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void tickEvent(ServerTickEvent.Post event){
        VoiceChatRecording.TASKS.tick();
        ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).tick();
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
    public static void onLoadedAudio(AudioLoadedEvent event){
        if (event.getAudio() == null) { return; }
        if (VoiceChatRecording.LOGGER.isDebugEnabled()) {
            VoiceChatRecording.LOGGER.debug("EVENT: Audio loaded! {} {} {}", event.getAudio().getFilterResult(), event.getAudio().getPlayerUUID(), event.getAudio().getId());
        }
    }

    @SubscribeEvent
    public static void onGenericAudio(AudioEvent event){
        if (event.getAudio() == null) { return; }
        if (VoiceChatRecording.LOGGER.isDebugEnabled()) {
            VoiceChatRecording.LOGGER.debug("GENERIC EVENT: audio {} {} {}", event.getAudio().getFilterResult().toString(), event.getAudio().getPlayerUUID(), event.getAudio().getId());
        }
        if(RememberAudiosCommand.shouldRemember) {
            IRecordedAudio.FilterResult filter = event.getAudio().getFilterResult();
            if((filter == IRecordedAudio.FilterResult.PASSED || filter == IRecordedAudio.FilterResult.TOO_LONG)) {
                VoiceChatRecording.LOGGER.debug("remembering");
                VoiceChatRecording.storedAudios.add(event.getAudio());
            }
        }
    }
}
