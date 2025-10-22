package com.qisumei.csgo.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 抽象玩家相关操作的服务接口。
 * 
 * <p>此接口遵循依赖倒置原则（Dependency Inversion Principle），
 * 允许高层模块（如 Match）不依赖于具体实现，而是依赖于抽象接口。
 * 这提高了代码的可测试性和可维护性。</p>
 * 
 * <p>设计优势：</p>
 * <ul>
 *   <li>解耦：Match 类不需要直接依赖 MatchPlayerHelper 静态类</li>
 *   <li>可测试：可以注入 mock 实现进行单元测试</li>
 *   <li>可扩展：可以提供不同的实现而不修改 Match 类</li>
 * </ul>
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

