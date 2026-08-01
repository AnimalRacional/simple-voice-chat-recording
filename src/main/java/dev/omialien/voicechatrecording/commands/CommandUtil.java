package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.omialien.voicechatrecording.api.AudioId;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CommandUtil {
    // TODO maybe only get the audios in the API's namespace?
    public static Supplier<RequiredArgumentBuilder<CommandSourceStack, UUID>> PLAYER_ARGUMENT =
            () -> Commands.argument("player",
                    UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                    SharedSuggestionProvider.suggest(
                            CommandUtil.getAllSavedAudios().map((p) -> p.player().toString()), suggestionsBuilder));
    public static Supplier<RequiredArgumentBuilder<CommandSourceStack, UUID>> AUDIO_ARGUMENT =
            () -> Commands.argument("audio",
                    UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                    SharedSuggestionProvider.suggest(
                            CommandUtil.getAllSavedAudios().map((p) -> p.audio().toString()), suggestionsBuilder));

    public static Stream<AudioId> getAllSavedAudios() {
        return ((VoiceChatRecordingPlugin) VoiceChatRecording.recordingApi).savedAudios.values().stream()
                .flatMap(Set::stream);
    }

    public static Stream<IRecordedAudio> getRememberedAudios() {
        return VoiceChatRecording.storedAudios.stream();
    }

    public static Stream<AudioId> getSavedAudios(String namespace) {
        return VoiceChatRecording.recordingApi.getNamespaceAudios(namespace).stream();
    }

    public static void loadAudio(UUID player, UUID id, Consumer<IRecordedAudio> reaction) {
        VoiceChatRecording.recordingApi.loadAudio(player, id, reaction);
    }
}

