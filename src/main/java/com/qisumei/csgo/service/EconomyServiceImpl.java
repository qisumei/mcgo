package com.qisumei.csgo.service;

import com.qisumei.csgo.game.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 默认 EconomyService 实现，委托给 EconomyManager 静态方法以保证向后兼容性。
 * 这种设计允许在不修改现有代码的情况下替换经济系统实现。
 */
public class EconomyServiceImpl implements EconomyService {
    @Override
    public void giveMoney(ServerPlayer player, int amount) {
        EconomyManager.giveMoney(player, amount);
    }
    
    @Override
    public void setMoney(ServerPlayer player, int amount) {
        EconomyManager.setMoney(player, amount);
    }

    @Override
    public int getRewardForKill(ItemStack weapon) {
        return EconomyManager.getRewardForKill(weapon);
    }
}

