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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
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
        MatchManager.tick();
    }

    /**
     * 在每个玩家的游戏刻（tick）中调用。
     * 用于持续检查并纠正CT玩家持有C4的情况。
     * @param event 玩家 tick 事件对象
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // 确认事件发生在服务端，并且玩家是 ServerPlayer 类型
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取该玩家所在的比赛
            Match match = MatchManager.getPlayerMatch(player);
            // 如果玩家不在比赛中，则不进行任何操作
            if (match == null) {
                return;
            }

            // 获取玩家的统计数据（包含队伍信息）
            PlayerStats stats = match.getPlayerStats().get(player.getUUID());
            if (stats == null) {
                return;
            }

            // 核心逻辑一：检查CT玩家是否持有C4
            // 如果玩家是CT阵营
            if ("CT".equals(stats.getTeam())) {
                // 遍历玩家的主物品栏
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    // 获取当前格子的物品
                    ItemStack stack = player.getInventory().getItem(i);
                    // 检查这个物品是不是C4
                    if (stack.is(QisCSGO.C4_ITEM.get())) {

                        // 1. 创建C4物品的一个副本
                        ItemStack c4ToDrop = stack.copy();
                        
                        // 2. 将玩家物品栏中对应格子的物品清空。
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        
                        // 3. 调用玩家实体的 drop 方法，将C4副本扔到地上，并捕获返回的ItemEntity。
                        // --- (开始修改) ---
                        ItemEntity c4Entity = player.drop(c4ToDrop, false, false);

                        // --- (开始修改) ---
                        if (c4Entity != null) {
                            // 1. 创建一个新的 Marker 实体
                            Marker marker = new Marker(EntityType.MARKER, player.level());
                            
                            // 2. 设置 Marker 的位置与 C4 相同
                            marker.setPos(c4Entity.getX(), c4Entity.getY(), c4Entity.getZ());
                            
                            // 3. 设置 Marker 的自定义名称
                            marker.setCustomName(Component.literal("C4").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                            marker.setCustomNameVisible(true);
                            
                            // 4. 将 Marker 添加到世界中
                            player.level().addFreshEntity(marker);
                            
                            // 5. 【核心】让 Marker "骑" 在 C4 物品上，这样它们会一起移动
                            marker.startRiding(c4Entity, true);

                            // 6. 将 Marker (而不是 C4 物品) 加入 T 队
                            net.minecraft.world.scores.Scoreboard scoreboard = player.server.getScoreboard();
                            net.minecraft.world.scores.PlayerTeam tTeam = scoreboard.getPlayerTeam(match.getTTeamName());
                            if (tTeam != null) {
                                scoreboard.addPlayerToTeam(marker.getStringUUID(), tTeam);
                            }
                        }

                        // 4. 给玩家一个明确的提示。
                        player.sendSystemMessage(Component.literal("§c作为CT，你不能持有C4！已强制丢弃。").withStyle(ChatFormatting.RED));
                        
                        // 5. 在日志中记录，方便调试。
                        QisCSGO.LOGGER.warn("已强制CT玩家 {} 丢弃C4。", player.getName().getString());
                        
                        
                        break; 
                    }
                }
            }

            // --- 逻辑二：为T提供包点指引 ---
            else if ("T".equals(stats.getTeam())) {
                // --- 包点指引逻辑 ---
                // 检查玩家主手或副手是否持有C4
                boolean holdingC4 = player.getMainHandItem().is(QisCSGO.C4_ITEM.get()) || player.getOffhandItem().is(QisCSGO.C4_ITEM.get());
                // 如果手持C4，并且当前回合正在进行中
                if (holdingC4 && match.getRoundState() == Match.RoundState.IN_PROGRESS) {
                    // 检查玩家是否在任何一个包点内
                    if (match.isPlayerInBombsite(player)) {
                        // 创建要在快捷栏上方显示的消息
                        Component message = Component.literal("你正处于炸弹安放区，可以安放C4！").withStyle(ChatFormatting.GREEN);
                        // 发送消息，第二个参数 'true' 意味着它显示在 action bar 上
                        player.sendSystemMessage(message, true);
                    }
                }
            }
            // --- 新增逻辑三：处理C4拆除 ---
            // 如果C4已经被安放，并且当前tick的玩家是CT
            if (match.isC4Planted() && "CT".equals(stats.getTeam())) {
                // 调用Match类中的拆弹处理逻辑
                match.handlePlayerDefuseTick(player);
            }
        }
    }


    /**
     * 处理玩家死亡事件。
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        Match match = MatchManager.getPlayerMatch(deadPlayer);
        if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
            
            // --- 1. 手动处理物品掉落 ---
            for (int i = 0; i < deadPlayer.getInventory().getContainerSize(); i++) {
                ItemStack stack = deadPlayer.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }

                // 检查物品是否受保护（比如钱、护甲）
                boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                                            .anyMatch(id -> ItemNBTHelper.idMatches(stack, id));

                // 如果物品不受保护，则将其掉落
                if (!isProtected) {
                    ItemEntity droppedItem = deadPlayer.drop(stack.copy(), true, false);
                    deadPlayer.getInventory().setItem(i, ItemStack.EMPTY);

                    // --- (开始修改) ---
                    if (droppedItem != null && droppedItem.getItem().is(QisCSGO.C4_ITEM.get())) {
                        // 1. 创建一个新的 Marker 实体
                        Marker marker = new Marker(EntityType.MARKER, deadPlayer.level());
                        
                        // 2. 设置 Marker 的位置与 C4 相同
                        marker.setPos(droppedItem.getX(), droppedItem.getY(), droppedItem.getZ());
                        
                        // 3. 设置 Marker 的自定义名称
                        marker.setCustomName(Component.literal("C4").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                        marker.setCustomNameVisible(true);
                        
                        // 4. 将 Marker 添加到世界中
                        deadPlayer.level().addFreshEntity(marker);

                        // 5. 【核心】让 Marker "骑" 在 C4 物品上
                        marker.startRiding(droppedItem, true);

                        // 6. 将 Marker 加入 T 队
                        net.minecraft.world.scores.Scoreboard scoreboard = deadPlayer.server.getScoreboard();
                        net.minecraft.world.scores.PlayerTeam tTeam = scoreboard.getPlayerTeam(match.getTTeamName());
                        if (tTeam != null) {
                            scoreboard.addPlayerToTeam(marker.getStringUUID(), tTeam);
                        }
                    }
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
                    int reward = EconomyManager.getRewardForKill(weapon);
                    if (reward > 0) EconomyManager.giveMoney(killerPlayer, reward);
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
}