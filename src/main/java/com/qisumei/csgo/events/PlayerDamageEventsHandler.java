package com.qisumei.csgo.events;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.List;

/**
 * 玩家伤害事件处理器
 * 分别处理玩家之间的直接攻击和爆炸伤害，以控制友军伤害。
 */
@EventBusSubscriber
public class PlayerDamageEventsHandler {

    /**
     * 当玩家直接攻击另一个实体时触发。
     * 这个方法用于处理近战、弓箭等直接指向性攻击的友伤。
     *
     * @param event 攻击实体事件对象。
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        // 检查服务器配置是否禁用了友伤。
        if (!ServerConfig.friendlyFireEnabled) {
            // 确认攻击者和被攻击者都是玩家。
            if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer victim) {
                // 获取攻击者所在的比赛。
                Match match = MatchManager.getPlayerMatch(attacker);
                // 确保他们在同一场比赛中。
                if (match != null && match.getPlayerStats().containsKey(victim.getUUID())) {
                    PlayerStats attackerStats = match.getPlayerStats().get(attacker.getUUID());
                    PlayerStats victimStats = match.getPlayerStats().get(victim.getUUID());
                    // 检查他们是否在同一个队伍。
                    if (attackerStats != null && victimStats != null && attackerStats.getTeam().equals(victimStats.getTeam())) {
                        // 如果是队友，取消攻击事件。
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    /**
     * 当爆炸即将发生并确定影响范围时触发。
     * 这个方法专门用于处理手雷等爆炸物造成的友军伤害。
     *
     * @param event 爆炸事件对象。
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        // 检查服务器配置是否禁用了友伤。
        if (!ServerConfig.friendlyFireEnabled) {
            // 获取爆炸的来源实体（例如，扔手雷的玩家）。
            Entity exploder = event.getExplosion().getDirectSourceEntity();

            // 如果爆炸源是一个玩家。
            if (exploder instanceof Player attacker) {
                // 获取该玩家所在的比赛。
                Match match = MatchManager.getPlayerMatch(attacker);
                if (match == null) {
                    return; // 如果该玩家不在比赛中，则不做任何处理。
                }

                // 获取攻击者的队伍信息。
                PlayerStats attackerStats = match.getPlayerStats().get(attacker.getUUID());
                if (attackerStats == null) {
                    return; // 如果获取不到统计信息，则不做处理。
                }
                String attackerTeam = attackerStats.getTeam();

                // 获取即将被爆炸波及的所有实体列表。
                List<Entity> affectedEntities = event.getAffectedEntities();

                // 使用 removeIf 方法，移除所有即将被波及的队友。
                // 这是一个高效且安全地在迭代中修改列表的方式。
                affectedEntities.removeIf(entity -> {
                    // 检查被波及的实体是不是一名玩家。
                    if (entity instanceof Player victim) {
                        // 获取受害玩家的比赛统计信息。
                        PlayerStats victimStats = match.getPlayerStats().get(victim.getUUID());
                        // 如果该玩家也在比赛中，并且和攻击者是同一队。
                        if (victimStats != null && attackerTeam.equals(victimStats.getTeam())) {
                            // 并且确保不是自己炸自己（虽然一般不会移除自己）。
                            if (attacker != victim) {
                                // 返回 true，代表将这个队友从受影响列表中移除。
                                return true;
                            }
                        }
                    }
                    // 其他情况（非玩家，或不是队友），返回 false，保留在列表中。
                    return false;
                });
            }
        }
    }
}