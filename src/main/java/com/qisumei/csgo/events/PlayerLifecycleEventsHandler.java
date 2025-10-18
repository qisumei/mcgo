package com.qisumei.csgo.events;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 玩家生命周期事件处理器
 * 处理玩家登录、重生等生命周期事件，确保玩家在比赛中的状态正确
 */
@EventBusSubscriber
public class PlayerLifecycleEventsHandler {

    /**
     * 处理玩家登录事件
     * 当玩家登录时，检查其是否处于进行中的比赛，如果是则设置为旁观者模式并重新同步计分板。
     *
     * @param event 玩家登录事件对象，包含登录的玩家实体
     */
    // --- 处理断线重连 ---
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 检查登录实体是否为服务器玩家
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取玩家所在的比赛
            Match match = MatchManager.getPlayerMatch(player);

            // 如果玩家在进行中的比赛中断线重连
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                player.setGameMode(GameType.SPECTATOR);
                player.sendSystemMessage(Component.literal("你已重新连接至比赛，请等待下一回合开始。"));
                // --- 新增代码: 为重连的玩家重新应用计分板 ---
                match.reapplyScoreboardToPlayer(player);
                // --- 新增代码: 为重连的玩家重新显示Boss栏 ---
                match.getBossBar().addPlayer(player); // 我们需要为Match类添加一个getter
            }
        }
    }

    /**
     * 处理玩家重生事件
     * 当玩家重生时，如果其处于进行中的比赛，则调用比赛的重生处理逻辑
     *
     * @param event 玩家重生事件对象，包含重生的玩家实体
     */
    // --- 新增：处理玩家重生 ---
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 检查重生实体是否为服务器玩家
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取玩家所在的比赛
            Match match = MatchManager.getPlayerMatch(player);

            // 如果玩家在比赛中重生，则将其变为旁观者
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                match.handlePlayerRespawn(player);
            }
        }
    }
}
