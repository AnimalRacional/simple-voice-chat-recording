package dev.omialien.voicechatrecording.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class RecordingClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<Boolean> PRIVACY;

    static {
        BUILDER.push("Client Configs for Voice Chat Recording Mod");
        PRIVACY = BUILDER.comment("Whether your audios get saved to the disk of servers you speak on").define("Privacy Mode", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
