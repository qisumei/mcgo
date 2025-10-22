package com.qisumei.csgo.service;

import com.qisumei.csgo.game.EconomyManager;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * 兼容适配器：集中处理服务优先 / 静态管理器回退的逻辑。
 * 目标：逐步将代码迁移到基于 Service 的调用，而无需一次性修改大型类。
 */
@SuppressWarnings("unused")
public final class ServiceFallbacks {
    private ServiceFallbacks() {}

    // --- Economy ---
    // 【修复】直接调用 EconomyManager 避免与 EconomyServiceImpl 形成递归调用环
    // EconomyServiceImpl 本身会调用 EconomyManager，所以这里不应该再委托给 EconomyService
    public static void giveMoney(ServerPlayer player, int amount) {
        EconomyManager.giveMoney(player, amount);
    }

    public static int getRewardForKill(ItemStack weapon) {
        return EconomyManager.getRewardForKill(weapon);
    }

    // --- Match ---
    public static boolean createMatch(String name, int maxPlayers, MinecraftServer server) {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) return ms.createMatch(name, maxPlayers, server);
        return MatchManager.createMatch(name, maxPlayers, server);
    }

    public static com.qisumei.csgo.game.Match getMatch(String name) {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) return ms.getMatch(name);
        return MatchManager.getMatch(name);
    }

    public static Collection<com.qisumei.csgo.game.Match> getAllMatches() {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) return ms.getAllMatches();
        return MatchManager.getAllMatches();
    }

    public static com.qisumei.csgo.game.Match getPlayerMatch(ServerPlayer player) {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) return ms.getPlayerMatch(player);
        return MatchManager.getPlayerMatch(player);
    }

    public static com.qisumei.csgo.game.Match getMatchFromC4Pos(net.minecraft.core.BlockPos pos) {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) return ms.getMatchFromC4Pos(pos);
        return MatchManager.getMatchFromC4Pos(pos);
    }

    public static void removeMatch(String name) {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) ms.removeMatch(name);
        else MatchManager.removeMatch(name);
    }

    public static void tickMatches() {
        MatchService ms = ServiceRegistry.get(MatchService.class);
        if (ms != null) ms.tick();
        else MatchManager.tick();
    }
}
