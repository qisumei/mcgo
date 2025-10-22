package com.qisumei.csgo.game;

import com.qisumei.csgo.server.ServerCommandExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * PlayerService 的默认实现。
 */
public final class MatchPlayerService implements PlayerService {
    private final ServerCommandExecutor commandExecutor;

    /**
     * 构造一个 MatchPlayerService 实例。
     * 
     * @param commandExecutor 服务器命令执行器（不能为 null）
     * @throws NullPointerException 如果 commandExecutor 为 null
     */
    public MatchPlayerService(ServerCommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "CommandExecutor cannot be null");
    }

    @Override
    public void performSelectiveClear(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        MatchPlayerHelper.performSelectiveClear(player);
    }

    @Override
    public void giveInitialGear(ServerPlayer player, String team) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(team, "Team cannot be null");
        // 原有逻辑需要一个命令执行器来发放物品
        MatchPlayerHelper.giveInitialGear(player, team, this.commandExecutor);
    }

    @Override
    public List<ItemStack> capturePlayerGear(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return MatchPlayerHelper.capturePlayerGear(player);
    }
}
