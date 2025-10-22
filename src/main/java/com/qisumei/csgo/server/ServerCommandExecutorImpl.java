package com.qisumei.csgo.server;

import com.qisumei.csgo.util.ServerCommands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 默认实现，简单委托到现有的 ServerCommands 工具类。
 */
public class ServerCommandExecutorImpl implements ServerCommandExecutor {
    private final MinecraftServer server;

    public ServerCommandExecutorImpl(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void executeGlobal(String command) {
        ServerCommands.execute(server, command);
    }

    @Override
    public void executeForPlayer(ServerPlayer player, String command) {
        // 保持原语义：通过 player.server 执行原始命令（兼容现有用法）
        ServerCommands.execute(player.server, command);
    }
}

