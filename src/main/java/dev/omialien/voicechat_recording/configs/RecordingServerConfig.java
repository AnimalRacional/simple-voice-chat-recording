package dev.omialien.voicechat_recording.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class RecordingServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<Integer> RECORDING_LIMIT;
    public static final ForgeConfigSpec.ConfigValue<Integer> SILENCE_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> AUDIO_READER_THREAD_COUNT;
    public static final ForgeConfigSpec.ConfigValue<Integer> MINIMUM_AUDIO_COUNT;

    static {
        BUILDER.push("Server Configs for Voice Chat Recording Mod");
        RECORDING_LIMIT = BUILDER.comment("Maximum audios that will be saved in memory").define("Max Saved Recordings", 200);
        SILENCE_THRESHOLD = BUILDER.comment("Amplitude threshold to detect speech. Change this if you feel like Revervox notices you even when you're not speaking").define("Silence Threshold", 700);
        AUDIO_READER_THREAD_COUNT = BUILDER.comment("How many threads are used to read the saved audios from a player that joins the server. Lower this if a player joining the server lags, and raise it if it doesn't but audio reading is too slow.").define("Audio Reading Thread Count", 4);
        MINIMUM_AUDIO_COUNT = BUILDER.comment("The minimum amount of audios to keep in memory; audios are only gonna be removed if it won't make the total audio count go below this number").define("Minimum Audio Count", 100);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
