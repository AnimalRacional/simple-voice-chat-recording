package dev.omialien.voicechat_recording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import dev.omialien.voicechat_recording.commands.*;
import dev.omialien.voicechat_recording.configs.RecordingServerConfig;
import dev.omialien.voicechat_recording.taskscheduler.TaskScheduler;
import dev.omialien.voicechat_recording.voicechat.RecordedPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RecordingSimpleVoiceChat.MOD_ID)
public class RecordingSimpleVoiceChat {
    public static VoicechatApi vcApi;
    public static TaskScheduler TASKS;
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "voicechat_recording";
    public static final LevelResource AUDIO_DIRECTORY = new LevelResource("player_audios");
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public RecordingSimpleVoiceChat(FMLJavaModLoadingContext ctx) {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ctx.registerConfig(ModConfig.Type.SERVER, RecordingServerConfig.SPEC);
        TASKS = new TaskScheduler();
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RecordingSimpleVoiceChat.LOGGER.debug("Server starting");
        RecordedPlayer.audiosPath = event.getServer().getWorldPath(RecordingSimpleVoiceChat.AUDIO_DIRECTORY);
        if(!Files.exists(RecordedPlayer.audiosPath)){
            try {
                Files.createDirectory(RecordedPlayer.audiosPath);
            } catch (IOException e) {
                RecordingSimpleVoiceChat.LOGGER.error("Error creating audios directory: " + e.getMessage());
            }
        }
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NearestEntityPlayVoiceCommand.register(event.getDispatcher());
        StartRecordingCommand.register(event.getDispatcher());
        StopRecordingCommand.register(event.getDispatcher());
        isRecordingCommand.register(event.getDispatcher());
        ScheduleLogCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void tickEvent(TickEvent event){
        if(event.phase == TickEvent.Phase.END){
            TASKS.tick();
        }
    }
}
