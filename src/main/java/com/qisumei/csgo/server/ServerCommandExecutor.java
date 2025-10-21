package com.qisumei.csgo.server;

import net.minecraft.server.level.ServerPlayer;

/**
 * 抽象化的服务器命令执行器，封装对控制台/玩家命令执行的调用。
 * 这样可以在未来替换实现、便于单元测试并降低对具体工具类的耦合。
 */
public interface ServerCommandExecutor {
    /**
     * 在服务器上下文中执行一个全局命令（如 tellraw @a ...）。
     * @param command 要执行的命令字符串
     */
    void executeGlobal(String command);

    /**
     * 在服务器上下文中为指定玩家执行命令（通常效果等同于由服务器执行但目标是某玩家）。
     * @param player 目标玩家
     * @param command 要执行的命令字符串
     */
    void executeForPlayer(ServerPlayer player, String command);
}

