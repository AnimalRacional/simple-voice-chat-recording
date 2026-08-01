package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechatrecording.api.AudioId;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.VoiceChatRecordingPlugin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ListAudiosCommand {
    private static final int PERMS = 2;
    // TODO for all commands, get audios not just from VoiceChatRecording.storedAudios, but from the saved and loaded audios cache
    //  Also add a command for listing saved audios, and another to load them, which will result in them being added to the cache
    //  Maybe only the ones in the API's namespace
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("listAudios").requires((cmd) -> cmd.hasPermission(PERMS))
                .executes((src) -> {
                    for(String namespace : ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).savedAudios.keySet()) {
                        src.getSource().sendSuccess(() -> Component.literal("§aAudios saved by " + namespace + ":"), false);
                        for(AudioId audio : ((VoiceChatRecordingPlugin)VoiceChatRecording.recordingApi).savedAudios.get(namespace)){
                            StringBuilder builder = new StringBuilder();
                            builder.append(audio.audio()).append(" by ");
                            Player p = src.getSource().getLevel().getPlayerByUUID(audio.player());
                            builder.append(p == null ? audio.player() : p.getName().getString());
                            src.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                        }
                    }
                    return 0;
                }));
    }
}
