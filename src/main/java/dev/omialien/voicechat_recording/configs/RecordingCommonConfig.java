package dev.omialien.voicechat_recording.configs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RecordingCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<Integer> RECORDING_LIMIT = BUILDER
            .define("maxSavedRecordings", 200);
    public static final ModConfigSpec.ConfigValue<Integer> SILENCE_THRESHOLD = BUILDER
            .define("silenceThreshold", 700);
    public static final ModConfigSpec.ConfigValue<Integer> AUDIO_READER_THREAD_COUNT = BUILDER
        .define("readingThreadCount", 4);
    public static final ModConfigSpec.ConfigValue<Integer> MINIMUM_AUDIO_COUNT = BUILDER
            .define("minimumAudioCount", 100);
    public static final ModConfigSpec SPEC = BUILDER.build();
}
