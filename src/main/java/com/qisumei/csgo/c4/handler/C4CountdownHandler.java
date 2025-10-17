package com.qisumei.csgo.c4.handler;

import com.qisumei.csgo.c4.sound.ModSounds;
import com.qisumei.csgo.game.Match;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;

public class C4CountdownHandler {
    private final Match match;
    private BlockPos c4Pos;
    private int ticksLeft;
    private int nextBeepTick;
    private boolean announced10;
    private boolean isActive = false;

    public C4CountdownHandler(Match match) {
        this.match = match;
    }

    public void start(BlockPos pos) {
        this.c4Pos = pos;
        this.ticksLeft = 40 * 20; // 40秒
        this.nextBeepTick = ticksLeft - calculateNextInterval(ticksLeft);
        this.announced10 = false;
        this.isActive = true;

        // --- 修正 #1: 调用 broadcastToAllPlayersInMatch 时不再需要 server 参数 ---
        match.broadcastToAllPlayersInMatch(Component.literal("§c[警报] 炸弹已安放！"));
        playBeepSound();
    }

    public void stop() {
        this.isActive = false;
        this.c4Pos = null;
    }

    public void tick() {
        if (!isActive) return;

        ticksLeft--;

        if (ticksLeft <= 0) {
            match.onC4Exploded();
            stop();
            return;
        }

        if (ticksLeft <= nextBeepTick) {
            playBeepSound();
            nextBeepTick = ticksLeft - calculateNextInterval(ticksLeft);
        }

        if (!announced10 && ticksLeft <= 10 * 20) {
            // --- 修正 #2: 调用 broadcastToAllPlayersInMatch 时不再需要 server 参数 ---
            match.broadcastToAllPlayersInMatch(Component.literal("§e[警告] 10秒后爆炸！"));
            announced10 = true;
        }
    }
    
    private void playBeepSound() {
        MinecraftServer server = match.getServer();
        if (server == null || c4Pos == null) return;
        
        server.overworld().playSound(null, c4Pos, ModSounds.ALARM_SOUND(), SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    private static int calculateNextInterval(int remainingTicks) {
        float progress = 1.0f - (remainingTicks / (40.0f * 20.0f));
        if (progress > 0.85) return 5;
        if (progress > 0.6) return 10;
        return Math.max(15, (int)(40 * (1 - progress * 0.7f)));
    }

    public boolean isActive() {
        return this.isActive;
    }
}