package com.qisumei.csgo.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;

/**
 * Command registration only - delegate implementations to CommandHandlers for better cohesion.
 */
public final class CSCommand {
    private CSCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cs")
            // --- 管理员指令 ---
            // --- [核心修改] 重构 /cs start 命令以优化参数顺序 ---
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    // -> /cs start <name> from <preset> [players <count>]
                    .then(Commands.literal("from")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            // 对应: /cs start <name> from <preset>
                            .executes(context -> CommandHandlers.createMatch(context, StringArgumentType.getString(context, "name"), 10, StringArgumentType.getString(context, "preset_name")))

                            // 对应: /cs start <name> from <preset> players <count>
                            .then(Commands.argument("players", IntegerArgumentType.integer(2))
                                .executes(context -> CommandHandlers.createMatch(context, StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "players"), StringArgumentType.getString(context, "preset_name"))))
                        )
                    )
                    // -> /cs start <name> [players <count>]
                    // 对应: /cs start <name> players <count>
                    .then(Commands.argument("players", IntegerArgumentType.integer(2))
                        .executes(context -> CommandHandlers.createMatch(context, StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "players"), null))
                    )
                    // 对应: /cs start <name>
                    .executes(context -> CommandHandlers.createMatch(context, StringArgumentType.getString(context, "name"), 10, null))
                )
            )
            .then(Commands.literal("match")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("save")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            .executes(CommandHandlers::saveMatchPreset)))
                    .then(Commands.literal("set")
                        .then(Commands.literal("spawnpoint")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> CommandHandlers.setSpawnpoint(context, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> CommandHandlers.setSpawnpoint(context, "T"))))
                        )
                        .then(Commands.literal("bombsite")
                            .then(Commands.literal("A")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context -> CommandHandlers.setBombsite(context, "A"))))) // --- 修正 #1
                            .then(Commands.literal("B")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context -> CommandHandlers.setBombsite(context, "B"))))) // --- 修正 #2
                        )
                        .then(Commands.literal("num").then(Commands.argument("rounds", IntegerArgumentType.integer(2)).executes(CommandHandlers::setNumRounds)))
                        .then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(10)).executes(CommandHandlers::setRoundTime)))
                        .then(Commands.literal("shop")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> CommandHandlers.setShopPos(context, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> CommandHandlers.setShopPos(context, "T"))))
                        )
                    )
                )
            )
            .then(Commands.literal("presets")
                .requires(source -> source.hasPermission(2))
                .executes(CommandHandlers::listPresets)
            )
            .then(Commands.literal("config")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.literal("initialgear")
                        .then(Commands.literal("CT")
                            .executes(context -> CommandHandlers.setInitialGear(context, "CT")) // --- 修正 #3
                        )
                        .then(Commands.literal("T")
                            .executes(context -> CommandHandlers.setInitialGear(context, "T")) // --- 修正 #4
                        )
                    )
                )
            )
            .then(Commands.literal("began")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(context -> CommandHandlers.beganMatch(context, false))
                    .then(Commands.literal("yes").executes(context -> CommandHandlers.beganMatch(context, true)))
                )
            )
            .then(Commands.literal("end")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CommandHandlers::endMatch)
                )
            )
            .then(Commands.literal("player")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("quit")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(CommandHandlers::kickPlayer)
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .executes(context -> CommandHandlers.listMatches(context.getSource()))
            )
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
                   .executes(CommandHandlers::joinMatch)

                )
            )

            // --- 玩家指令 ---
            .then(Commands.literal("balance")
                .executes(CommandHandlers::checkBalance)
            )

            // --- watch 命令 ---
            .then(Commands.literal("watch")
                .then(Commands.argument("name", StringArgumentType.string())
                    // -> /cs watch <比赛名称>
                    .executes(CommandHandlers::watchMatch)
                    // -> /cs watch <比赛名称> quit
                    .then(Commands.literal("quit")
                        .executes(CommandHandlers::unwatchMatch)
                    )
                )
            )
        );
    }
}
