package dev.omialien.voicechat_recording.commands;

import dev.omialien.voicechat_recording.VoiceChatRecording;
import dev.omialien.voicechat_recording.voicechat.VoiceChatRecordingPlugin;
import dev.omialien.voicechat_recording.voicechat.audio.AudioPlayer;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class NearestEntityPlayVoiceCommand {
    public static final int PERMISSION_LEVEL = 2;
    private static final int CHANNEL_DISTANCE = 20;
    public static final int bbX = 5;
    public static final int bbY = 5;
    public static final int bbZ = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, Boolean> removeArgument =
                Commands.argument("remove", BoolArgumentType.bool());
        dispatcher.register(
                Commands.literal("playVoice").requires((src) -> src.hasPermission(PERMISSION_LEVEL))
                    .then(
                        Commands.argument("players", GameProfileArgument.gameProfile())
                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                        .executes((ctx) ->{
                                            VoiceChatRecording.LOGGER.debug("pl ind");
                                            return runCommand(ctx, null,
                                                    GameProfileArgument.getGameProfiles(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "index"),
                                                    false);
                                        })
                                        .then(removeArgument
                                                .executes((ctx) ->{
                                                    VoiceChatRecording.LOGGER.debug("pl ind rem");
                                                    try{
                                                        return runCommand(
                                                                ctx, null,
                                                                GameProfileArgument.getGameProfiles(ctx, "players"),
                                                                IntegerArgumentType.getInteger(ctx, "index"),
                                                                BoolArgumentType.getBool(ctx, "remove")
                                                        );
                                                    } catch(Exception e){
                                                        VoiceChatRecording.LOGGER.error("error command {}\n{}", e.getMessage(), e.getStackTrace());
                                                    }
                                                    return 999;
                                                }))
                                        .then(Commands.argument("entity", EntityArgument.entities())
                                                .executes((ctx) ->{
                                                    VoiceChatRecording.LOGGER.debug("ent plr ind");
                                                    return runCommand(
                                                        ctx, EntityArgument.getEntities(ctx, "entity"),
                                                        GameProfileArgument.getGameProfiles(ctx, "players"),
                                                        IntegerArgumentType.getInteger(ctx, "index"),
                                                        false);
                                                })
                                                .then(removeArgument.executes((ctx) -> {
                                                    VoiceChatRecording.LOGGER.debug("ent pl ind rem");
                                                    return runCommand(
                                                        ctx, EntityArgument.getEntities(ctx, "entity"),
                                                        GameProfileArgument.getGameProfiles(ctx, "players"),
                                                        IntegerArgumentType.getInteger(ctx, "index"),
                                                        BoolArgumentType.getBool(ctx, "remove")
                                                    );
                                                })))
                ))
        );
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
                                  Entity entity, Collection<GameProfile> players, int index, boolean remove,
                                  VoicechatServerApi api){
        VoiceChatRecording.LOGGER.debug("Entity: " + entity.getName());
        for (GameProfile player : players) {
            UUID channelID = UUID.randomUUID();
            EntityAudioChannel channel = createChannel(api, channelID, VoiceChatRecording.CATEGORY_ID, entity);
            VoiceChatRecording.LOGGER.debug("Created new channel: " + channel);
            short[] audio = VoiceChatRecordingPlugin.getAudio(player.getId(), index, remove);
            if(audio != null){
                ctx.getSource().sendSuccess(() ->
                        Component.literal("Playing audio from " + player.getName() + " index " + index + " from " + entity.getName()), true);
                new AudioPlayer(audio, api, channel).start();
            } else {
                ctx.getSource().sendFailure(Component.literal("Invalid index " + index + " for player " + player.getName()));
            }
        }
    }

    public static int runCommand(CommandContext<CommandSourceStack> ctx,
                                 @Nullable Collection<? extends Entity> targets,
                                 @NotNull Collection<GameProfile> players, int index, boolean remove){
        try{
            if(VoiceChatRecording.vcApi instanceof VoicechatServerApi api){
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
                    // TODO quando remove é true, tocar em várias entidades vai remover vários audios
                    playAudio(ctx, audioTarget, players, index, remove, api);
                }
                return 0;
            }
            return 50;
        } catch(Exception e){
            VoiceChatRecording.LOGGER.error("Error running playVoice: {}\r\n{}", e.getMessage(), e.getStackTrace());
            return 100;
        }
    }

    private static EntityAudioChannel createChannel(VoicechatServerApi api, UUID channelID, String category, Entity nearestEntity) {
        EntityAudioChannel channel = api.createEntityAudioChannel(channelID, api.fromEntity(nearestEntity));
        if (channel == null) {
            VoiceChatRecording.LOGGER.error("Couldn't create channel");
            return null;
        }
        channel.setCategory(category); // The category of the audio channel
        channel.setDistance(NearestEntityPlayVoiceCommand.CHANNEL_DISTANCE); // The distance in which the audio channel can be heard
        return channel;
    }
}
