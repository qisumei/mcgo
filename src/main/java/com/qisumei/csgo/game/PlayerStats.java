package com.qisumei.csgo.game;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家统计数据类
 * 用于跟踪和管理CSGO游戏中玩家的各种统计信息
 */
public class PlayerStats {
    private String team;
    private int consecutiveLosses = 0;
    private int kills = 0;
    private int deaths = 0;

    // --- 新增：用于记录玩家回合内的装备 ---
    private final List<ItemStack> roundGear = new ArrayList<>();

    /**
     * 构造函数，初始化玩家统计数据
     * @param team 玩家所属队伍
     */
    public PlayerStats(String team) {
        this.team = team;
    }

    /**
     * 获取玩家所属队伍
     * @return 队伍名称
     */
    public String getTeam() {
        return team;
    }

    /**
     * 设置玩家所属队伍
     * @param team 队伍名称
     */
    public void setTeam(String team) {
        this.team = team;
    }

    /**
     * 获取连续失败次数
     * @return 连续失败次数
     */
    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }

    /**
     * 增加连续失败次数
     */
    public void incrementConsecutiveLosses() {
        this.consecutiveLosses++;
    }

    /**
     * 重置连续失败次数
     */
    public void resetConsecutiveLosses() {
        this.consecutiveLosses = 0;
    }

    /**
     * 增加击杀数
     */
    public void incrementKills() {
        this.kills++;
    }

    /**
     * 增加死亡数
     */
    public void incrementDeaths() {
        this.deaths++;
    }

    /**
     * 获取击杀数
     * @return 击杀数量
     */
    public int getKills() { return kills; }

    /**
     * 获取死亡数
     * @return 死亡数量
     */
    public int getDeaths() { return deaths; }

    // --- 新增：管理回合装备的方法 ---
    /**
     * 获取回合内装备列表
     * @return 装备物品栈列表
     */
    public List<ItemStack> getRoundGear() {
        return roundGear;
    }

    /**
     * 设置回合内装备
     * @param gear 装备物品栈列表
     */
    public void setRoundGear(List<ItemStack> gear) {
        this.roundGear.clear();
        this.roundGear.addAll(gear);
    }

    /**
     * 清空回合内装备
     */
    public void clearRoundGear() {
        this.roundGear.clear();
    }
}
