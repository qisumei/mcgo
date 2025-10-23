package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.service.ServiceFallbacks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 玩家生命周期事件处理器
 * 处理玩家登录、重生等事件，确保玩家在比赛中的状态得到正确恢复。
 */
@EventBusSubscriber
public class PlayerLifecycleEventsHandler {

    /**
     * 处理玩家登录事件。
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                // 通过 ServiceFallbacks 获取 match（支持替代实现或回退）
                Match match = ServiceFallbacks.getPlayerMatch(player);

                if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.sendSystemMessage(Component.literal("你已重新连接至比赛，请等待下一回合开始。"));

                    // 为重新连接的玩家重新应用计分板
                    match.reapplyScoreboardToPlayer(player);
                    // 为重新连接的玩家重新显示Boss栏
                    match.getBossBar().addPlayer(player);
                    
                    QisCSGO.LOGGER.info("玩家 {} 重新连接到比赛 '{}'", player.getName().getString(), match.getName());
                }
            }
        } catch (Exception e) {
            QisCSGO.LOGGER.error("处理玩家登录事件时发生异常", e);
        }
    }

    /**
     * 处理玩家重生事件。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                Match match = ServiceFallbacks.getPlayerMatch(player);

                if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                    match.handlePlayerRespawn(player);
                    QisCSGO.LOGGER.debug("处理玩家 {} 在比赛中的重生", player.getName().getString());
                }
            }
        } catch (Exception e) {
            QisCSGO.LOGGER.error("处理玩家重生事件时发生异常", e);
        }
    }
}
