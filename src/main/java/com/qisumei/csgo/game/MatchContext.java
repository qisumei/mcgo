package com.qisumei.csgo.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MatchContext 接口提供比赛上下文的最小必需 API。
 * 
 * <p>此接口遵循接口隔离原则（Interface Segregation Principle），
 * 只暴露 C4Manager 等子系统真正需要的方法，而不是完整的 Match API。
 * 这种设计降低了模块间的耦合，提高了代码的可维护性和可测试性。</p>
 * 
 * <p>设计优势：</p>
 * <ul>
 *   <li>最小接口：只暴露必需的方法，遵循最少知识原则</li>
 *   <li>解耦：C4Manager 不需要依赖完整的 Match 类</li>
 *   <li>可测试：可以轻松创建 mock 实现进行单元测试</li>
 *   <li>灵活性：可以在不修改 C4Manager 的情况下更改 Match 内部实现</li>
 * </ul>
 */
public interface MatchContext {
    /**
     * 获取当前 Minecraft 服务器实例。
     * 
     * @return 服务器实例（不为 null）
     */
    MinecraftServer getServer();
    
    /**
     * 向比赛中的所有玩家广播消息。
     * 
     * @param message 要广播的消息组件（不能为 null）
     */
    void broadcastToAllPlayersInMatch(Component message);
    
    /**
     * 向指定队伍的所有玩家广播消息。
     * 
     * @param message 要广播的消息组件（不能为 null）
     * @param team 目标队伍（"CT" 或 "T"）
     */
    void broadcastToTeam(Component message, String team);
    
    /**
     * 获取所有玩家的统计数据映射。
     * 
     * @return 玩家UUID到统计数据的映射（不为 null）
     */
    Map<UUID, PlayerStats> getPlayerStats();
    
    /**
     * 获取当前回合所有存活玩家的 UUID 集合。
     * 
     * @return 存活玩家的 UUID 集合（不为 null）
     */
    Set<UUID> getAlivePlayers();
    
    /**
     * 结束当前回合。
     * 
     * @param winningTeam 获胜队伍（"CT" 或 "T"）
     * @param reason 获胜原因描述
     */
    void endRound(String winningTeam, String reason);
    
    /**
     * 获取当前回合状态。
     * 
     * @return 回合状态枚举（不为 null）
     */
    Match.RoundState getRoundState();
    
    /**
     * 获取 A 点炸弹区域的边界框。
     * 
     * @return A 点边界框，如果未设置则为 null
     */
    AABB getBombsiteA();
    
    /**
     * 获取 B 点炸弹区域的边界框。
     * 
     * @return B 点边界框，如果未设置则为 null
     */
    AABB getBombsiteB();
    
    /**
     * 检查玩家是否在任何一个炸弹安放区域内。
     * 
     * @param player 要检查的玩家（不能为 null）
     * @return 如果玩家在炸弹区内返回 true
     */
    boolean isPlayerInBombsite(ServerPlayer player);
    
    /**
     * 获取包围比赛所有关键点的边界框。
     * 用于清理掉落物品等操作。
     * 
     * @return 比赛区域边界框，如果无法计算则为 null
     */
    AABB getMatchAreaBoundingBox();
}
