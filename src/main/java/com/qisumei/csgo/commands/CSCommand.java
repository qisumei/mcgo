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
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * CSGO 模组的指令注册与处理类。
 * <p>
 * 这个类负责定义所有以 {@code /cs} 开头的指令，包括比赛管理、玩家操作和配置设定等。
 * 所有指令逻辑都被分发到各自的私有方法中进行处理，以保持注册方法的清晰性。
 * </p>
 *
 * @author Qisumei
 */
public final class CSCommand {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private CSCommand() {}

    /**
     * 注册所有 {@code /cs} 指令。
     *
     * @param dispatcher 命令调度器。
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cs")
            // --- 比赛生命周期管理指令 (管理员权限) ---
            .then(Commands.literal("start")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    // -> /cs start <name>
                    .executes(ctx -> executeStartMatch(ctx, 10, null))
                    // -> /cs start <name> from <preset>
                    .then(Commands.literal("from")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            // -> /cs start <name> from <preset>
                            .executes(ctx -> executeStartMatch(ctx, 10, StringArgumentType.getString(ctx, "preset_name")))
                            // -> /cs start <name> from <preset> players <count>
                            .then(Commands.argument("players", IntegerArgumentType.integer(2))
                                .executes(ctx -> executeStartMatch(ctx, IntegerArgumentType.getInteger(ctx, "players"), StringArgumentType.getString(ctx, "preset_name"))))))
                    // -> /cs start <name> players <count>
                    .then(Commands.argument("players", IntegerArgumentType.integer(2))
                        .executes(ctx -> executeStartMatch(ctx, IntegerArgumentType.getInteger(ctx, "players"), null)))))
            .then(Commands.literal("began")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> executeBeganMatch(ctx, false))
                    .then(Commands.literal("yes").executes(ctx -> executeBeganMatch(ctx, true)))))
            .then(Commands.literal("end")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CSCommand::executeEndMatch)))

            // --- 比赛与地图设置指令 (管理员权限) ---
            .then(Commands.literal("match")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("set")
                        .then(Commands.literal("spawnpoint")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> executeSetSpawnpoint(ctx, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> executeSetSpawnpoint(ctx, "T")))))
                        .then(Commands.literal("bombsite")
                            .then(Commands.literal("A").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(ctx -> executeSetBombsite(ctx, "A")))))
                            .then(Commands.literal("B").then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(ctx -> executeSetBombsite(ctx, "B"))))))
                        .then(Commands.literal("num").then(Commands.argument("rounds", IntegerArgumentType.integer(2)).executes(CSCommand::executeSetNumRounds)))
                        .then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(10)).executes(CSCommand::executeSetRoundTime)))
                        .then(Commands.literal("shop")
                            .then(Commands.literal("CT").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> executeSetShopPos(ctx, "CT"))))
                            .then(Commands.literal("T").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> executeSetShopPos(ctx, "T"))))))
                    .then(Commands.literal("save")
                        .then(Commands.argument("preset_name", StringArgumentType.string())
                            .executes(CSCommand::executeSaveMatchPreset)))))
            .then(Commands.literal("presets")
                .requires(source -> source.hasPermission(2))
                .executes(CSCommand::executeListPresets))
            .then(Commands.literal("config")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.literal("initialgear")
                        .then(Commands.literal("CT").executes(ctx -> executeSetInitialGear(ctx, "CT")))
                        .then(Commands.literal("T").executes(ctx -> executeSetInitialGear(ctx, "T"))))))

            // --- 通用与玩家指令 ---
            .then(Commands.literal("list").executes(CSCommand::executeListMatches))
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CSCommand::executeJoinMatch)))
            .then(Commands.literal("player")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.literal("quit")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(CSCommand::executeKickPlayer)))))
            .then(Commands.literal("watch")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CSCommand::executeWatchMatch)
                    .then(Commands.literal("quit")
                        .executes(CSCommand::executeUnwatchMatch))))
        );
    }

    /**
     * 执行 `/cs start` 指令，创建一个新的比赛。
     *
     * @param context    指令上下文。
     * @param maxPlayers 比赛最大玩家数。
     * @param presetName 要加载的预设名称，可为 null。
     * @return 指令执行结果。
     */
    private static int executeStartMatch(CommandContext<CommandSourceStack> context, int maxPlayers, String presetName) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");

        if (MatchManager.getMatch(matchName) != null) {
            source.sendFailure(Component.literal("错误：同名比赛 '" + matchName + "' 已存在！"));
            return 0;
        }

        if (!MatchManager.createMatch(matchName, maxPlayers, source.getServer())) {
            source.sendFailure(Component.literal("错误：创建比赛 '" + matchName + "' 失败！"));
            return 0;
        }

        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("严重错误：比赛实例创建后无法找到！"));
            return 0;
        }

        // 如果提供了预设名称，则尝试加载并应用
        if (presetName != null) {
            MatchPreset preset = PresetManager.loadPreset(presetName, source.getServer());
            if (preset == null) {
                source.sendFailure(Component.literal("错误：找不到名为 '" + presetName + "' 的预设！比赛创建已取消。"));
                MatchManager.removeMatch(matchName); // 清理创建失败的比赛
                return 0;
            }
            match.applyPreset(preset);
            source.sendSuccess(() -> Component.literal("已从预设 '" + presetName + "' 创建比赛 '" + matchName + "'！"), true);
        } else {
            source.sendSuccess(() -> Component.literal("比赛 '" + matchName + "' 已创建！最大人数: " + maxPlayers), true);
        }

        // 创建游戏内队伍
        executeServerCommand(source, "team add " + match.getCtTeamName() + " '{\"text\":\"Counter-Terrorists\",\"color\":\"blue\"}'");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);
        executeServerCommand(source, "team add " + match.getTTeamName() + " '{\"text\":\"Terrorists\",\"color\":\"gold\"}'");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);

        return 1;
    }
    
    /**
     * 执行 `/cs began` 指令，正式开始一场处于准备阶段的比赛。
     *
     * @param context 指令上下文。
     * @param force   是否强制开始（忽略人数不足或队伍不平衡的警告）。
     * @return 指令执行结果。
     */
    private static int executeBeganMatch(CommandContext<CommandSourceStack> context, boolean force) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);

        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        if (match.getState() != Match.MatchState.PREPARING) {
            source.sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已经开始或已结束。"));
            return 0;
        }

        // 检查开始条件
        if (!force) {
            if (match.getPlayerCount() < 2) {
                source.sendFailure(Component.literal("警告：比赛至少需要2名玩家才能开始！"));
                sendForceStartMessage(source, matchName);
                return 0;
            }
            if (Math.abs(match.getCtCount() - match.getTCount()) > 1) {
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
            source.sendFailure(Component.literal("开始比赛时发生内部错误，请检查服务器日志！"));
            match.forceEnd();
        }
        return 1;
    }
    
    /**
     * 执行 `/cs end` 指令，强制结束一场比赛。
     *
     * @param context 指令上下文。
     * @return 指令执行结果。
     */
    private static int executeEndMatch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        match.forceEnd();
        source.sendSuccess(() -> Component.literal("已强制结束并清理了比赛 '" + matchName + "'。"), true);
        return 1;
    }
    
    /**
     * 执行 `/cs match <name> set spawnpoint <team> <pos>` 指令。
     *
     * @param context 指令上下文。
     * @param team    要设置出生点的队伍 ("CT" 或 "T")。
     * @return 指令执行结果。
     * @throws CommandSyntaxException 如果坐标参数无效。
     */
    private static int executeSetSpawnpoint(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if ("CT".equals(team)) match.addCtSpawn(pos);
        else match.addTSpawn(pos);
        source.sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 添加 " + team + " 方出生点: " + pos.toShortString()), true);
        return 1;
    }
    
    /**
     * 执行 `/cs match <name> set bombsite <A|B> <from> <to>` 指令。
     *
     * @param context 指令上下文。
     * @param site    要设置的包点 ("A" 或 "B")。
     * @return 指令执行结果。
     * @throws CommandSyntaxException 如果坐标参数无效。
     */
    private static int executeSetBombsite(CommandContext<CommandSourceStack> context, String site) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");

        // [修复] AABB 构造函数 API 变更
        // 旧的 AABB(BlockPos, BlockPos) 构造函数已不存在。
        // 新方法是提供最小和最大的x, y, z坐标来创建一个边界框。
        // 为了确保区域完全包含 'to' 方块，我们需要在最大坐标上加1。
        AABB area = new AABB(
            Math.min(from.getX(), to.getX()),
            Math.min(from.getY(), to.getY()),
            Math.min(from.getZ(), to.getZ()),
            Math.max(from.getX(), to.getX()) + 1.0,
            Math.max(from.getY(), to.getY()) + 1.0,
            Math.max(from.getZ(), to.getZ()) + 1.0
        );

        if ("A".equals(site)) match.setBombsiteA(area);
        else match.setBombsiteB(area);
        source.sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 设置 " + site + " 包点区域"), true);
        return 1;
    }
    
    // ... 其他 execute 方法 ...

    /**
     * 执行 `/cs join` 指令，让玩家加入一场比赛。
     *
     * @param context 指令上下文。
     * @return 指令执行结果。
     * @throws CommandSyntaxException 如果指令不是由玩家执行的。
     */
    private static int executeJoinMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
        if (match.getPlayerCount() >= match.getMaxPlayers()) {
            context.getSource().sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已满员。"));
            return 0;
        }

        // 自动平衡队伍
        String teamToJoin = (match.getCtCount() <= match.getTCount()) ? "CT" : "T";
        String teamName = "CT".equals(teamToJoin) ? match.getCtTeamName() : match.getTTeamName();

        executeServerCommand(context.getSource(), "team join " + teamName + " " + player.getName().getString());
        match.addPlayer(player, teamToJoin);

        Component teamComponent = "CT".equals(teamToJoin) ? Component.literal("反恐精英").withStyle(ChatFormatting.BLUE) : Component.literal("恐怖分子").withStyle(ChatFormatting.GOLD);
        context.getSource().sendSuccess(() -> Component.literal("你已成功加入比赛 '").append(matchName).append("'，阵营为 ").append(teamComponent), false);
        return 1;
    }
    
    // ... 其他方法 ...

    /**
     * 辅助方法，用于在服务器端执行一条指令。
     *
     * @param source  指令源。
     * @param command 要执行的指令字符串。
     */
    private static void executeServerCommand(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source.getServer().createCommandSourceStack(), command);
    }
    
    /**
     * 辅助方法，用于向需要强制开始的玩家发送提示信息。
     *
     * @param source    指令源。
     * @param matchName 比赛名称。
     */
    private static void sendForceStartMessage(CommandSourceStack source, String matchName) {
        source.sendSystemMessage(Component.literal("如果仍要强制开始，请使用 ").append(Component.literal("/cs began " + matchName + " yes").withStyle(ChatFormatting.AQUA)));
    }
    
        private static int executeSetNumRounds(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int rounds = IntegerArgumentType.getInteger(context, "rounds");
        if (rounds % 2 != 0) {
            source.sendFailure(Component.literal("错误：回合数必须为偶数！"));
            return 0;
        }
        match.setTotalRounds(rounds);
        source.sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的总回合数设置为 " + rounds), true);
        return 1;
    }

    private static int executeSetRoundTime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        match.setRoundTimeSeconds(seconds);
        source.sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的回合时间设置为 " + seconds + " 秒"), true);
        return 1;
    }

    private static int executeSetShopPos(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);
        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if ("CT".equals(team)) match.setCtShopPos(pos);
        else match.setTShopPos(pos);
        source.sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 设置 " + team + " 方商店位置: " + pos.toShortString()), true);
        return 1;
    }

    private static int executeSaveMatchPreset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        String presetName = StringArgumentType.getString(context, "preset_name");
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

    private static int executeListPresets(CommandContext<CommandSourceStack> context) {
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

    private static int executeSetInitialGear(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("错误：你的主手上没有物品！"));
            return 0;
        }
        // 使用更新后的工具方法
        String itemIdString = ItemNBTHelper.toCommandString(heldItem, context.getSource().registryAccess());
        if (itemIdString.isEmpty()) {
            context.getSource().sendFailure(Component.literal("错误：无法识别你手中的物品。"));
            return 0;
        }
        List<String> newGearList = List.of(itemIdString);

        if ("CT".equals(team)) {
            ServerConfig.CT_PISTOL_ROUND_GEAR_SPEC.set(newGearList);
        } else {
            ServerConfig.T_PISTOL_ROUND_GEAR_SPEC.set(newGearList);
        }
        // 注意: set() 只在内存中修改，需要游戏重启或配置重载命令来保存到文件
        context.getSource().sendSuccess(() -> Component.literal(team + " 方的手枪局初始装备已设置为: ").append(heldItem.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int executeListMatches(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
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

    private static int executeKickPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = StringArgumentType.getString(context, "name");
        ServerPlayer playerToKick = EntityArgument.getPlayer(context, "player");
        Match match = MatchManager.getMatch(matchName);
        if (match == null || !match.getPlayerStats().containsKey(playerToKick.getUUID())) {
            source.sendFailure(Component.literal("错误：玩家 " + playerToKick.getName().getString() + " 不在该比赛中。"));
            return 0;
        }

        executeServerCommand(source, "team leave " + playerToKick.getName().getString());
        match.removePlayer(playerToKick);

        playerToKick.setGameMode(GameType.SURVIVAL);
        playerToKick.removeAllEffects();
        executeServerCommand(source, "attribute " + playerToKick.getName().getString() + " minecraft:generic.knockback_resistance base set 0.0");
        playerToKick.getInventory().clearContent();
        BlockPos spawnPos = source.getServer().overworld().getSharedSpawnPos();
        playerToKick.teleportTo(source.getServer().overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);

        source.sendSuccess(() -> Component.literal("已将玩家 " + playerToKick.getName().getString() + " 移出比赛 '" + matchName + "'。"), true);
        playerToKick.sendSystemMessage(Component.literal("你已被管理员移出比赛。").withStyle(ChatFormatting.RED));
        return 1;
    }
    
    private static int executeWatchMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer spectator = context.getSource().getPlayerOrException();
        String matchName = StringArgumentType.getString(context, "name");
        Match match = MatchManager.getMatch(matchName);

        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        List<ServerPlayer> alivePlayers = match.getAlivePlayers().stream()
                .map(uuid -> context.getSource().getServer().getPlayerList().getPlayer(uuid))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("错误：比赛 '" + matchName + "' 中当前没有可观战的存活玩家。"));
            return 0;
        }

        ServerPlayer target = alivePlayers.get(new Random().nextInt(alivePlayers.size()));
        spectator.setGameMode(GameType.SPECTATOR);
        spectator.setCamera(target);

        context.getSource().sendSuccess(() -> Component.literal("你现在正在观战比赛 '").append(matchName).append("'. 正在跟随玩家 ").append(target.getDisplayName()), false);
        return 1;
    }

    private static int executeUnwatchMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer spectator = context.getSource().getPlayerOrException();
        spectator.setGameMode(GameType.SURVIVAL);
        BlockPos spawnPos = context.getSource().getServer().overworld().getSharedSpawnPos();
        spectator.teleportTo(context.getSource().getServer().overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        context.getSource().sendSuccess(() -> Component.literal("你已退出观战模式。"), false);
        return 1;
    }
}