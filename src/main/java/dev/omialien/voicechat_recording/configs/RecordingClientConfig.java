package dev.omialien.voicechat_recording.configs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RecordingClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue PRIVACY = BUILDER
            .define("privacyMode", false);
    public static final ModConfigSpec SPEC = BUILDER.build();
}
