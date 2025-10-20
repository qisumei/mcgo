package com.qisumei.csgo.game;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代表一名玩家在单场CSGO比赛中的所有统计数据和状态。
 * <p>
 * 这个类是一个纯数据容器（Data Class），用于跟踪玩家的队伍、战绩（K/D）、
 * 经济状况（连败次数）以及在一个回合中存活下来时所持有的装备。
 * </p>
 *
 * @author Qisumei
 */
public class PlayerStats {

    private String team;
    private int kills;
    private int deaths;
    private int consecutiveLosses;

    /**
     * 存储玩家在一个回合中存活下来时所持有的装备。
     * 这用于实现CSGO中的 "eco round" 机制，即胜利方幸存者可以在下一回合保留他们的武器。
     */
    private final List<ItemStack> roundGear;

    /**
     * 构造一个新的 PlayerStats 实例。
     *
     * @param team 玩家初始加入的队伍 ("CT" 或 "T")。
     */
    public PlayerStats(String team) {
        this.team = team;
        this.kills = 0;
        this.deaths = 0;
        this.consecutiveLosses = 0;
        this.roundGear = new ArrayList<>();
    }

    /**
     * 获取玩家当前所属的队伍。
     *
     * @return 队伍名称 ("CT" 或 "T")。
     */
    public String getTeam() {
        return team;
    }

    /**
     * 设置玩家的队伍。
     * 主要在半场换边时使用。
     *
     * @param team 新的队伍名称。
     */
    public void setTeam(String team) {
        this.team = team;
    }

    /**
     * 获取玩家当前的击杀数。
     *
     * @return 击杀总数。
     */
    public int getKills() {
        return kills;
    }

    /**
     * 增加一次击杀数。
     */
    public void incrementKills() {
        this.kills++;
    }

    /**
     * 获取玩家当前的死亡数。
     *
     * @return 死亡总数。
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * 增加一次死亡数。
     */
    public void incrementDeaths() {
        this.deaths++;
    }

    /**
     * 获取玩家当前的连败次数。
     *
     * @return 连败次数。
     */
    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }

    /**
     * 增加一次连败次数。
     */
    public void incrementConsecutiveLosses() {
        this.consecutiveLosses++;
    }

    /**
     * 重置连败次数为0。
     * 通常在玩家所在队伍赢得一个回合后调用。
     */
    public void resetConsecutiveLosses() {
        this.consecutiveLosses = 0;
    }

    /**
     * 获取一个不可修改的玩家回合幸存装备列表。
     *
     * @return 幸存装备列表的只读视图。
     */
    public List<ItemStack> getRoundGear() {
        return Collections.unmodifiableList(roundGear);
    }

    /**
     * 设置玩家的回合幸存装备。
     * 这会清空旧列表并用新列表的内容填充。
     *
     * @param gear 新的装备列表。
     */
    public void setRoundGear(List<ItemStack> gear) {
        this.roundGear.clear();
        this.roundGear.addAll(gear);
    }

    /**
     * 清空玩家的回合幸存装备列表。
     */
    public void clearRoundGear() {
        this.roundGear.clear();
    }
}
