package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class GetPrivacyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("getPrivacy").executes((s) -> {
                    Player sender = s.getSource().getPlayer();
                    if ( sender != null ) {
                        boolean privacy = VoiceChatRecording.recordingApi.getPrivacy(sender.getUUID());
                        if ( privacy ) {
                            s.getSource().sendSuccess(() -> Component.literal("Privacy mode is enabled").withStyle(ChatFormatting.GREEN), false);
                        } else {
                            s.getSource().sendSuccess(() -> Component.literal("Privacy mode is disabled").withStyle(ChatFormatting.GREEN), false);
                        }
                    } else {
                        s.getSource().sendFailure(Component.literal("Only players may have privacy"));
                    }
                    return 0;
                })
        );
    }
}
