package dev.omialien.voicechatrecording.commands;

import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StopRecordingCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stopRecording").requires((cmdSrc) -> cmdSrc.hasPermission(PERMISSION_LEVEL)).executes((cmdSrc) -> {

            Player player = cmdSrc.getSource().getPlayerOrException();

            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).stopRecording(player.getUUID());

            cmdSrc.getSource().sendSuccess(() -> Component.literal("Stopped Recording for " + player.getGameProfile().getName() + "..."), false);

            return 1;
        }));
    }
}
