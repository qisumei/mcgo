package com.qisumei.csgo.c4.handler;

import com.qisumei.csgo.c4.C4Manager;
import com.qisumei.csgo.c4.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;

/**
 * C4倒计时处理器，用于管理C4炸弹的倒计时逻辑、声音播放和广播提示。
 * 现在由C4Manager统一管理，实现更好的模块化。
 */
public class C4CountdownHandler {
    private final C4Manager c4Manager;
    private BlockPos c4Pos;
    private int ticksLeft;
    private int nextBeepTick;
    private boolean announced10;
    private boolean isActive = false;

    /**
     * 构造一个C4倒计时处理器实例。
     *
     * @param c4Manager C4管理器实例，用于回调C4相关事件
     */
    public C4CountdownHandler(C4Manager c4Manager) {
        this.c4Manager = c4Manager;
    }

    /**
     * 启动C4倒计时。
     *
     * @param pos C4炸弹的位置
     */
    public void start(BlockPos pos) {
        this.c4Pos = pos;
        this.ticksLeft = 40 * 20; // 40秒
        this.nextBeepTick = ticksLeft - calculateNextInterval(ticksLeft);
        this.announced10 = false;
        this.isActive = true;

        // 广播炸弹安放警告
        c4Manager.getMatch().broadcastToAllPlayersInMatch(Component.literal("§c[警报] 炸弹已安放！"));
        playBeepSound();
    }

    /**
     * 停止当前的C4倒计时。
     */
    public void stop() {
        this.isActive = false;
        this.c4Pos = null;
    }

    /**
     * 每tick调用一次，更新倒计时状态并处理相关逻辑。
     */
    public void tick() {
        if (!isActive) return;

        ticksLeft--;

        // 倒计时结束，触发爆炸
        if (ticksLeft <= 0) {
            c4Manager.onC4Exploded();
            stop();
            return;
        }

        // 判断是否需要播放提示音
        if (ticksLeft <= nextBeepTick) {
            playBeepSound();
            nextBeepTick = ticksLeft - calculateNextInterval(ticksLeft);
        }

        // 倒计时剩余10秒时广播警告
        if (!announced10 && ticksLeft <= 10 * 20) {
            c4Manager.getMatch().broadcastToAllPlayersInMatch(Component.literal("§e[警告] 10秒后爆炸！"));
            announced10 = true;
        }
    }

    /**
     * 播放C4提示音效。
     */
    private void playBeepSound() {
        MinecraftServer server = c4Manager.getMatch().getServer();
        if (server == null || c4Pos == null) return;

        server.overworld().playSound(null, c4Pos, ModSounds.ALARM_SOUND(), SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    /**
     * 根据剩余时间计算下一次提示音播放的时间间隔。
     *
     * @param remainingTicks 剩余时间（tick）
     * @return 下一次提示音播放的时间间隔（tick）
     */
    private static int calculateNextInterval(int remainingTicks) {
        float progress = 1.0f - (remainingTicks / (40.0f * 20.0f));
        if (progress > 0.85) return 5;
        if (progress > 0.6) return 10;
        return Math.max(15, (int)(40 * (1 - progress * 0.7f)));
    }
}