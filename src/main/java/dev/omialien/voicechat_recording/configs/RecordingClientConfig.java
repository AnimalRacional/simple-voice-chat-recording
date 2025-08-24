package dev.omialien.voicechat_recording.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class RecordingClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<Boolean> PRIVACY_MODE;

    static {
        BUILDER.push("Client Configs for Voice Chat Recording Mod");
        PRIVACY_MODE = BUILDER.comment("Whether your audios get saved to the disk of servers you speak on").define("Privacy Mode", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
