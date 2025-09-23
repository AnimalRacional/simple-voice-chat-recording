package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RememberAudiosCommand {
    private static final int PERMS = 2;
    public static boolean shouldRemember = false;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("rememberAudios").requires((cmd) -> cmd.hasPermission(PERMS))
                .executes((src) -> {
                    shouldRemember = !shouldRemember;
                    if (shouldRemember) {
                        src.getSource().sendSuccess(() -> Component.literal("enabled saving; this is for debug only, don't leave it on for too long"), true);
                    } else {
                        src.getSource().sendSuccess(() -> Component.literal("disabled saving"), true);
                    }
                    return 0;
                }));
    }
}
