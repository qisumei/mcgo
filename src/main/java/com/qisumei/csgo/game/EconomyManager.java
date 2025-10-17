package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.qisumei.csgo.util.ItemNBTHelper;

/**
 * 经济系统管理类，用于处理游戏中玩家的金钱发放以及击杀奖励计算逻辑。
 */
public class EconomyManager {

    /**
     * 向指定玩家发放一定数量的游戏货币（以钻石形式发放）。
     *
     * @param player 要发放货币的玩家对象
     * @param amount 发放的数量，必须大于0才生效
     */
    public static void giveMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        // --- 修改: minecraft:emerald -> minecraft:diamond ---
        String command = "give " + player.getName().getString() + " minecraft:diamond " + amount;
        player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), command);
    }

    /**
     * 根据使用的武器类型获取击杀敌人后的奖励金额。
     *
     * @param weapon 击杀所用的武器 ItemStack 对象
     * @return 对应武器类型的击杀奖励金额
     */
    public static int getRewardForKill(ItemStack weapon) {
        if (weapon.isEmpty()) return ServerConfig.killRewardPistol; // 默认手枪奖励

        // --- 使用新的工具方法来比较物品ID (忽略NBT) ---
        // 判断是否为近战武器（刀）
        if (ServerConfig.weaponsKnife.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardKnife;
        // 判断是否为手枪
        if (ServerConfig.weaponsPistol.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardPistol;
        // 判断是否为冲锋枪
        if (ServerConfig.weaponsSmg.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardSmg;
        // 判断是否为重型武器（如机枪）
        if (ServerConfig.weaponsHeavy.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardHeavy;
        // 判断是否为步枪
        if (ServerConfig.weaponsRifle.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardRifle;
        // 判断是否为狙击枪（AWP 类型）
        if (ServerConfig.weaponsAwp.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardAwp;
        // 判断是否为投掷物（如手雷）
        if (ServerConfig.weaponsGrenade.stream().anyMatch(s -> ItemNBTHelper.idMatches(weapon, s))) return ServerConfig.killRewardGrenade;

        return ServerConfig.killRewardPistol; // 默认手枪奖励
    }
}
