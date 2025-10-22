// 文件: src/main/java/com/qisumei/csgo/events/GameEventsHandler.java
package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.PlayerStats;
import com.qisumei.csgo.util.ItemNBTHelper;
import com.qisumei.csgo.service.ServiceRegistry;
import com.qisumei.csgo.service.ServiceFallbacks;
import com.qisumei.csgo.server.ServerCommandExecutor;
import com.qisumei.csgo.server.ServerCommandExecutorImpl;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 游戏事件处理器，用于处理服务器 tick、玩家 tick 和玩家死亡等游戏核心事件。
 * 
 * <p>改进点：
 * <ul>
 *   <li>使用Java 21的增强模式匹配简化类型检查</li>
 *   <li>使用var简化局部变量声明</li>
 *   <li>改进代码可读性和结构</li>
 * </ul>
 */
@EventBusSubscriber
public final class GameEventsHandler {
    
    // 私有构造器防止实例化
    private GameEventsHandler() {
        throw new AssertionError("Event handler class should not be instantiated");
    }

    /**
     * 每个服务器 tick 结束后调用该方法，驱动 MatchManager 的逻辑更新。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 使用 ServiceFallbacks 统一调用（支持注册的 MatchService 或回退实现）
        ServiceFallbacks.tickMatches();
    }

    /**
     * 在每个玩家的游戏刻（tick）中调用。
     * 将tick事件转发给C4Manager来处理所有C4相关的逻辑。
     * 使用Java 21的模式匹配简化类型检查
     * 
     * @param event 玩家 tick 事件对象
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // 使用Java 21的模式匹配简化instanceof检查和类型转换
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 确保注册默认的ServerCommandExecutor（懒加载，只注册一次）
        ensureCommandExecutorRegistered(player);

        // 获取该玩家所在的比赛
        var match = ServiceFallbacks.getPlayerMatch(player);
        if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
            return;
        }

        // 将所有C4相关的tick逻辑委托给C4Manager处理
        match.getC4Manager().handlePlayerTick(player);
    }

    /**
     * 确保ServerCommandExecutor已注册（懒加载）
     * 
     * @param player 服务器玩家实例，用于获取server对象
     */
    private static void ensureCommandExecutorRegistered(ServerPlayer player) {
        if (ServiceRegistry.get(ServerCommandExecutor.class) == null) {
            try {
                ServiceRegistry.register(
                    ServerCommandExecutor.class, 
                    new ServerCommandExecutorImpl(player.server)
                );
                QisCSGO.LOGGER.info("Registered default ServerCommandExecutorImpl via ServiceRegistry.");
            } catch (Exception ex) {
                QisCSGO.LOGGER.warn("Failed to register ServerCommandExecutorImpl: {}", ex.getMessage());
            }
        }
    }

    /**
     * 处理玩家死亡事件。
     * 使用Java 21的模式匹配和var简化代码
     * 
     * @param event 玩家死亡事件
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 使用模式匹配简化类型检查
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        // 获取玩家所在的比赛
        var match = ServiceFallbacks.getPlayerMatch(deadPlayer);
        if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
            return;
        }

        // 1. 处理物品掉落
        handleItemDrops(deadPlayer);

        // 2. 处理击杀播报和奖励
        handleKillBroadcast(event, deadPlayer, match);

        // 3. 标记玩家死亡
        match.markPlayerAsDead(deadPlayer);
    }

    /**
     * 处理死亡玩家的物品掉落
     * 受保护的物品（如货币、护甲等）不会掉落
     * 
     * @param deadPlayer 死亡的玩家
     */
    private static void handleItemDrops(ServerPlayer deadPlayer) {
        var inventory = deadPlayer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            var isProtected = ServerConfig.inventoryProtectedItems.stream()
                .anyMatch(id -> ItemNBTHelper.idMatches(stack, id));

            if (!isProtected) {
                deadPlayer.drop(stack.copy(), true, false);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 处理击杀播报和奖励发放
     * 
     * @param event 死亡事件
     * @param deadPlayer 死亡的玩家
     * @param match 当前比赛
     */
    private static void handleKillBroadcast(LivingDeathEvent event, ServerPlayer deadPlayer, Match match) {
        var source = event.getSource();
        var killerEntity = source.getEntity();

        // 使用模式匹配检查击杀者
        if (killerEntity instanceof ServerPlayer killerPlayer && killerPlayer != deadPlayer) {
            var weapon = killerPlayer.getMainHandItem();
            
            // 构建击杀消息
            var deathMessage = killerPlayer.getDisplayName().copy()
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" 使用 ").withStyle(ChatFormatting.GRAY))
                .append(weapon.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 击杀了 ").withStyle(ChatFormatting.GRAY))
                .append(deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED));
            
            match.broadcastToAllPlayersInMatch(deathMessage);

            // 处理击杀奖励
            if (match.getPlayerStats().containsKey(killerPlayer.getUUID())) {
                var reward = ServiceFallbacks.getRewardForKill(weapon);
                if (reward > 0) {
                    ServiceFallbacks.giveMoney(killerPlayer, reward);
                }

                var killerStats = match.getPlayerStats().get(killerPlayer.getUUID());
                if (killerStats != null) {
                    killerStats.incrementKills();
                }
            }
        } else {
            // 非玩家击杀或自杀
            var deathMessage = deadPlayer.getDisplayName().copy()
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(" 阵亡了").withStyle(ChatFormatting.GRAY));
            match.broadcastToAllPlayersInMatch(deathMessage);
        }
    }
}