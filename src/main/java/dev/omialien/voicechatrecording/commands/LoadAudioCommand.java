package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.omialien.voicechatrecording.voicechat.RecordedAudio;
import dev.omialien.voicechatrecording_api.IRecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class LoadAudioCommand {
    public static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loadAudio").
                requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(CommandUtil.PLAYER_ARGUMENT
                        .then(CommandUtil.AUDIO_ARGUMENT.executes(LoadAudioCommand::executeCommand))));
    }

    private static int executeCommand(CommandContext<CommandSourceStack> src) {
        UUID player = UuidArgument.getUuid(src, "player");
        UUID id = UuidArgument.getUuid(src, "audio");
        src.getSource().sendSuccess(() -> Component.literal("Loading audio..."), false);
        CommandUtil.loadAudio(player, id, src, (IRecordedAudio res) -> {
            if(res != null) {
                src.getSource().sendSuccess(() -> Component.literal("Loading audio with " + res.getDuration() + " seconds"), true);
                String info = ((RecordedAudio)res).getAudioInfo();
                src.getSource().sendSuccess(() -> Component.literal(info), false);
            } else {
                src.getSource().sendFailure(Component.literal("Audio " + id + " not found"));
            }
        });
        return 0;
    }
}
