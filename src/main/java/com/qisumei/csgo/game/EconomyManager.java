package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.qisumei.csgo.util.ItemNBTHelper;

public class EconomyManager {
    
    public static void giveMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        // --- 修改: minecraft:emerald -> minecraft:diamond ---
        String command = "give " + player.getName().getString() + " minecraft:diamond " + amount;
        if (player.server != null) {
            player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), command);
        }
    }
    
    public static int getRewardForKill(ItemStack weapon) {
    if (weapon.isEmpty()) return ServerConfig.killRewardPistol; // 默认手枪奖励

    // --- 使用新的工具方法来比较物品ID (忽略NBT) ---
    if (ServerConfig.weaponsKnife.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardKnife;
    if (ServerConfig.weaponsPistol.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardPistol;
    if (ServerConfig.weaponsSmg.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardSmg;
    if (ServerConfig.weaponsHeavy.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardHeavy;
    if (ServerConfig.weaponsRifle.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardRifle;
    if (ServerConfig.weaponsAwp.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardAwp;
    if (ServerConfig.weaponsGrenade.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardGrenade;
    
    return ServerConfig.killRewardPistol; // 默认手枪奖励
}
}