package com.qisumei.csgo.events;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber
public class PlayerDamageEventsHandler {

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        // 如果禁用了友伤
        if (!ServerConfig.friendlyFireEnabled) {
            // 攻击者和被攻击者都是玩家
            if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer victim) {
                Match match = MatchManager.getPlayerMatch(attacker);
                // 且他们在同一场比赛中
                if (match != null && match.getPlayerStats().containsKey(victim.getUUID())) {
                    PlayerStats attackerStats = match.getPlayerStats().get(attacker.getUUID());
                    PlayerStats victimStats = match.getPlayerStats().get(victim.getUUID());
                    // 且他们在同一队
                    if (attackerStats != null && victimStats != null && attackerStats.getTeam().equals(victimStats.getTeam())) {
                        // 取消攻击事件
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}