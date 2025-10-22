package com.qisumei.csgo.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface EconomyService {
    void giveMoney(ServerPlayer player, int amount);
    int getRewardForKill(ItemStack weapon);
}

