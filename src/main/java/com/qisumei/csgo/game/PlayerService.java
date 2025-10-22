package com.qisumei.csgo.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 抽象玩家相关操作的服务接口，便于注入不同实现以降低耦合。
 */
public interface PlayerService {
    void performSelectiveClear(ServerPlayer player);
    void giveInitialGear(ServerPlayer player, String team);
    List<ItemStack> capturePlayerGear(ServerPlayer player);
}

