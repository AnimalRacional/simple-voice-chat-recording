package dev.omialien.voicechatrecording.configs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RecordingCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<Integer> SILENCE_THRESHOLD = BUILDER
            .define("silenceThreshold", 700);
    public static final ModConfigSpec.ConfigValue<Integer> AUDIO_READER_THREAD_COUNT = BUILDER
        .define("readingThreadCount", 4);
    public static final ModConfigSpec.ConfigValue<Integer> AUDIO_SAVER_THREAD_COUNT = BUILDER
            .define("saveThreadCount", 4);
    public static final ModConfigSpec.ConfigValue<Integer> CACHE_REMOVAL_TIME = BUILDER
            .define("cacheRemovalTime", (5 * 60 * 1000));
    public static final ModConfigSpec SPEC = BUILDER.build();
}
