package dev.omialien.voicechat_recording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording_api.IRecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ListAudiosCommand {
    private static final int PERMS = 2;
    // TODO for all commands, get audios not just from VoiceChatRecording.storedAudios, but from the saved and loaded audios cache
    // Also add a command for listing saved audios, and another to load them, which will result in them being added to the cache
    // Maybe only the ones in the API's namespace
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("listAudios").requires((cmd) -> cmd.hasPermission(PERMS))
                .executes((src) -> {
                    StringBuilder builder = new StringBuilder("Stored audios:\n");
                    for(IRecordedAudio audio : VoiceChatRecording.storedAudios){
                        builder.append(audio.getId()).append(" by ");
                        Player p = src.getSource().getLevel().getPlayerByUUID(audio.getPlayerUUID());
                        builder.append(p == null ? audio.getPlayerUUID() : p.getName().getString()).append("\n");
                    }
                    src.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                    return 0;
                }));
    }
}
