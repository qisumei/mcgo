package com.qisumei.csgo.game;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PlayerStats {
    private String team;
    private int consecutiveLosses = 0;
    private int kills = 0;
    private int deaths = 0;

    // --- 新增：用于记录玩家回合内的装备 ---
    private final List<ItemStack> roundGear = new ArrayList<>();

    public PlayerStats(String team) {
        this.team = team;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }

    public void incrementConsecutiveLosses() {
        this.consecutiveLosses++;
    }

    public void resetConsecutiveLosses() {
        this.consecutiveLosses = 0;
    }
    
    public void incrementKills() {
        this.kills++;
    }

    public void incrementDeaths() {
        this.deaths++;
    }
    
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }

    // --- 新增：管理回合装备的方法 ---
    public List<ItemStack> getRoundGear() {
        return roundGear;
    }

    public void setRoundGear(List<ItemStack> gear) {
        this.roundGear.clear();
        this.roundGear.addAll(gear);
    }
    
    public void clearRoundGear() {
        this.roundGear.clear();
    }
}