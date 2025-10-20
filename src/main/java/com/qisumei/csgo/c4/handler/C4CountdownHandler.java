package com.qisumei.csgo.c4.handler;

import com.qisumei.csgo.c4.sound.ModSounds;
import com.qisumei.csgo.game.Match;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * C4 倒计时处理器，负责管理C4炸弹安放后的计时、音效和事件触发。
 * <p>
 * 这个类作为一个状态机，由 {@link Match#tick()} 方法驱动。它在C4安放后被激活，
 * 并在倒计时结束时调用 {@link Match#onC4Exploded()}。
 * </p>
 *
 * @author Qisumei
 */
public class C4CountdownHandler {

    private static final int C4_FUSE_TICKS = 40 * 20; // C4总倒计时长 (40秒)
    private static final int TEN_SECOND_WARNING_TICKS = 10 * 20; // 10秒警告时间点

    private final Match match;
    private BlockPos c4Pos;
    private int ticksLeft;
    private int nextBeepTick;
    private boolean hasAnnounced10Seconds;
    private boolean isActive;

    /**
     * 构造一个新的 C4CountdownHandler 实例。
     *
     * @param match 当前倒计时处理器所属的比赛实例。
     */
    public C4CountdownHandler(Match match) {
        this.match = match;
        this.isActive = false;
    }

    /**
     * 启动C4倒计时。
     *
     * @param pos C4炸弹被安放的位置。
     */
    public void start(BlockPos pos) {
        this.c4Pos = pos;
        this.ticksLeft = C4_FUSE_TICKS;
        this.nextBeepTick = this.ticksLeft - calculateNextBeepInterval(this.ticksLeft);
        this.hasAnnounced10Seconds = false;
        this.isActive = true;

        match.broadcastToAllPlayersInMatch(Component.literal("§c[警报] 炸弹已安放！"));
        playBeepSound();
    }

    /**
     * 停止当前的C4倒计时。
     * 通常在回合结束（例如C4被拆除）时调用。
     */
    public void stop() {
        this.isActive = false;
        this.c4Pos = null;
    }

    /**
     * 每tick调用一次，更新倒计时状态并处理相关逻辑。
     * 这是倒计时处理器的核心驱动方法。
     */
    public void tick() {
        if (!isActive) return;

        ticksLeft--;

        // 倒计时结束，触发爆炸
        if (ticksLeft <= 0) {
            match.onC4Exploded();
            stop(); // 爆炸后停止计时器
            return;
        }

        // 检查是否到达下一次播放提示音的时间点
        if (ticksLeft <= nextBeepTick) {
            playBeepSound();
            // 计算再下一次的提示音时间点
            this.nextBeepTick = this.ticksLeft - calculateNextBeepInterval(this.ticksLeft);
        }

        // 在倒计时剩余10秒时广播警告
        if (!hasAnnounced10Seconds && ticksLeft <= TEN_SECOND_WARNING_TICKS) {
            match.broadcastToAllPlayersInMatch(Component.literal("§e[警告] 10秒后爆炸！"));
            this.hasAnnounced10Seconds = true;
        }
    }

    /**
     * 在C4位置播放提示音效。
     */
    private void playBeepSound() {
        MinecraftServer server = match.getServer();
        if (server == null || c4Pos == null) return;

        // 在主世界播放声音
        Level world = server.overworld();
        world.playSound(null, c4Pos, ModSounds.ALARM_SOUND, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    /**
     * 根据剩余时间动态计算下一次提示音的时间间隔。
     * 倒计时越接近结束，提示音播放得越频繁。
     *
     * @param remainingTicks 剩余的倒计时 tick 数。
     * @return 下一次提示音之前需要等待的 tick 数。
     */
    private static int calculateNextBeepInterval(int remainingTicks) {
        // 计算倒计时进度 (0.0 -> 1.0)
        float progress = 1.0f - (remainingTicks / (float) C4_FUSE_TICKS);
        if (progress > 0.85) return 5;  // 最后阶段，每 0.25 秒响一次
        if (progress > 0.6) return 10; // 中后阶段，每 0.5 秒响一次
        // 早期阶段，间隔从 40 tick (2秒) 逐渐缩短到 15 tick (0.75秒)
        return Math.max(15, (int) (40 * (1 - progress * 0.7f)));
    }
}
