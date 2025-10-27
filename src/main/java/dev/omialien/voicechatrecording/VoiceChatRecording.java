package dev.omialien.voicechatrecording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import dev.omialien.voicechatrecording.configs.RecordingClientConfig;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.networking.ServerPayloadHandler;
import dev.omialien.voicechatrecording.taskscheduler.TaskScheduler;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.api.VoiceChatRecordingApi;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VoiceChatRecording.MOD_ID)
public class VoiceChatRecording {
    public static VoicechatServerApi vcApi;
    public static TaskScheduler TASKS;
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "voicechatrecording";
    public static final String CATEGORY_ID = "recording_vc";
    public static final LevelResource AUDIO_DIRECTORY = new LevelResource("player_audios");
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static List<IRecordedAudio> storedAudios = new ArrayList<>();
    public static VoiceChatRecordingApi recordingApi;

    public VoiceChatRecording(FMLJavaModLoadingContext ctx){
        LOGGER.debug("FMLJavaModLoadingContext is being used!");
        ServerPayloadHandler.registerPackets();
        ctx.registerConfig(ModConfig.Type.COMMON, RecordingCommonConfig.SPEC);
        ctx.registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }

    public VoiceChatRecording() {
        LOGGER.warn("Old version: not using FMLJavaModLoadingContext");
        ServerPayloadHandler.registerPackets();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecordingCommonConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }
}

