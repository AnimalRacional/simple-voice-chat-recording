package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StopRecordingCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("saveRecording").requires((cmdSrc) -> cmdSrc.hasPermission(PERMISSION_LEVEL)).executes((cmdSrc) -> {

            Player player = cmdSrc.getSource().getPlayerOrException();

            ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).stopRecording(player.getUUID());

            cmdSrc.getSource().sendSuccess(() -> Component.literal("Saved current recording for " + player.getGameProfile().getName() + "..."), false);

            return 1;
        }));
    }
}
