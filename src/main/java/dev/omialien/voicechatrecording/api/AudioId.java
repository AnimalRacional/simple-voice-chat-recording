package dev.omialien.voicechatrecording.api;

import java.util.UUID;

public record AudioId(UUID first, UUID second) {
    public static AudioId of(UUID player, UUID audio) { return new AudioId(player, audio); }
    public UUID player() { return this.first; }
    public UUID audio() { return this.second; }
}
