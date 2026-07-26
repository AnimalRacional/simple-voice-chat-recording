package dev.omialien.voicechatrecording.events;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.commands.*;
import dev.omialien.voicechatrecording.voicechat.RecordedAudio;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.api.events.AudioEvent;
import dev.omialien.voicechatrecording.api.events.AudioLoadedEvent;
import dev.omialien.voicechatrecording.api.events.RecordingSetupEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = VoiceChatRecording.MOD_ID)
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
                VoiceChatRecording.LOGGER.error("Error creating audios directory: {}", e.getMessage());
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
            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).shutdownSaving();
            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).shutdownAudioLoading();
        } catch(InterruptedException e){
            VoiceChatRecording.LOGGER.error("Audio saving shutdown interrupted! {}\n{}", e.getMessage(), e.getStackTrace());
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
        LoadAudioCommand.register(event.getDispatcher());
        UnsaveAudioCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void tickEvent(TickEvent.ServerTickEvent event){
        if ( event.phase == TickEvent.Phase.END ) {
            VoiceChatRecording.TASKS.tick();
        }
    }

    @SubscribeEvent
    public static void onLoadedAudio(AudioLoadedEvent event){
        VoiceChatRecording.LOGGER.debug("EVENT: Audio loaded! {} {} {}", event.getAudio().getFilterResult(), event.getAudio().getPlayerUUID(), event.getAudio().getId());
    }

    @SubscribeEvent
    public static void onGenericAudio(AudioEvent event){
        VoiceChatRecording.LOGGER.debug("GENERIC EVENT: audio {} {} {}", event.getAudio().getFilterResult().toString(), event.getAudio().getPlayerUUID(), event.getAudio().getId());
        if(RememberAudiosCommand.shouldRemember) {
            IRecordedAudio.FilterResult filter = event.getAudio().getFilterResult();
            if((filter == IRecordedAudio.FilterResult.PASSED || filter == IRecordedAudio.FilterResult.TOO_LONG)) {
                VoiceChatRecording.LOGGER.debug("remembering");
                VoiceChatRecording.storedAudios.add(event.getAudio());
            }
        }
    }
}
