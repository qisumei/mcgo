package com.qisumei.csgo.c4;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.c4.task.C4DefuseTask;
import com.qisumei.csgo.c4.task.C4TickTask;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * C4管理器，作为C4功能的总调度者。
 */
public class C4Manager implements C4Controller {

    private final MatchContext context;
    private final C4CountdownHandler countdownHandler;
    private final C4DefuseTask defuseTask;
    private final C4TickTask tickTask;

    // C4 核心状态
    private boolean isPlanted = false;
    private BlockPos c4Pos;

    public C4Manager(MatchContext context) {
        this.context = context;
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
     * @param player 当时tick的玩家
     */
    public void handlePlayerTick(ServerPlayer player) {
        tickTask.handlePlayerTick(player);
        defuseTask.handlePlayerDefuseTick(player);
    }

    /**
     * 重置所有C4相关状态。
     */
    public void reset() {
        if (isPlanted && c4Pos != null && context != null && context.getServer() != null) {
            context.getServer().overworld().removeBlock(c4Pos, false);
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
        if(context.getBombsiteA() != null && context.getBombsiteA().contains(pos.getCenter())){
            siteName = "A点";
        } else if(context.getBombsiteB() != null && context.getBombsiteB().contains(pos.getCenter())){
            siteName = "B点";
        }

        Component message = Component.literal("§e[信息] §f炸弹已安放在 §a" + siteName + "！");
        context.broadcastToAllPlayersInMatch(message);
    }

    public void onC4Defused(ServerPlayer defuser) {
        if (context.getRoundState() == Match.RoundState.IN_PROGRESS) {
            countdownHandler.stop();
            defuseTask.reset();
            context.broadcastToAllPlayersInMatch(Component.literal("§b" + (defuser != null ? defuser.getName().getString() : "CT") + " §f已成功拆除炸弹！"));
            context.endRound("CT", "炸弹已被拆除");
        }
    }

    public void onC4Exploded() {
        if (c4Pos != null) {
            applyCustomExplosionDamage(c4Pos);
        }
        
        // C4爆炸时清空所有存活CT玩家的装备
        clearCtEquipmentOnExplosion();
        
        context.endRound("T", "炸弹已爆炸");
    }
    
    /**
     * C4爆炸时清空所有存活CT玩家的装备
     * 强化"C4防守失败的经济代价"
     */
    private void clearCtEquipmentOnExplosion() {
        for (UUID playerUUID : new ArrayList<>(context.getAlivePlayers())) {
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;
            
            // 只清空CT玩家的装备
            var stats = context.getPlayerStats().get(playerUUID);
            if (stats != null && "CT".equals(stats.getTeam())) {
                // 清空整个背包（包括工具栏和背包）
                player.getInventory().clearContent();
                player.sendSystemMessage(
                    Component.literal("§c炸弹爆炸！你的所有装备已被摧毁！")
                        .withStyle(ChatFormatting.RED),
                    false
                );
                QisCSGO.LOGGER.info("CT玩家 {} 的装备因C4爆炸而被清空", player.getName().getString());
            }
        }
    }

    /**
     * 随机给一名 T 队玩家发放 C4。
     * 改进的防御性编程：更详细的日志记录和错误处理。
     */
    public void giveC4ToRandomT() {
        // 保护性检查：确保 C4 物品已正确注册
        Item c4Item = null;
        try {
            c4Item = QisCSGO.C4_ITEM.get();
        } catch (Throwable t) {
            QisCSGO.LOGGER.error("尝试获取 C4_ITEM 时发生异常：", t);
            return;
        }

        if (c4Item == null) {
            QisCSGO.LOGGER.warn("C4 物品未注册或不可用：跳过发放 C4。");
            return;
        }

        // 获取所有在线的 T 队玩家
        List<ServerPlayer> tPlayers = context.getPlayerStats().entrySet().stream()
            .filter(e -> "T".equals(e.getValue().getTeam()))
            .map(e -> context.getServer().getPlayerList().getPlayer(e.getKey()))
            .filter(Objects::nonNull)
            .toList(); 

        if (tPlayers.isEmpty()) {
            QisCSGO.LOGGER.warn("没有在线的 T 队玩家可以接收 C4");
            return;
        }

        // 随机选择一名 T 队玩家并发放 C4
        ServerPlayer playerWithC4 = tPlayers.get(new Random().nextInt(tPlayers.size()));
        try {
            playerWithC4.getInventory().add(new ItemStack(c4Item));
            playerWithC4.sendSystemMessage(Component.literal("§e你携带了C4炸弹！").withStyle(ChatFormatting.BOLD));
            QisCSGO.LOGGER.info("C4 已发放给玩家 {}", playerWithC4.getName().getString());
        } catch (Throwable t) {
            QisCSGO.LOGGER.error("给玩家 {} 发放 C4 时发生异常：", playerWithC4.getName().getString(), t);
        }
    }

    private void applyCustomExplosionDamage(BlockPos c4Pos) {
        final double explosionRadius = 16.0;
        final float maxDamage = 100.0f;

        double explosionX = c4Pos.getX() + 0.5;
        double explosionY = c4Pos.getY() + 0.5;
        double explosionZ = c4Pos.getZ() + 0.5;

        DamageSource damageSource = context.getServer().overworld().damageSources().genericKill();

        for (UUID playerUUID : new ArrayList<>(context.getAlivePlayers())) {
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerUUID);
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
    
    public MatchContext getContext() {
        return this.context;
    }
    
    public int getC4TicksLeft() {
        return countdownHandler.getTicksLeft();
    }
}