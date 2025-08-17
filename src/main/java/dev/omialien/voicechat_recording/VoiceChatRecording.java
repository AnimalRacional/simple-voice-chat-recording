package dev.omialien.voicechat_recording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import dev.omialien.voicechat_recording.configs.RecordingClientConfig;
import dev.omialien.voicechat_recording.configs.RecordingCommonConfig;
import dev.omialien.voicechat_recording.taskscheduler.TaskScheduler;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VoiceChatRecording.MOD_ID)
public class VoiceChatRecording {
    public static VoicechatApi vcApi;
    public static TaskScheduler TASKS;
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "voicechatrecording";
    public static final String CATEGORY_ID = "recording_vc";
    public static final LevelResource AUDIO_DIRECTORY = new LevelResource("player_audios");
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public VoiceChatRecording(IEventBus modEventBus, ModContainer modContainer) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, RecordingCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RecordingClientConfig.SPEC);
        TASKS = new TaskScheduler();
    }
}

