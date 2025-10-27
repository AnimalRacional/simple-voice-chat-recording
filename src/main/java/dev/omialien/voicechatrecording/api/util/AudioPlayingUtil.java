package dev.omialien.voicechatrecording.api.util;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.omialien.voicechatrecording.api.AudioEffect;
import dev.omialien.voicechatrecording.api.IRecordedAudio;
import dev.omialien.voicechatrecording.VoiceChatRecording;
import dev.omialien.voicechatrecording.voicechat.audio.AudioPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class AudioPlayingUtil {
    public static void playLocationalAudio(IRecordedAudio audio, Vec3 position, ServerLevel level, String category){
        playLocationalAudio(audio, position, level, null, category, 32);
    }
    public static void playLocationalAudio(IRecordedAudio audio, Vec3 position, ServerLevel level, AudioEffect effects, String category, float distance){
        if(audio == null){ return; }
        LocationalAudioChannel chan = VoiceChatRecording.vcApi.createLocationalAudioChannel(
            UUID.randomUUID(),
            VoiceChatRecording.vcApi.fromServerLevel(level),
            VoiceChatRecording.vcApi.createPosition(position.x, position.y, position.z)
        );
        if(chan == null){
            VoiceChatRecording.LOGGER.error("Couldn't create audio channel for {}", position);
            return;
        }
        chan.setDistance(distance);
        chan.setCategory(category);
        play(audio.applyEffects(effects), chan, VoiceChatRecording.vcApi);
    }

    public static void playFromEntity(IRecordedAudio audio, Entity entity, String category){
        playFromEntity(audio, entity, null, category, 32);
    }

    public static void playFromEntity(IRecordedAudio audio, Entity entity, AudioEffect effects, String category, float distance){
        if(audio == null){ return; }
        VoicechatServerApi api = VoiceChatRecording.vcApi;
        EntityAudioChannel chan = api.createEntityAudioChannel(
                UUID.randomUUID(),
                api.fromEntity(entity)
        );
        if(chan == null){
            VoiceChatRecording.LOGGER.error("Couldn't create audio channel for {}", entity.getName());
            return;
        }
        chan.setCategory(category);
        chan.setDistance(distance);
        play(audio.applyEffects(effects), chan, api);
    }

    private static void play(short[] audio, AudioChannel chan, VoicechatServerApi api){
        new AudioPlayer(audio, api, chan).start();
    }
}
