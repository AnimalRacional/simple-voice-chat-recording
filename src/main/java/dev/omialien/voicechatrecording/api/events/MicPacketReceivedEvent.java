package dev.omialien.voicechatrecording.api.events;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

public class MicPacketReceivedEvent extends Event {
    private final MicrophonePacketEvent inner;

    public MicPacketReceivedEvent(MicrophonePacketEvent inner){
        this.inner = inner;
    }

    /**
     * Gets the received Simple Voice Chat API packet
     * @return the received packet
     */
    public MicrophonePacket getPacket(){
        return inner.getPacket();
    }

    /**
     * Get the player that sent the packet
     * @return the player
     */
    @Nullable
    public ServerPlayer getPlayer(){
        if(inner.getSenderConnection() == null){ return null; }
        return (ServerPlayer)inner.getSenderConnection().getPlayer().getPlayer();
    }

    /**
     * Gets the voice chat api event that triggered this event
     * @return the Simple Voice Chat Api event
     */
    public MicrophonePacketEvent getVoiceChatApiEvent(){
        return this.inner;
    }
}
