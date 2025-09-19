package dev.omialien.voicechat_recording.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.IRecordedAudio;
import dev.omialien.voicechat_recording.voicechat.RecordedAudio;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SaveAudioCommand {
    public static final int PERMISSION_LEVEL = 2;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("saveAudio").
                requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(Commands.argument("audio", UuidArgument.uuid())
                        .suggests((src, suggestionsBuilder) ->
                                SharedSuggestionProvider.suggest(
                                        VoiceChatRecording.storedAudios.stream().map((r) -> r.getId().toString()), suggestionsBuilder))
                        .executes((src) -> {
            UUID id = UuidArgument.getUuid(src, "audio");
            RecordedAudio res = null;
            for(IRecordedAudio cur : VoiceChatRecording.storedAudios){
                if(cur.getId().equals(id)){
                    res = (RecordedAudio) cur;
                    break;
                }
            }
            if(res != null){
                if(res.wasSaved()){
                    src.getSource().sendFailure(Component.literal("Audio already saved"));
                } else {
                    res.saveAudio(VoiceChatRecording.MOD_ID);
                    src.getSource().sendSuccess(() -> Component.literal("Audio " + id + " saved"), true);
                }
            } else {
                src.getSource().sendFailure(Component.literal("Audio " + id + " not found"));
            }
            return 0;
        })));
    }
}
