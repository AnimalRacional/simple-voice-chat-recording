package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechatrecording_api.IRecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CommandUtil {
    // TODO maybe only get the audios in the API's namespace?
    public static RequiredArgumentBuilder<CommandSourceStack, UUID> PLAYER_ARGUMENT =
            Commands.argument("player",
                    UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                    SharedSuggestionProvider.suggest(
                            CommandUtil.getAllSavedAudios().map((p) -> p.getFirst().toString()), suggestionsBuilder));
    public static RequiredArgumentBuilder<CommandSourceStack, UUID> AUDIO_ARGUMENT =
            Commands.argument("audio",
                    UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                    SharedSuggestionProvider.suggest(
                            CommandUtil.getAllSavedAudios().map((p) -> p.getSecond().toString()), suggestionsBuilder));

    public static Stream<Pair<UUID, UUID>> getAllSavedAudios() {
        return ((VoiceChatRecordingPlugin) VoiceChatRecording.recordingApi).savedAudios.values().stream()
                .flatMap(Set::stream);
    }

    public static Stream<IRecordedAudio> getRememberedAudios() {
        return VoiceChatRecording.storedAudios.stream();
    }

    public static void sendInterruptFailure(CommandSourceStack src) {
        src.sendFailure(Component.literal("Audio loading was interrupted!"));
    }

    public static void sendLoadFailure(CommandSourceStack src) {
        src.sendFailure(Component.literal("There was an error during audio loading!"));
    }

    public static void loadAudio(UUID player, UUID id, CommandContext<CommandSourceStack> src, Consumer<IRecordedAudio> reaction) {
        try {
            VoiceChatRecording.recordingApi.loadAudio(player, id, reaction).get();
        } catch (InterruptedException e) {
            CommandUtil.sendInterruptFailure(src.getSource());
        } catch (ExecutionException e) {
            CommandUtil.sendLoadFailure(src.getSource());
        }
    }
}

