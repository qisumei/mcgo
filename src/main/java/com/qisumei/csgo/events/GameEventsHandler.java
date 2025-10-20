package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.EconomyManager;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import com.qisumei.csgo.util.ItemNBTHelper;
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
 * 游戏核心事件处理器。
 * <p>
 * 这个类监听服务端和玩家的 tick 事件以及生物死亡事件，以驱动比赛的核心逻辑循环、
 * 处理玩家行为检测（如拆弹、C4持有检查）和处理玩家死亡相关的事务。
 * </p>
 *
 * @author Qisumei
 */
@EventBusSubscriber(modid = QisCSGO.MODID)
public final class GameEventsHandler {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private GameEventsHandler() {}

    /**
     * 监听服务器 tick 事件。
     * 每个服务器 tick 结束后，此方法会调用 {@link MatchManager#tick()} 来更新所有正在进行的比赛状态。
     *
     * @param event 服务器 tick 事件对象。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MatchManager.tick();
    }

    /**
     * 监听玩家 tick 事件。
     * 在每个玩家的每个 tick 中，此方法会执行一系列与比赛相关的状态检查和逻辑处理。
     *
     * @param event 玩家 tick 事件对象。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Match match = MatchManager.getPlayerMatch(player);
        if (match == null) {
            return;
        }

        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        if (stats == null) {
            return;
        }

        // --- 逻辑一: 检查CT玩家是否非法持有C4 ---
        if ("CT".equals(stats.getTeam())) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(QisCSGO.C4_ITEM.get())) {
                    // 发现CT持有C4，强制其丢弃
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.drop(stack.copy(), false, false);
                    player.sendSystemMessage(Component.literal("§c作为CT，你不能持有C4！已强制丢弃。"), true);
                    QisCSGO.LOGGER.warn("已强制CT玩家 {} 丢弃C4。", player.getName().getString());
                    break; // 找到一个就够了
                }
            }
        }
        // --- 逻辑二: 为持有C4的T提供包点指引 ---
        else if ("T".equals(stats.getTeam())) {
            boolean isHoldingC4 = player.getMainHandItem().is(QisCSGO.C4_ITEM.get()) || player.getOffhandItem().is(QisCSGO.C4_ITEM.get());
            if (isHoldingC4 && match.getRoundState() == Match.RoundState.IN_PROGRESS) {
                if (match.isPlayerInBombsite(player)) {
                    // 如果T正手持C4且在包点内，显示提示信息
                    player.sendSystemMessage(Component.literal("你正处于炸弹安放区，可以安放C4！").withStyle(ChatFormatting.GREEN), true);
                }
            }
        }

        // --- 逻辑三: 为CT处理C4拆除逻辑 ---
        if (match.isC4Planted() && "CT".equals(stats.getTeam())) {
            // 如果C4已安放且玩家是CT，每tick调用比赛的拆弹处理逻辑
            match.handlePlayerDefuseTick(player);
        }
    }


    /**
     * 监听生物死亡事件，专门处理在比赛中玩家的死亡。
     *
     * @param event 生物死亡事件对象。
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        Match match = MatchManager.getPlayerMatch(deadPlayer);
        if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
            return;
        }

        // --- 1. 自定义物品掉落逻辑 ---
        for (int i = 0; i < deadPlayer.getInventory().getContainerSize(); i++) {
            ItemStack stack = deadPlayer.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            // **[变量同步]** 使用重构后的 ServerConfig 和 ItemNBTHelper
            boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                    .anyMatch(id -> ItemNBTHelper.isSameBaseItem(stack, id));

            // 如果物品不受保护，则掉落并从背包中移除
            if (!isProtected) {
                deadPlayer.drop(stack.copy(), true, false);
                deadPlayer.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        // --- 2. 处理击杀信息和奖励 ---
        DamageSource source = event.getSource();
        Entity killerEntity = source.getEntity();

        if (killerEntity instanceof ServerPlayer killerPlayer && killerPlayer != deadPlayer) {
            // 如果击杀者是另一名玩家
            ItemStack weapon = killerPlayer.getMainHandItem();
            Component deathMessage = killerPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" 使用 ").withStyle(ChatFormatting.GRAY))
                .append(weapon.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 击杀了 ").withStyle(ChatFormatting.GRAY))
                .append(deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED));
            match.broadcastToAllPlayersInMatch(deathMessage);

            // 如果击杀者在比赛中，则给予奖励并增加击杀数
            if (match.getPlayerStats().containsKey(killerPlayer.getUUID())) {
                int reward = EconomyManager.getRewardForKill(weapon);
                EconomyManager.giveMoney(killerPlayer, reward);
                
                PlayerStats killerStats = match.getPlayerStats().get(killerPlayer.getUUID());
                if (killerStats != null) {
                    killerStats.incrementKills();
                }
            }
        } else {
            // 如果是其他原因死亡（如摔死、溺水等）
            Component deathMessage = deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED)
                .append(Component.literal(" 阵亡了").withStyle(ChatFormatting.GRAY));
            match.broadcastToAllPlayersInMatch(deathMessage);
        }

        // --- 3. 通知Match类处理玩家死亡的核心逻辑 ---
        match.markPlayerAsDead(deadPlayer);
    }
}
