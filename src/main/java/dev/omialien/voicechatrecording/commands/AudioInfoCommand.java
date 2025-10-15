package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.omialien.voicechatrecording.voicechat.RecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class AudioInfoCommand {
    public static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("audioInfo").
                requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(CommandUtil.PLAYER_ARGUMENT
                        .then(CommandUtil.AUDIO_ARGUMENT.executes(AudioInfoCommand::executeCommand))));
    }

    private static int executeCommand(CommandContext<CommandSourceStack> src) {
        UUID player = UuidArgument.getUuid(src, "player");
        UUID id = UuidArgument.getUuid(src, "audio");
        RecordedAudio res = CommandUtil.loadAudio(player, id, src);
        if (res != null) {
            String info = res.getAudioInfo();
            src.getSource().sendSuccess(() -> Component.literal(info), false);
        } else {
            src.getSource().sendFailure(Component.literal("Audio " + id + " not found"));
        }
        return 0;
    }
}
