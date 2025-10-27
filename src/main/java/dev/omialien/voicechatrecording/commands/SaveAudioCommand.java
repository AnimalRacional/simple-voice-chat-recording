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

public class SaveAudioCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("saveAudio").requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(
                        Commands.argument("player",
                                UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                                SharedSuggestionProvider.suggest(
                                        CommandUtil.getRememberedAudios().map(r -> r.getPlayerUUID().toString()), suggestionsBuilder))
                                .then(
                                        Commands.argument("audio",
                                                        UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                CommandUtil.getRememberedAudios().map(r -> r.getId().toString()), suggestionsBuilder))
                                        .executes(SaveAudioCommand::runCommand))));
    }

    private static int runCommand(CommandContext<CommandSourceStack> src) {
        UUID player = UuidArgument.getUuid(src, "player");
        UUID audioId = UuidArgument.getUuid(src, "audio");
        for(IRecordedAudio audio : VoiceChatRecording.storedAudios) {
            if(audio.getPlayerUUID().equals(player) && audio.getId().equals(audioId)) {
                boolean res = audio.saveAudio(VoiceChatRecording.MOD_ID);
                if(res) {
                    StringBuilder builder = new StringBuilder("Saved audio ");
                    builder.append(audioId);
                    builder.append(" by ");
                    Player p = src.getSource().getLevel().getPlayerByUUID(player);
                    builder.append(p == null ? player : p.getName().getString());
                    src.getSource().sendSuccess(() -> Component.literal(builder.toString()), true);
                } else {
                    src.getSource().sendFailure(Component.literal("Could not save audio!"));
                    if(VoiceChatRecording.recordingApi.getPrivacy(player)) {
                        src.getSource().sendFailure(Component.literal("Player has privacy mode enabled"));
                    } else {
                        src.getSource().sendFailure(Component.literal("Unknown reason"));
                    }
                }
                return 0;
            }
        }
        src.getSource().sendFailure(Component.literal("Audio not found"));
        return 1;
    }
}
