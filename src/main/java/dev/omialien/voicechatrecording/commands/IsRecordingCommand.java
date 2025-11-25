package dev.omialien.voicechatrecording.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.api.IRecordedPlayer;
import dev.omialien.voicechatrecording.voicechat.RecordedPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;

public class IsRecordingCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("isRecording").requires((cmdSrc) -> cmdSrc.hasPermission(PERMISSION_LEVEL)).then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((cmdSrc, suggestionsBuilder) -> {

            PlayerList playerlist = cmdSrc.getSource().getServer().getPlayerList();

            return SharedSuggestionProvider.suggest(playerlist.getPlayers().stream().map((player) -> player.getGameProfile().getName()), suggestionsBuilder);

        }).executes((cmdSrc) -> {

            StringBuilder sb = new StringBuilder();
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(cmdSrc, "targets");

            for (GameProfile target : targets) {
                IRecordedPlayer p = (VoiceChatRecording.recordingApi).getRecordedPlayer(target.getId());
                String state;
                if ( p == null ) {
                    state = "Not Connected";
                } else {
                    state = String.valueOf(p.isRecording());
                }
                sb.append(target.getName()).append(": ").append(state);
                if (targets.size() != 1) sb.append("\n");
            }

            VoiceChatRecording.LOGGER.debug(sb.toString());

            cmdSrc.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        })));
    }
}
