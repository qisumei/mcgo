package com.qisumei.csgo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;

/**
 * Utility to centralize execution of server commands to avoid duplicated code.
 */
public final class ServerCommands {
    private ServerCommands() { }

    public static void execute(MinecraftServer server, String command) {
        if (server == null || command == null || command.isEmpty()) return;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    public static void execute(CommandSourceStack source, String command) {
        if (source == null || command == null || command.isEmpty()) return;
        source.getServer().getCommands().performPrefixedCommand(source.getServer().createCommandSourceStack(), command);
    }
}

