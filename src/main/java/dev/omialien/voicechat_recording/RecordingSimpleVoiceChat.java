package dev.omialien.voicechat_recording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import dev.omialien.voicechat_recording.commands.*;
import dev.omialien.voicechat_recording.configs.RecordingClientConfig;
import dev.omialien.voicechat_recording.configs.RecordingServerConfig;
import dev.omialien.voicechat_recording.events.ClientForgeEventBus;
import dev.omialien.voicechat_recording.networking.RecordingPacketHandler;
import dev.omialien.voicechat_recording.taskscheduler.TaskScheduler;
import dev.omialien.voicechat_recording.voicechat.RecordedPlayer;
import dev.omialien.voicechat_recording.voicechat.RecordingSimpleVoiceChatPlugin;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
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
    public static final String CATEGORY_ID = "recording_vc";
    public static final LevelResource AUDIO_DIRECTORY = new LevelResource("player_audios");
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public RecordingSimpleVoiceChat(FMLJavaModLoadingContext ctx){
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.debug("FMLJavaModLoadingContext is being used!");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new ClientForgeEventBus()));
        RecordingPacketHandler.registerPackets();
        ctx.registerConfig(ModConfig.Type.SERVER, RecordingServerConfig.SPEC);
        ctx.registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }

    public RecordingSimpleVoiceChat() {
        LOGGER.warn("Old version: not using FMLJavaModLoadingContext");
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new ClientForgeEventBus()));
        RecordingPacketHandler.registerPackets();
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RecordingServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RecordingSimpleVoiceChatPlugin.addCategory(RecordingSimpleVoiceChat.CATEGORY_ID, "Recording Plugin", "Description", null, (VoicechatServerApi) vcApi);
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
        if(event.type == TickEvent.Type.SERVER && event.phase == TickEvent.Phase.END){
            TASKS.tick();
        }
    }
}
