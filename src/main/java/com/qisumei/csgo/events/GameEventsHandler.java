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
 */
@EventBusSubscriber
public class GameEventsHandler {

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
     * @param event 玩家 tick 事件对象
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // 确认事件发生在服务端，并且玩家是 ServerPlayer 类型
        if (event.getEntity() instanceof ServerPlayer player) {
            // Ensure a default ServerCommandExecutor is registered (once) when server is available
            try {
                if (ServiceRegistry.get(ServerCommandExecutor.class) == null) {
                    ServiceRegistry.register(ServerCommandExecutor.class, new ServerCommandExecutorImpl(player.server));
                    QisCSGO.LOGGER.info("Registered default ServerCommandExecutorImpl via ServiceRegistry.");
                }
            } catch (Exception ex) {
                QisCSGO.LOGGER.warn("Failed to register ServerCommandExecutorImpl: {}", ex.getMessage());
            }

            // 获取该玩家所在的比赛（使用 ServiceFallbacks 以支持回退）
            Match match = ServiceFallbacks.getPlayerMatch(player);
             // 如果玩家不在比赛中，则不进行任何操作
             if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
                 return;
             }

            // 将所有C4相关的tick逻辑委托给C4Manager处理
            match.getC4Manager().handlePlayerTick(player);
        }
    }

    /**
     * 处理玩家死亡事件。
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;

        // 通过 ServiceFallbacks 获取玩家所处比赛（支持回退）
        Match match = ServiceFallbacks.getPlayerMatch(deadPlayer);
        if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) return;

         // --- 1. 手动处理物品掉落 ---
        for (int i = 0; i < deadPlayer.getInventory().getContainerSize(); i++) {
             ItemStack stack = deadPlayer.getInventory().getItem(i);
             if (stack.isEmpty()) continue;

             boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                     .anyMatch(id -> ItemNBTHelper.idMatches(stack, id));

             if (!isProtected) {
                 deadPlayer.drop(stack.copy(), true, false);
                 deadPlayer.getInventory().setItem(i, ItemStack.EMPTY);
             }
         }

         // --- 2. 处理击杀播报 ---
         DamageSource source = event.getSource();
         Entity killerEntity = source.getEntity();

         if (killerEntity instanceof ServerPlayer killerPlayer && killerPlayer != deadPlayer) {
             ItemStack weapon = killerPlayer.getMainHandItem();
             Component deathMessage = killerPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
                     .append(Component.literal(" 使用 ").withStyle(ChatFormatting.GRAY))
                     .append(weapon.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                     .append(Component.literal(" 击杀了 ").withStyle(ChatFormatting.GRAY))
                     .append(deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED));
             match.broadcastToAllPlayersInMatch(deathMessage);

             if (match.getPlayerStats().containsKey(killerPlayer.getUUID())) {
                 // Use ServiceFallbacks which will prefer a registered EconomyService and fall back to EconomyManager
                 int reward = ServiceFallbacks.getRewardForKill(weapon);
                 if (reward > 0) {
                     ServiceFallbacks.giveMoney(killerPlayer, reward);
                 }

                 PlayerStats killerStats = match.getPlayerStats().get(killerPlayer.getUUID());
                 if (killerStats != null) killerStats.incrementKills();
             }
         } else {
             Component deathMessage = deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED)
                     .append(Component.literal(" 阵亡了").withStyle(ChatFormatting.GRAY));
             match.broadcastToAllPlayersInMatch(deathMessage);
         }

         // --- 3. 最后再调用比赛的死亡处理逻辑 ---
         match.markPlayerAsDead(deadPlayer);
     }
}