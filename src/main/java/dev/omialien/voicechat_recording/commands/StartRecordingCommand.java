package dev.omialien.voicechat_recording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechat_recording.voicechat.RevervoxVoicechatPlugin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StartRecordingCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startRecording").requires((cmdSrc) -> cmdSrc.hasPermission(PERMISSION_LEVEL)).executes((cmdSrc) -> {
            Player player = cmdSrc.getSource().getPlayerOrException();

            RevervoxVoicechatPlugin.startRecording(player.getUUID());

            cmdSrc.getSource().sendSuccess(() -> Component.literal("Started Recording for " + player.getGameProfile().getName() + "..."), false);

            return 1;
        }));
    }
}
