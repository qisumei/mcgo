package com.qisumei.csgo.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 抽象玩家相关操作的服务接口。
 */
public interface PlayerService {
    /**
     * 选择性清空玩家背包，保留受保护的物品（如护甲、货币等）。
     * 
     * @param player 要清空背包的玩家（不能为 null）
     * @throws NullPointerException 如果 player 为 null
     */
    void performSelectiveClear(ServerPlayer player);
    
    /**
     * 在手枪局为玩家发放初始装备。
     * 
     * @param player 要发放装备的玩家（不能为 null）
     * @param team 玩家所属队伍（"CT" 或 "T"）
     * @throws NullPointerException 如果 player 为 null
     * @throws IllegalArgumentException 如果 team 不是有效的队伍名称
     */
    void giveInitialGear(ServerPlayer player, String team);
    
    /**
     * 捕获玩家当前的装备快照，用于在下回合保留装备。
     * 
     * @param player 要捕获装备的玩家（不能为 null）
     * @return 装备物品栈列表的副本
     * @throws NullPointerException 如果 player 为 null
     */
    List<ItemStack> capturePlayerGear(ServerPlayer player);
}

