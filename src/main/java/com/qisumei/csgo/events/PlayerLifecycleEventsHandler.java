package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 玩家生命周期事件处理器。
 * <p>
 * 负责监听玩家登录和重生等事件，以处理玩家在比赛中断线重连或死亡后重生时的状态同步问题。
 * </p>
 *
 * @author Qisumei
 */
@EventBusSubscriber(modid = QisCSGO.MODID)
public final class PlayerLifecycleEventsHandler {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private PlayerLifecycleEventsHandler() {}

    /**
     * 当玩家登录服务器时触发。
     * <p>
     * 如果一个正在进行比赛的玩家掉线后重新登录，此方法会：
     * 1. 将其设置为观察者模式，防止其在不合适的时间点复活。
     * 2. 重新向其发送计分板和Boss栏信息，以同步UI。
     * </p>
     *
     * @param event 玩家登录事件对象。
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Match match = MatchManager.getPlayerMatch(player);

            // 检查玩家是否属于一场正在进行中的比赛
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                player.setGameMode(GameType.SPECTATOR); // 强制设为观察者
                player.sendSystemMessage(Component.literal("你已重新连接至比赛，请等待下一回合开始。"));

                // 重新同步UI
                match.reapplyScoreboardToPlayer(player);
                match.getBossBar().addPlayer(player);
            }
        }
    }

    /**
     * 当玩家重生时触发。
     * <p>
     * 确保在比赛中死亡的玩家重生后，立即被设置为观察者模式，而不是在出生点复活。
     * 具体的观战逻辑由 {@link Match#handlePlayerRespawn(ServerPlayer)} 处理。
     * </p>
     *
     * @param event 玩家重生事件对象。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Match match = MatchManager.getPlayerMatch(player);

            // 如果玩家在一场正在进行的比赛中重生
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                // 调用比赛类中的重生处理逻辑
                match.handlePlayerRespawn(player);
            }
        }
    }
}
