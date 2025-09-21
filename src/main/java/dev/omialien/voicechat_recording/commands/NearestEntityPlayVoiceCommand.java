package dev.omialien.voicechat_recording.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording_api.IRecordedAudio;
import dev.omialien.voicechat_recording_api.AudioEffect;
import dev.omialien.voicechat_recording.voicechat.util.AudioPlayingUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class NearestEntityPlayVoiceCommand {
    public static final int PERMISSION_LEVEL = 2;
    private static final float CHANNEL_DISTANCE = 20;
    public static final int bbX = 5;
    public static final int bbY = 5;
    public static final int bbZ = 5;

    private static LiteralArgumentBuilder<CommandSourceStack> PITCH_ARG(Command<CommandSourceStack> cmd){
        return Commands.literal("pitch").then(Commands.argument("pitchFactor", FloatArgumentType.floatArg()).executes(cmd));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> REVERB_ARG(Command<CommandSourceStack> cmd){
        return Commands.literal("reverb").then(
                Commands.argument("decay", FloatArgumentType.floatArg()).then(
                        Commands.argument("delay-ms", IntegerArgumentType.integer()).then(
                                Commands.argument("repeats", IntegerArgumentType.integer()).executes(cmd)
                        ))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ROBOT_ARG(Command<CommandSourceStack> cmd){
        return Commands.literal("robot").then(
                Commands.argument("lfo-frequency", FloatArgumentType.floatArg()).executes(cmd)
        );
    }
    // TODO adicionar argumento de localização em vez de entidade
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("playVoice").requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                .then(Commands.argument("uuid", UuidArgument.uuid())
                        .suggests((src, suggestionsBuilder) ->
                            SharedSuggestionProvider.suggest(
                                    VoiceChatRecording.storedAudios.stream().map((r) -> r.getId().toString()), suggestionsBuilder))
                        .executes((ctx) ->{
                            VoiceChatRecording.LOGGER.debug("id");
                            return runCommand(ctx, null,
                                    UuidArgument.getUuid(ctx, "uuid"), null);
                        })
                        .then(Commands.argument("entity", EntityArgument.entities())
                                .executes((ctx) ->{
                                    VoiceChatRecording.LOGGER.debug("ent id");
                                    return runCommand(
                                        ctx, EntityArgument.getEntities(ctx, "entity"),
                                        UuidArgument.getUuid(ctx, "uuid"), null);
                                })
                                .then(PITCH_ARG((ctx) -> {
                                    VoiceChatRecording.LOGGER.debug("ent id pitch");
                                    return runCommand(
                                            ctx, EntityArgument.getEntities(ctx, "entity"),
                                            UuidArgument.getUuid(ctx, "uuid"),
                                            AudioEffect.pitch(FloatArgumentType.getFloat(ctx, "pitchFactor"))
                                    );
                                })).then(REVERB_ARG((ctx) -> {
                                    VoiceChatRecording.LOGGER.debug("ent id reverb");
                                    float decay = FloatArgumentType.getFloat(ctx, "decay");
                                    int delay = IntegerArgumentType.getInteger(ctx, "delay-ms");
                                    int repeats = IntegerArgumentType.getInteger(ctx, "repeats");
                                    return runCommand(
                                            ctx, EntityArgument.getEntities(ctx, "entity"),
                                            UuidArgument.getUuid(ctx, "uuid"),
                                            AudioEffect.reverb(decay, delay, repeats)
                                    );
                                        })
                                ).then(ROBOT_ARG((ctx) -> {
                                    VoiceChatRecording.LOGGER.debug("ent id robot");
                                    return runCommand(
                                            ctx, EntityArgument.getEntities(ctx, "entity"),
                                            UuidArgument.getUuid(ctx, "uuid"),
                                            AudioEffect.robot(FloatArgumentType.getFloat(ctx, "lfo-frequency"))
                                    );
                                }))
                                .then(Commands.literal("random").executes((ctx) -> {
                                    VoiceChatRecording.LOGGER.debug("ent id random");
                                    return runCommand(
                                            ctx, EntityArgument.getEntities(ctx, "entity"),
                                            UuidArgument.getUuid(ctx, "uuid"),
                                            AudioEffect.random()
                                    );
                                })))));
    }

    private static LivingEntity getNearestEntity(CommandContext<CommandSourceStack> ctx){
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 srcPos = ctx.getSource().getPosition();
        AABB aabb = new AABB(srcPos.x + bbX, srcPos.y + bbY, srcPos.z + bbZ,
                srcPos.x - bbX, srcPos.y - bbY, srcPos.z - bbZ);
        return level.getNearestEntity(LivingEntity.class, TargetingConditions.DEFAULT,
                null, srcPos.x, srcPos.y, srcPos.z, aabb);
    }

    private static void playAudio(CommandContext<CommandSourceStack> ctx,
                                  Entity entity, UUID id,
                                  AudioEffect effects){
        VoiceChatRecording.LOGGER.debug("Entity: " + entity.getName());
        IRecordedAudio audio = null;
        for(IRecordedAudio cur : VoiceChatRecording.storedAudios){
            if(cur.getId().equals(id)){
                audio = cur;
                break;
            }
        }
        if(audio != null){
            Player player = entity.level().getPlayerByUUID(audio.getPlayerUUID());
            String playerName = player == null ? audio.getPlayerUUID().toString() : player.getName().getString();
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Playing audio of " + playerName + " from ").append(entity.getName()), true);
            AudioPlayingUtil.playFromEntity(audio, entity, effects, VoiceChatRecording.CATEGORY_ID, CHANNEL_DISTANCE);
        } else {
            ctx.getSource().sendFailure(Component.literal("Invalid ID " + id));
        }
    }

    public static int runCommand(CommandContext<CommandSourceStack> ctx,
                                 @Nullable Collection<? extends Entity> targets,
                                 UUID id, AudioEffect effects){
        try{
            Collection<Entity> entities = targets == null ? null : targets.stream().map((e) -> (Entity)e).toList();
            if(entities == null){
                // If no entities are specified, use the nearest entity
                LivingEntity nearestEntity = getNearestEntity(ctx);
                if(nearestEntity == null){
                    ctx.getSource().sendFailure(Component.literal("No entity found!"));
                    return 20;
                }
                entities = new ArrayList<>(); entities.add(nearestEntity);
            }
            for(Entity audioTarget : entities){
                playAudio(ctx, audioTarget, id, effects);
            }
            return 0;
        } catch(Exception e){
            VoiceChatRecording.LOGGER.error("Error running playVoice: {}\r\n{}", e.getMessage(), e.getStackTrace());
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 100;
        }
    }
}
