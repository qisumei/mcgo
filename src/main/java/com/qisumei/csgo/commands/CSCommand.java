package com.qisumei.csgo.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.preset.MatchPreset;
import com.qisumei.csgo.game.preset.PresetManager;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * CSCommand 类用于注册和处理与CSGO比赛相关的命令。
 * 包括创建比赛、设置比赛参数、加入比赛、开始比赛、结束比赛等操作。
 */
public class CSCommand {

    /**
     * 注册所有与CSGO相关的命令。
     *
     * @param dispatcher 命令调度器，用于注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cs")
            // --- 管理员指令 ---
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("from")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            .executes(context -> createMatch(context, StringArgumentType.getString(context, "name"), 10, StringArgumentType.getString(context, "preset_name")))))
                    .then(Commands.argument("players", IntegerArgumentType.integer(2))
                        .executes(context -> createMatch(context, StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "players"), null)))
                    .executes(context -> createMatch(context, StringArgumentType.getString(context, "name"), 10, null))
                )
            )
            .then(Commands.literal("match")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("save")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            .executes(CSCommand::saveMatchPreset)))
                    .then(Commands.literal("set")
                        .then(Commands.literal("spawnpoint")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> setSpawnpoint(context, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> setSpawnpoint(context, "T"))))
                        )
                        .then(Commands.literal("bombsite")
                            .then(Commands.literal("A")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context -> setBombsite(context, "A"))))) // --- 修正 #1
                            .then(Commands.literal("B")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context -> setBombsite(context, "B"))))) // --- 修正 #2
                        )
                        .then(Commands.literal("num").then(Commands.argument("rounds", IntegerArgumentType.integer(2)).executes(CSCommand::setNumRounds)))
                        .then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(10)).executes(CSCommand::setRoundTime)))
                        .then(Commands.literal("shop")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> setShopPos(context, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> setShopPos(context, "T"))))
                        )
                    )
                )
            )
            .then(Commands.literal("presets")
                .requires(source -> source.hasPermission(2))
                .executes(CSCommand::listPresets)
            )
            .then(Commands.literal("config")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.literal("initialgear")
                        .then(Commands.literal("CT")
                            .executes(context -> setInitialGear(context, "CT")) // --- 修正 #3
                        )
                        .then(Commands.literal("T")
                            .executes(context -> setInitialGear(context, "T")) // --- 修正 #4
                        )
                    )
                )
            )
            .then(Commands.literal("began")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(context -> beganMatch(context, false))
                    .then(Commands.literal("yes").executes(context -> beganMatch(context, true)))
                )
            )
            .then(Commands.literal("end")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CSCommand::endMatch)
                )
            )
            .then(Commands.literal("player")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("quit")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(CSCommand::kickPlayer)
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .executes(context -> listMatches(context.getSource()))
            )
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(context -> joinMatch(context))
                )
            )
        );
    }

    /**
     * 创建一个新的比赛。
     *
     * @param context 命令上下文
     * @param name 比赛名称
     * @param maxPlayers 最大玩家数量
     * @param presetName 预设名称（可选）
     * @return 执行结果代码
     */
    private static int createMatch(CommandContext<CommandSourceStack> context, String name, int maxPlayers, String presetName) {
        CommandSourceStack source = context.getSource();

        if (MatchManager.getMatch(name) != null) {
            source.sendFailure(Component.literal("错误：同名比赛 '" + name + "' 已存在！"));
            return 0;
        }

        if (!MatchManager.createMatch(name, maxPlayers, source.getServer())) {
            source.sendFailure(Component.literal("错误：创建比赛 '" + name + "' 失败！"));
            return 0;
        }

        Match match = MatchManager.getMatch(name);

        if (presetName != null) {
            MatchPreset preset = PresetManager.loadPreset(presetName, source.getServer());
            if (preset == null) {
                source.sendFailure(Component.literal("错误：找不到名为 '" + presetName + "' 的预设！"));
                MatchManager.removeMatch(name);
                return 0;
            }
            match.applyPreset(preset);
            source.sendSuccess(() -> Component.literal("已从预设 '" + presetName + "' 创建比赛 '" + name + "'！"), true);
        } else {
            source.sendSuccess(() -> Component.literal("比赛 '" + name + "' 已创建！最大人数: " + maxPlayers), true);
        }

        executeServerCommand(source, "team add " + match.getCtTeamName() + " '{\"text\":\"Counter-Terrorists\"}'");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " color blue");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " nametagVisibility hideForOtherTeams");

        executeServerCommand(source, "team add " + match.getTTeamName() + " '{\"text\":\"Terrorists\"}'");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " color gold");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);
        executeServerCommand(source, "team modify " + match.getTTeamName() + " nametagVisibility hideForOtherTeams");

        return 1;
    }

    /**
     * 将当前比赛保存为预设。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int saveMatchPreset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        String presetName = StringArgumentType.getString(context, "preset_name");
        CommandSourceStack source = context.getSource();
        Match match = MatchManager.getMatch(matchName);

        if (match == null) {
            source.sendFailure(Component.literal("错误：找不到名为 '" + matchName + "' 的比赛来保存。"));
            return 0;
        }

        MatchPreset preset = match.toPreset();
        if (PresetManager.savePreset(preset, presetName, source.getServer())) {
            source.sendSuccess(() -> Component.literal("成功将比赛 '" + matchName + "' 的设置保存为预设 '" + presetName + "'！"), true);
        } else {
            source.sendFailure(Component.literal("错误：保存预设时发生问题，请检查服务器日志。"));
        }
        return 1;
    }

    /**
     * 列出所有已保存的比赛预设。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     */
    private static int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> presets = PresetManager.listPresets(source.getServer());
        if (presets.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有已保存的比赛预设。"), false);
            return 1;
        }
        MutableComponent message = Component.literal("--- 已保存的预设列表 ---");
        presets.forEach(name -> message.append("\n - ").append(Component.literal(name).withStyle(ChatFormatting.GREEN)));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    /**
     * 玩家加入比赛。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int joinMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        if (match.getState() != Match.MatchState.PREPARING) {
            context.getSource().sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已经开始或已结束。"));
            return 0;
        }
        if (MatchManager.getPlayerMatch(player) != null) {
            context.getSource().sendFailure(Component.literal("错误：你已经在一场比赛中了。"));
            return 0;
        }
        if (match.getPlayerCount() >= 10) { // Assuming max players 10
            context.getSource().sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已满员。"));
            return 0;
        }
        String teamToJoin;
        if (match.getCtCount() < match.getTCount()) {
            teamToJoin = "CT";
        } else if (match.getTCount() < match.getCtCount()) {
            teamToJoin = "T";
        } else {
            teamToJoin = new Random().nextBoolean() ? "CT" : "T";
        }
        String teamName = teamToJoin.equals("CT") ? match.getCtTeamName() : match.getTTeamName();
        executeServerCommand(context.getSource(), "team join " + teamName + " " + player.getName().getString());
        match.addPlayer(player, teamToJoin);
        Component teamComponent = teamToJoin.equals("CT") ? Component.literal("反恐精英").withStyle(ChatFormatting.BLUE) : Component.literal("恐怖分子").withStyle(ChatFormatting.GOLD);
        context.getSource().sendSuccess(() -> Component.literal("你已成功加入比赛 '").append(matchName).append("'，阵营为 ").append(teamComponent), false);
        return 1;
    }

    /**
     * 列出所有比赛。
     *
     * @param source 命令源
     * @return 执行结果代码
     */
    private static int listMatches(CommandSourceStack source) {
        var matches = MatchManager.getAllMatches();
        if (matches.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有正在准备或进行的比赛。"), false);
            return 1;
        }
        MutableComponent message = Component.literal("--- 比赛列表 ---");
        for (Match match : matches) {
            MutableComponent matchLine = Component.literal("\n - " + match.getName());
            if (match.getState() == Match.MatchState.PREPARING) {
                matchLine.append(Component.literal(" [准备中]").withStyle(ChatFormatting.GREEN));
            } else if (match.getState() == Match.MatchState.IN_PROGRESS) {
                matchLine.append(Component.literal(" [进行中]").withStyle(ChatFormatting.RED));
            }
            message.append(matchLine);
        }
        source.sendSuccess(() -> message, false);
        return 1;
    }

    /**
     * 开始比赛。
     *
     * @param context 命令上下文
     * @param force 是否强制开始
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int beganMatch(CommandContext<CommandSourceStack> context, boolean force) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        CommandSourceStack source = context.getSource();
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        if (match.getState() != Match.MatchState.PREPARING) {
            source.sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已经开始或已结束。"));
            return 0;
        }
        boolean isTeamBalanced = Math.abs(match.getCtCount() - match.getTCount()) <= 1;
        boolean hasEnoughPlayers = match.getPlayerCount() >= 2;
        if (!force) {
            if (!hasEnoughPlayers) {
                source.sendFailure(Component.literal("警告：比赛至少需要2名玩家才能开始！"));
                sendForceStartMessage(source, matchName);
                return 0;
            }
            if (!isTeamBalanced) {
                source.sendFailure(Component.literal("警告：队伍人数不平衡！"));
                sendForceStartMessage(source, matchName);
                return 0;
            }
        }

        try {
            match.start();
            Component startMessage = Component.literal("比赛 '").append(Component.literal(matchName).withStyle(ChatFormatting.YELLOW)).append("' 已由管理员正式开始！");
            source.getServer().getPlayerList().broadcastSystemMessage(startMessage, false);
        } catch (Exception e) {
            QisCSGO.LOGGER.error("开始比赛 '{}' 时发生严重错误:", matchName, e);
            source.sendFailure(Component.literal("开始比赛时发生内部错误，请检查服务器日志获取详细信息！"));
            match.forceEnd();
            MatchManager.removeMatch(matchName);
        }
        return 1;
    }

    /**
     * 发送强制开始比赛的提示信息。
     *
     * @param source 命令源
     * @param matchName 比赛名称
     */
    private static void sendForceStartMessage(CommandSourceStack source, String matchName) {
        source.sendSystemMessage(Component.literal("如果仍要强制开始，请使用 ").append(Component.literal("/cs began " + matchName + " yes").withStyle(ChatFormatting.AQUA)));
    }

    /**
     * 将玩家踢出比赛。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int kickPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        ServerPlayer playerToKick = EntityArgument.getPlayer(context, "player");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        if (!match.getPlayerStats().containsKey(playerToKick.getUUID())) {
            source.sendFailure(Component.literal("错误：玩家 " + playerToKick.getName().getString() + " 不在该比赛中。"));
            return 0;
        }
        executeServerCommand(source, "team leave " + playerToKick.getName().getString());
        match.removePlayer(playerToKick);
        source.sendSuccess(() -> Component.literal("已将玩家 " + playerToKick.getName().getString() + " 移出比赛 '" + matchName + "'。"), true);
        playerToKick.sendSystemMessage(Component.literal("你已被管理员移出比赛。").withStyle(ChatFormatting.RED));
        return 1;
    }

    /**
     * 设置比赛的出生点。
     *
     * @param context 命令上下文
     * @param team 队伍名称（CT 或 T）
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setSpawnpoint(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (team.equals("CT")) {
            match.addCtSpawn(pos);
        } else {
            match.addTSpawn(pos);
        }
        context.getSource().sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 添加 " + team + " 方出生点: " + pos.toShortString()), true);
        return 1;
    }

    /**
     * 设置比赛的总回合数。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setNumRounds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int rounds = IntegerArgumentType.getInteger(context, "rounds");
        if (rounds % 2 != 0) {
            context.getSource().sendFailure(Component.literal("错误：回合数必须为偶数！"));
            return 0;
        }
        match.setTotalRounds(rounds);
        context.getSource().sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的总回合数设置为 " + rounds), true);
        return 1;
    }

    /**
     * 设置比赛的回合时间。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setRoundTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        match.setRoundTimeSeconds(seconds);
        context.getSource().sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的回合时间设置为 " + seconds + " 秒"), true);
        return 1;
    }

    /**
     * 设置比赛的商店位置。
     *
     * @param context 命令上下文
     * @param team 队伍名称（CT 或 T）
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setShopPos(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (team.equals("CT")) {
            match.setCtShopPos(pos);
        } else {
            match.setTShopPos(pos);
        }
        context.getSource().sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 设置 " + team + " 方商店位置: " + pos.toShortString()), true);
        return 1;
    }

    /**
     * 设置初始装备。
     *
     * @param context 命令上下文
     * @param team 队伍名称（CT 或 T）
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setInitialGear(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("错误：你的主手上没有物品！"));
            return 0;
        }

        String itemIdString = ItemNBTHelper.itemStackToString(heldItem, context.getSource().registryAccess());

        if (itemIdString.isEmpty()) {
            context.getSource().sendFailure(Component.literal("错误：无法识别你手中的物品。"));
            return 0;
        }

        List<String> newGearList = List.of(itemIdString);

        if ("CT".equals(team)) {
            ServerConfig.CT_PISTOL_ROUND_GEAR_SPEC.set(newGearList);
            ServerConfig.CT_PISTOL_ROUND_GEAR_SPEC.save();
        } else {
            ServerConfig.T_PISTOL_ROUND_GEAR_SPEC.set(newGearList);
            ServerConfig.T_PISTOL_ROUND_GEAR_SPEC.save();
        }

        ServerConfig.bake();

        Component successMessage = Component.literal(team + " 方的手枪局初始装备已设置为: ")
            .append(heldItem.getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
        context.getSource().sendSuccess(() -> successMessage, true);

        return 1;
    }

    /**
     * 设置包点区域。
     *
     * @param context 命令上下文
     * @param site 包点名称（A 或 B）
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int setBombsite(CommandContext<CommandSourceStack> context, String site) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");

        AABB area = new AABB(
            Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()),
            Math.max(from.getX(), to.getX()) + 1, Math.max(from.getY(), to.getY()) + 1, Math.max(from.getZ(), to.getZ()) + 1
        );

        if ("A".equals(site)) {
            match.setBombsiteA(area);
        } else {
            match.setBombsiteB(area);
        }

        context.getSource().sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 设置 " + site + " 包点区域"), true);
        return 1;
    }

    /**
     * 执行服务器命令。
     *
     * @param source 命令源
     * @param command 要执行的命令
     */
    private static void executeServerCommand(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source.getServer().createCommandSourceStack(), command);
    }

    /**
     * 强制结束比赛。
     *
     * @param context 命令上下文
     * @return 执行结果代码
     * @throws CommandSyntaxException 如果命令语法错误
     */
    private static int endMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        CommandSourceStack source = context.getSource();

        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        match.forceEnd();
        MatchManager.removeMatch(matchName);

        executeServerCommand(source, "team remove " + match.getCtTeamName());
        executeServerCommand(source, "team remove " + match.getTTeamName());

        source.sendSuccess(() -> Component.literal("已强制结束并清理了比赛 '" + matchName + "'。"), true);
        return 1;
    }
}
