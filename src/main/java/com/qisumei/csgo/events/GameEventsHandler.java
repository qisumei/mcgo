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

@EventBusSubscriber
public class GameEventsHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MatchManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            Match match = MatchManager.getPlayerMatch(deadPlayer);
            if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {

                DamageSource source = event.getSource();
                Entity killerEntity = source.getEntity();

                if (killerEntity instanceof ServerPlayer killerPlayer && killerPlayer != deadPlayer) {
                    ItemStack weapon = killerPlayer.getMainHandItem();
                    Component deathMessage = killerPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(" 使用 ").withStyle(ChatFormatting.GRAY))
                        .append(weapon.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" 击杀了 ").withStyle(ChatFormatting.GRAY))
                        .append(deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED));
                    
                    // --- 修正 #3: 调用 broadcastToAllPlayersInMatch 时不再需要 server 参数 ---
                    match.broadcastToAllPlayersInMatch(deathMessage);
                    
                    if (match.getPlayerStats().containsKey(killerPlayer.getUUID())) {
                        int reward = EconomyManager.getRewardForKill(weapon);
                        if (reward > 0) EconomyManager.giveMoney(killerPlayer, reward);
                        
                        PlayerStats killerStats = match.getPlayerStats().get(killerPlayer.getUUID());
                        if(killerStats != null) killerStats.incrementKills();
                    }
                } else {
                    Component deathMessage = deadPlayer.getDisplayName().copy().withStyle(ChatFormatting.RED)
                        .append(Component.literal(" 阵亡了").withStyle(ChatFormatting.GRAY));
                    
                    // --- 修正 #4: 调用 broadcastToAllPlayersInMatch 时不再需要 server 参数 ---
                    match.broadcastToAllPlayersInMatch(deathMessage);
                }
                
                match.markPlayerAsDead(deadPlayer);
            }
        }
    }
}