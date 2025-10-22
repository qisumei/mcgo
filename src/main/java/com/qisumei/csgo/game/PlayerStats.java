package com.qisumei.csgo.game;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家统计数据类
 * 用于跟踪和管理CSGO游戏中玩家的各种统计信息
 * 
 * 注意：由于需要支持可变状态（如击杀数、死亡数会在比赛过程中变化），
 * 这里保留为普通类而非record。record适用于不可变数据传输对象。
 */
public final class PlayerStats {
    private String team;
    private int consecutiveLosses;
    private int kills;
    private int deaths;
    private final List<ItemStack> roundGear;

    /**
     * 构造函数，初始化玩家统计数据
     * 
     * @param team 玩家所属队伍，不能为null
     * @throws IllegalArgumentException 如果team为null
     */
    public PlayerStats(String team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        this.team = team;
        this.consecutiveLosses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.roundGear = new ArrayList<>();
    }

    /**
     * 获取玩家所属队伍
     * 
     * @return 队伍名称，永不为null
     */
    public String getTeam() {
        return team;
    }

    /**
     * 设置玩家所属队伍
     * 
     * @param team 队伍名称，不能为null
     * @throws IllegalArgumentException 如果team为null
     */
    public void setTeam(String team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null");
        }
        this.team = team;
    }

    /**
     * 获取连续失败次数
     * 
     * @return 连续失败次数，始终非负
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
     * 重置连续失败次数为0
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
     * 
     * @return 击杀数量，始终非负
     */
    public int getKills() {
        return kills;
    }

    /**
     * 获取死亡数
     * 
     * @return 死亡数量，始终非负
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * 获取回合内装备列表的不可变视图
     * 
     * @return 装备物品栈列表，永不为null
     */
    public List<ItemStack> getRoundGear() {
        return List.copyOf(roundGear);
    }

    /**
     * 设置回合内装备
     * 
     * @param gear 装备物品栈列表，不能为null
     * @throws IllegalArgumentException 如果gear为null
     */
    public void setRoundGear(List<ItemStack> gear) {
        if (gear == null) {
            throw new IllegalArgumentException("Gear list cannot be null");
        }
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
