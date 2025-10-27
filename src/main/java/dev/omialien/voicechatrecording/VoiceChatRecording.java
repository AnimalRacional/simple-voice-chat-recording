package dev.omialien.voicechatrecording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import dev.omialien.voicechatrecording.configs.RecordingClientConfig;
import dev.omialien.voicechatrecording.configs.RecordingCommonConfig;
import dev.omialien.voicechatrecording.taskscheduler.TaskScheduler;
import dev.omialien.voicechatrecording_api.IRecordedAudio;
import dev.omialien.voicechatrecording_api.VoiceChatRecordingApi;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public VoiceChatRecording(IEventBus modEventBus, ModContainer modContainer) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, RecordingCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }
}

