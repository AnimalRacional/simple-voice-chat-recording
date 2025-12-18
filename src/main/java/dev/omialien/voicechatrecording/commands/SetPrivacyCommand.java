package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SetPrivacyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO currently, admins can change privacy mode of other players through '/execute as player run setPrivacy false'
        dispatcher.register(
                Commands.literal("setPrivacy").then(Commands.argument("privacymode", BoolArgumentType.bool()).executes((s) -> {
                    Player sender = s.getSource().getPlayer();
                    if ( sender != null ) {
                        boolean privacy = BoolArgumentType.getBool(s, "privacymode");
                        VoiceChatRecording.LOGGER.debug("Setting privacy mode for {} to {}", sender.getName(), privacy);
                        ((VoiceChatRecordingPlugin) VoiceChatRecording.recordingApi).setPrivacy(sender.getUUID(), privacy);
                        if ( privacy ) {
                            s.getSource().sendSuccess(() -> Component.literal("Privacy mode enabled").withStyle(ChatFormatting.GREEN), false);
                        } else {
                            s.getSource().sendSuccess(() -> Component.literal("Privacy mode disabled").withStyle(ChatFormatting.GREEN), false);
                        }
                    } else {
                        s.getSource().sendFailure(Component.literal("Only players may have privacy"));
                    }
                    return 0;
                }))
        );
    }
}
