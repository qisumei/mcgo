package com.qisumei.csgo.c4;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.c4.task.C4DefuseTask;
import com.qisumei.csgo.c4.task.C4TickTask;
import com.qisumei.csgo.game.Match;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * C4管理器，作为C4功能的总调度者。
 * 它持有C4的核心状态，并把具体任务委托给子任务处理器。
 */
public class C4Manager {

    private final Match match;
    private final C4CountdownHandler countdownHandler;
    private final C4DefuseTask defuseTask;
    private final C4TickTask tickTask;

    // C4 核心状态
    private boolean isPlanted = false;
    private BlockPos c4Pos;

    public C4Manager(Match match) {
        this.match = match;
        this.countdownHandler = new C4CountdownHandler(this);
        this.defuseTask = new C4DefuseTask(this);
        this.tickTask = new C4TickTask(this);
    }

    /**
     * 每服务器 tick 调用
     */
    public void tick() {
        countdownHandler.tick();
        tickTask.tick();
    }

    /**
     * 每玩家 tick 调用
     * @param player 当前tick的玩家
     */
    public void handlePlayerTick(ServerPlayer player) {
        tickTask.handlePlayerTick(player);
        defuseTask.handlePlayerDefuseTick(player);
    }

    /**
     * 重置所有C4相关状态。
     */
    public void reset() {
        if (isPlanted && c4Pos != null) {
            match.getServer().overworld().removeBlock(c4Pos, false);
        }
        countdownHandler.stop();
        defuseTask.reset();
        this.isPlanted = false;
        this.c4Pos = null;
    }

    // --- 核心事件处理 ---

    public void onC4Planted(BlockPos pos) {
        this.isPlanted = true;
        this.c4Pos = pos;
        countdownHandler.start(pos);

        String siteName = "包点";
        if(match.getBombsiteA() != null && match.getBombsiteA().contains(pos.getCenter())){
            siteName = "A点";
        } else if(match.getBombsiteB() != null && match.getBombsiteB().contains(pos.getCenter())){
            siteName = "B点";
        }

        Component message = Component.literal("§e[信息] §f炸弹已安放在 §a" + siteName + "！");
        match.broadcastToAllPlayersInMatch(message);
    }

    public void onC4Defused(ServerPlayer defuser) {
        if (match.getRoundState() == Match.RoundState.IN_PROGRESS) {
            countdownHandler.stop();
            defuseTask.reset();
            match.broadcastToAllPlayersInMatch(Component.literal("§b" + (defuser != null ? defuser.getName().getString() : "CT") + " §f已经拆除了炸弹！"));
            match.endRound("CT", "炸弹已被拆除");
        }
    }

    public void onC4Exploded() {
        if (c4Pos != null) {
            applyCustomExplosionDamage(c4Pos);
        }
        match.endRound("T", "炸弹已爆炸");
    }

    public void giveC4ToRandomT() {
        List<ServerPlayer> tPlayers = match.getPlayerStats().entrySet().stream()
            .filter(e -> "T".equals(e.getValue().getTeam()))
            .map(e -> match.getServer().getPlayerList().getPlayer(e.getKey()))
            .filter(Objects::nonNull)
            .toList(); 

        if (!tPlayers.isEmpty()) {
            ServerPlayer playerWithC4 = tPlayers.get(new Random().nextInt(tPlayers.size()));
            playerWithC4.getInventory().add(new ItemStack(QisCSGO.C4_ITEM.get()));
            playerWithC4.sendSystemMessage(Component.literal("§e你携带了C4炸弹！").withStyle(ChatFormatting.BOLD));
        }
    }

    private void applyCustomExplosionDamage(BlockPos c4Pos) {
        final double explosionRadius = 16.0;
        final float maxDamage = 100.0f;

        double explosionX = c4Pos.getX() + 0.5;
        double explosionY = c4Pos.getY() + 0.5;
        double explosionZ = c4Pos.getZ() + 0.5;

        DamageSource damageSource = match.getServer().overworld().damageSources().genericKill();

        for (UUID playerUUID : new ArrayList<>(match.getAlivePlayers())) {
            ServerPlayer player = match.getServer().getPlayerList().getPlayer(playerUUID);
            if (player == null || !player.isAlive()) continue;

            double distanceSq = player.distanceToSqr(explosionX, explosionY, explosionZ);

            if (distanceSq < explosionRadius * explosionRadius) {
                double distance = Math.sqrt(distanceSq);
                float damageFalloff = (float) (1.0 - distance / explosionRadius);
                float damageToApply = maxDamage * damageFalloff;

                if (damageToApply > 0) {
                    player.hurt(damageSource, damageToApply);
                }
            }
        }
    }

    // --- Getters and Setters ---

    public boolean isC4Planted() {
        return this.isPlanted;
    }

    public BlockPos getC4Pos() {
        return this.c4Pos;
    }
    
    public Match getMatch() {
        return this.match;
    }
    
    public int getC4TicksLeft() {
        return countdownHandler.getTicksLeft();
    }
}