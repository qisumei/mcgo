package com.qisumei.csgo.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 经济服务接口，提供玩家货币管理的核心功能。
 * 支持依赖注入和解耦的服务架构。
 */
public interface EconomyService {
    /**
     * 给玩家增加货币。
     * @param player 目标玩家
     * @param amount 货币数量
     */
    void giveMoney(ServerPlayer player, int amount);
    
    /**
     * 设置玩家的货币数量（替换当前值）。
     * @param player 目标玩家
     * @param amount 新的货币数量
     */
    void setMoney(ServerPlayer player, int amount);
    
    /**
     * 根据使用的武器获取击杀奖励金额。
     * @param weapon 武器物品栈
     * @return 奖励金额
     */
    int getRewardForKill(ItemStack weapon);
}

