package com.qisumei.csgo.game;

import com.qisumei.csgo.server.ServerCommandExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 默认的 PlayerService 实现，内部委托到原有的 MatchPlayerHelper，以保证行为不变。
 */
public class MatchPlayerService implements PlayerService {
    private final ServerCommandExecutor commandExecutor;

    public MatchPlayerService(ServerCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void performSelectiveClear(ServerPlayer player) {
        MatchPlayerHelper.performSelectiveClear(player);
    }

    @Override
    public void giveInitialGear(ServerPlayer player, String team) {
        // 原有逻辑需要一个命令执行器来发放物品
        MatchPlayerHelper.giveInitialGear(player, team, this.commandExecutor);
    }

    @Override
    public List<ItemStack> capturePlayerGear(ServerPlayer player) {
        return MatchPlayerHelper.capturePlayerGear(player);
    }
}
