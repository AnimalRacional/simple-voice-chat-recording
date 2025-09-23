package dev.omialien.voicechatrecording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.RecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class AudioInfoCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("audioInfo").
                requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(Commands.argument("audio", UuidArgument.uuid()).suggests((src, suggestionsBuilder) ->
                        SharedSuggestionProvider.suggest(
                                VoiceChatRecording.storedAudios.stream().map((r) -> r.getId().toString()), suggestionsBuilder))
                        .executes((src) -> {
                    UUID id = UuidArgument.getUuid(src, "audio");
                    RecordedAudio res = (RecordedAudio)VoiceChatRecording.storedAudios.stream().filter(cur -> cur.getId().equals(id)).findFirst().orElse(null);
                    if(res != null){
                        src.getSource().sendSuccess(() -> Component.literal(res.getAudioInfo()), false);
                    } else {
                        src.getSource().sendFailure(Component.literal("Audio " + id + " not found"));
                    }
                    return 0;
                })));
    }
}
