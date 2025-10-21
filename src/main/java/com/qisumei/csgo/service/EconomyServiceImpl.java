package com.qisumei.csgo.service;

import com.qisumei.csgo.game.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 默认 EconomyService 实现，临时委托给原有的 EconomyManager 静态方法以保证兼容性。
 */
public class EconomyServiceImpl implements EconomyService {
    @Override
    public void giveMoney(ServerPlayer player, int amount) {
        EconomyManager.giveMoney(player, amount);
    }

    @Override
    public int getRewardForKill(ItemStack weapon) {
        return EconomyManager.getRewardForKill(weapon);
    }
}

