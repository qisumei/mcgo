package com.qisumei.csgo.events;

import com.qisumei.csgo.game.EconomyManager;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 游戏事件处理器，用于处理服务器 tick 和玩家死亡等游戏核心事件。
 */
@EventBusSubscriber
public class GameEventsHandler {

    /**
     * 每个服务器 tick 结束后调用该方法，驱动 MatchManager 的逻辑更新。
     *
     * @param event 服务器 tick 事件对象（Post 类型）
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MatchManager.tick(event.getServer());
    }

    /**
     * 处理玩家死亡事件，在比赛中根据击杀情况广播消息、给予经济奖励并更新统计数据。
     *
     * @param event 生物死亡事件对象，包含死亡实体及伤害来源信息
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 判断死亡实体是否是服务器端玩家
        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            // 获取该玩家所在的比赛实例
            Match match = MatchManager.getPlayerMatch(deadPlayer);
            // 只有在比赛进行中才处理死亡逻辑
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {

                DamageSource source = event.getSource();
                Entity killerEntity = source.getEntity();

                // 如果杀手也是服务器端玩家，并且不是自己杀死自己
                if (killerEntity instanceof ServerPlayer killerPlayer && killerPlayer != deadPlayer) {
                    // 构造击杀提示消息：[击杀者] 使用 [武器] 击杀了 [死者]
                    ItemStack weapon = killerPlayer.getMainHandItem();
                    Component deathMessage = killerPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(" 使用 ").withStyle(ChatFormatting.GRAY))
                        .append(weapon.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" 击杀了 ").withStyle(ChatFormatting.GRAY))
                        .append(deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED));

                    // 广播击杀消息给所有参与当前比赛的玩家
                    match.broadcastToAllPlayersInMatch(deathMessage);

                    // 给予击杀者金钱奖励并增加击杀数统计
                    if (match.getPlayerStats().containsKey(killerPlayer.getUUID())) {
                        int reward = EconomyManager.getRewardForKill(weapon);
                        if (reward > 0) EconomyManager.giveMoney(killerPlayer, reward);

                        PlayerStats killerStats = match.getPlayerStats().get(killerPlayer.getUUID());
                        if(killerStats != null) killerStats.incrementKills();
                    }
                } else {
                    // 若无有效击杀者，则显示普通阵亡消息
                    Component deathMessage = deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED)
                        .append(Component.literal(" 阵亡了").withStyle(ChatFormatting.GRAY));

                    // 广播阵亡消息给所有参与当前比赛的玩家
                    match.broadcastToAllPlayersInMatch(deathMessage);
                }

                // 标记该玩家已死亡
                match.markPlayerAsDead(deadPlayer);
            }
        }
    }
}
