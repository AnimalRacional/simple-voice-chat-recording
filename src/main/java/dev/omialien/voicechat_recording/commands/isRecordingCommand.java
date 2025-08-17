package dev.omialien.voicechat_recording.commands;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;

public class isRecordingCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("isRecording").requires((cmdSrc) -> cmdSrc.hasPermission(PERMISSION_LEVEL)).then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((cmdSrc, suggestionsBuilder) -> {

            PlayerList playerlist = cmdSrc.getSource().getServer().getPlayerList();

            return SharedSuggestionProvider.suggest(playerlist.getPlayers().stream().map((player) -> player.getGameProfile().getName()), suggestionsBuilder);

        }).executes((cmdSrc) -> {

            StringBuilder sb = new StringBuilder();
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(cmdSrc, "targets");

            for (GameProfile target : targets) {
                sb.append(target.getName()).append(": ").append(VoiceChatRecordingPlugin.getRecordedPlayer(target.getId()).isRecording());
                if (targets.size() != 1) sb.append("\n");
            }

            VoiceChatRecording.LOGGER.debug(sb.toString());

            cmdSrc.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        })));
    }
}
