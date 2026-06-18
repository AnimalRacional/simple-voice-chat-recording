package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class UnsaveAudioCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unsaveAudio").requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(
                        Commands.argument("player",
                                        UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                                        SharedSuggestionProvider.suggest(
                                                CommandUtil.getSavedAudios(VoiceChatRecording.MOD_ID).map(r -> r.getFirst().toString()), suggestionsBuilder))
                                .then(
                                        Commands.argument("audio",
                                                        UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                CommandUtil.getSavedAudios(VoiceChatRecording.MOD_ID).map(r -> r.getSecond().toString()), suggestionsBuilder))
                                                .executes(UnsaveAudioCommand::runCommand))));
    }

    private static int runCommand(CommandContext<CommandSourceStack> src) {
        UUID player = UuidArgument.getUuid(src, "player");
        UUID audioId = UuidArgument.getUuid(src, "audio");
        VoiceChatRecording.recordingApi.unsaveAudio(VoiceChatRecording.MOD_ID, player, audioId);
        src.getSource().sendSuccess(() -> Component.literal("Unsaved audio " + player + " " + audioId), true);
        return 1;
    }
}
