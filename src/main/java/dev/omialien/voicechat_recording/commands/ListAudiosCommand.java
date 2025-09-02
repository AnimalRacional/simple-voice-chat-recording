package dev.omialien.voicechat_recording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ListAudiosCommand {
    private static final int PERMS = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("listAudios").requires((cmd) -> cmd.hasPermission(PERMS))
                .executes((src) -> {
                    StringBuilder builder = new StringBuilder("Stored audios:\n");
                    for(RecordedAudio audio : VoiceChatRecording.storedAudios){
                        builder.append(audio.getId()).append(" by ");
                        Player p = src.getSource().getLevel().getPlayerByUUID(audio.getPlayerUUID());
                        builder.append(p == null ? audio.getPlayerUUID() : p.getName().getString()).append("\n");
                    }
                    src.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                    return 0;
                }));
    }
}
