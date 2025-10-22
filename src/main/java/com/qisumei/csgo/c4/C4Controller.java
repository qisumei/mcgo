package com.qisumei.csgo.c4;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * C4 控制器接口，定义 C4 炸弹系统的核心行为。
 */
public interface C4Controller {
    /**
     * 每个服务器 tick 调用，用于更新 C4 相关状态。
     */
    void tick();
    
    /**
     * 重置所有 C4 相关状态（通常在回合开始时调用）。
     */
    void reset();
    
    /**
     * 当 C4 被安放时调用。
     * 
     * @param pos C4 安放的位置（不能为 null）
     */
    void onC4Planted(BlockPos pos);
    
    /**
     * 当 C4 被拆除时调用。
     * 
     * @param defuser 拆除 C4 的玩家（可以为 null）
     */
    void onC4Defused(ServerPlayer defuser);
    
    /**
     * 当 C4 爆炸时调用。
     */
    void onC4Exploded();
    
    /**
     * 随机给一名 T 队玩家发放 C4。
     */
    void giveC4ToRandomT();
    
    /**
     * 检查 C4 是否已被安放。
     * 
     * @return 如果 C4 已安放返回 true
     */
    boolean isC4Planted();
    
    /**
     * 获取 C4 的安放位置。
     * 
     * @return C4 位置，如果未安放则为 null
     */
    BlockPos getC4Pos();
    
    /**
     * 获取 C4 爆炸倒计时剩余 tick 数。
     * 
     * @return 剩余 tick 数
     */
    int getC4TicksLeft();
    
    /**
     * 在玩家 tick 时调用，用于处理拆弹等与玩家相关的逻辑。
     * 
     * @param player 当前 tick 的玩家（不能为 null）
     */
    void handlePlayerTick(ServerPlayer player);
}
