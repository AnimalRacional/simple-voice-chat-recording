package dev.omialien.voicechatrecording.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class RecordingCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<Integer> SILENCE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> AUDIO_READER_THREAD_COUNT;
    public static final ForgeConfigSpec.ConfigValue<Integer> AUDIO_SAVER_THREAD_COUNT;
    public static final ForgeConfigSpec.ConfigValue<Integer> CACHE_REMOVAL_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> NAMESPACE_SAVE_TICKS;

    static {
        BUILDER.push("Server Configs for Voice Chat Recording Mod");
        SILENCE_THRESHOLD = BUILDER.comment("Amplitude threshold to detect speech. Change this if you feel like Revervox notices you even when you're not speaking")
                .define("Silence Threshold", 700);
        AUDIO_READER_THREAD_COUNT = BUILDER.comment("How many threads are used to read the saved audios from a player that joins the server. Lower this if a player joining the server lags, and raise it if it doesn't but audio reading is too slow.")
                .define("Audio Reading Thread Count", 4);
        AUDIO_SAVER_THREAD_COUNT = BUILDER.comment("How many threads to use when saving audios")
                .define("Saving Thread Count", 4);
        CACHE_REMOVAL_TIME = BUILDER.comment("How long, in milliseconds, an audio loaded from disk stays in cache. Higher means more memory usage, but faster when loading the same audio repeatedly")
                .define("Cache Removal Time", 5 * 60 * 1000);
        NAMESPACE_SAVE_TICKS = BUILDER.comment("Amount of ticks between saving namespace files")
                .define("Namespace Save Ticks", (20*20));
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
