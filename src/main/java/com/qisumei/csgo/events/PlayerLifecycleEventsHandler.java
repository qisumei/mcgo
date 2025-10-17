package com.qisumei.csgo.events;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerLifecycleEventsHandler {

    // --- 处理断线重连 ---
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Match match = MatchManager.getPlayerMatch(player);
            
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                player.setGameMode(GameType.SPECTATOR);
                player.sendSystemMessage(Component.literal("你已重新连接至比赛，请等待下一回合开始。"));
            }
        }
    }

    // --- 新增：处理玩家重生 ---
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Match match = MatchManager.getPlayerMatch(player);
            
            // 如果玩家在比赛中重生，则将其变为旁观者
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
                match.handlePlayerRespawn(player);
            }
        }
    }
}