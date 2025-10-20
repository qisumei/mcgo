package com.qisumei.csgo.game;

import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 经济管理器工具类，负责处理CSGO比赛中的金钱相关逻辑。
 * <p>
 * 这个类提供了两个核心功能：
 * 1. 向玩家发放游戏货币。
 * 2. 根据玩家击杀时使用的武器类型，计算并返回相应的金钱奖励。
 * 所有数值都从 {@link ServerConfig} 中读取，确保了配置的灵活性。
 * </p>
 *
 * @author Qisumei
 */
public final class EconomyManager {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private EconomyManager() {}

    /**
     * 向指定玩家发放一定数量的游戏货币（通过执行give命令）。
     *
     * @param player 接收货币的玩家。
     * @param amount 发放的货币数量。如果小于等于0，则不执行任何操作。
     */
    public static void giveMoney(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        // 使用 /give 命令给予玩家钻石作为货币
        String command = "give " + player.getName().getString() + " minecraft:diamond " + amount;
        // 在服务器端执行命令
        player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), command);
    }

    /**
     * 根据玩家击杀时使用的武器，从服务器配置中获取相应的金钱奖励。
     * <p>
     * 它会依次检查武器是否属于刀、手枪、冲锋枪等类别，并返回第一个匹配到的奖励金额。
     * 如果武器不属于任何已定义的类别，或者玩家使用的是空手，则默认返回手枪的击杀奖励。
     * </p>
     *
     * @param weapon 玩家用于击杀的 {@link ItemStack}。
     * @return 对应的金钱奖励数额。
     */
    public static int getRewardForKill(ItemStack weapon) {
        // 如果武器物品栈为空（例如，空手），默认返回手枪奖励
        if (weapon.isEmpty()) {
            return ServerConfig.killRewardPistol;
        }

        // **[变量同步]** 使用了之前重构的 ItemNBTHelper.isSameBaseItem 方法和 ServerConfig 中的变量。
        if (ServerConfig.weaponsKnife.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardKnife;
        }
        if (ServerConfig.weaponsPistol.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardPistol;
        }
        if (ServerConfig.weaponsSmg.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardSmg;
        }
        if (ServerConfig.weaponsHeavy.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardHeavy;
        }
        if (ServerConfig.weaponsRifle.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardRifle;
        }
        if (ServerConfig.weaponsAwp.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardAwp;
        }
        if (ServerConfig.weaponsGrenade.stream().anyMatch(id -> ItemNBTHelper.isSameBaseItem(weapon, id))) {
            return ServerConfig.killRewardGrenade;
        }

        // 如果武器不属于任何特定类别，则返回默认的手枪奖励
        return ServerConfig.killRewardPistol;
    }
}
