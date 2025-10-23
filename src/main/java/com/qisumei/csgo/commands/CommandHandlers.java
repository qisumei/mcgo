package com.qisumei.csgo.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.preset.MatchPreset;
import com.qisumei.csgo.game.preset.PresetManager;
import com.qisumei.csgo.service.MatchService;
import com.qisumei.csgo.service.ServiceFallbacks;
import com.qisumei.csgo.service.ServiceRegistry;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
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

/**
 * 集中存放各个命令的处理器，降低 `CSCommand` 的职责并提高内聚性。
 */
public final class CommandHandlers {

    private CommandHandlers() { }

    public static int createMatch(CommandContext<CommandSourceStack> context, String name, int maxPlayers, String presetName) {
        CommandSourceStack source = context.getSource();
        // Prefer a registered MatchService if present
        MatchService svc = ServiceRegistry.get(MatchService.class);

        Match existing = ServiceFallbacks.getMatch(name);
        if (existing != null) {
            source.sendFailure(Component.literal("错误：同名比赛 '" + name + "' 已存在！"));
            return 0;
        }

        boolean created = ServiceFallbacks.createMatch(name, maxPlayers, source.getServer());
        if (!created) {
            source.sendFailure(Component.literal("错误：创建比赛 '" + name + "' 失败！"));
            return 0;
        }

        Match match = ServiceFallbacks.getMatch(name);

        if (presetName != null) {
            MatchPreset preset = PresetManager.loadPreset(presetName, source.getServer());
            if (preset == null) {
                source.sendFailure(Component.literal("错误：找不到名为 '" + presetName + "' 的预设！"));
                if (svc != null) svc.removeMatch(name); else MatchManager.removeMatch(name);
                return 0;
            }
            match.applyPreset(preset);
            source.sendSuccess(() -> Component.literal("已从预设 '" + presetName + "' 创建比赛 '" + name + "'！"), true);
        } else {
            source.sendSuccess(() -> Component.literal("比赛 '" + name + "' 已创建！最大人数: " + maxPlayers), true);
        }

        executeServerCommand(source, "team add " + match.getCtTeamName() + " {\"text\":\"Counter-Terrorists\"}");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " color blue");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " nametagVisibility hideForOtherTeams");
        executeServerCommand(source, "team modify " + match.getCtTeamName() + " seeFriendlyInvisibles true");

        executeServerCommand(source, "team add " + match.getTTeamName() + " {\"text\":\"Terrorists\"}");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " color gold");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " friendlyFire " + ServerConfig.friendlyFireEnabled);
        executeServerCommand(source, "team modify " + match.getTTeamName() + " nametagVisibility hideForOtherTeams");
        executeServerCommand(source, "team modify " + match.getTTeamName() + " seeFriendlyInvisibles true");

        return 1;
    }

    public static int saveMatchPreset(CommandContext<CommandSourceStack> context) {
        String matchName = context.getArgument("name", String.class);
        String presetName = context.getArgument("preset_name", String.class);
        CommandSourceStack source = context.getSource();
        Match match = ServiceFallbacks.getMatch(matchName);

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

    public static int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            List<String> presets = PresetManager.listPresets(source.getServer());
            if (presets.isEmpty()) {
                source.sendSuccess(() -> Component.literal("当前没有已保存的比赛预设。"), false);
                return 1;
            }
            MutableComponent message = Component.literal("--- 已保存的预设列表 ---");
            presets.forEach(name -> message.append("\n - ").append(Component.literal(name).withStyle(ChatFormatting.GREEN)));
            source.sendSuccess(() -> message, false);
            return 1;
        } catch (Exception e) {
            QisCSGO.LOGGER.error("列出预设时发生错误: ", e);
            source.sendFailure(Component.literal("错误：读取预设列表时出现异常，请查看日志。"));
            return 0;
        }
    }

    public static int joinMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String matchName = context.getArgument("name", String.class);
        Match match = ServiceFallbacks.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        if (match.getState() != Match.MatchState.PREPARING) {
            context.getSource().sendFailure(Component.literal("错误：比赛 '" + matchName + "' 已经开始或已结束。"));
            return 0;
        }
        Match playerMatch = ServiceFallbacks.getPlayerMatch(player);
        if (playerMatch != null) {
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

    public static int listMatches(CommandSourceStack source) {
        try {
            MatchService svc = ServiceRegistry.get(MatchService.class);
            var matches = svc != null ? svc.getAllMatches() : MatchManager.getAllMatches();
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
        } catch (Exception e) {
            QisCSGO.LOGGER.error("列出比赛时发生错误: ", e);
            source.sendFailure(Component.literal("错误：读取比赛列表时出现异常，请查看日志。"));
            return 0;
        }
    }

    public static int beganMatch(CommandContext<CommandSourceStack> context, boolean force) {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
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

        // 防止在没有玩家的情况下强制开始比赛 — 这通常没有意义并且可能触发运行时代码路径
        if (match.getPlayerCount() == 0) {
            source.sendFailure(Component.literal("错误：比赛没有玩家，无法开始（即使强制）。"));
            return 0;
        }

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
        } catch (Throwable t) {
            // 捕获 Throwable 以避免像 NoClassDefFoundError/LinkageError 这类 Error 导致整个 JVM 崩溃
            QisCSGO.LOGGER.error("开始比赛 '{}' 时发生严重错误:", matchName, t);
            source.sendFailure(Component.literal("开始比赛时发生内部错误，请检查服务器日志获取详细信息！"));
            try {
                match.forceEnd();
            } catch (Throwable ex) {
                QisCSGO.LOGGER.error("强制结束比赛 '{}' 时发生错误（忽略）:", matchName, ex);
            }
            if (svc != null) svc.removeMatch(matchName); else MatchManager.removeMatch(matchName);
        }
        return 1;
    }

    private static void sendForceStartMessage(CommandSourceStack source, String matchName) {
        source.sendSystemMessage(Component.literal("如果仍要强制开始，请使用 ").append(Component.literal("/cs began " + matchName + " yes").withStyle(ChatFormatting.AQUA)));
    }

    public static int kickPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String matchName = context.getArgument("name", String.class);
        ServerPlayer playerToKick = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
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

        // 重置玩家状态并传送
        playerToKick.setGameMode(GameType.SURVIVAL);
        playerToKick.removeAllEffects();

        String attributeCmd = "attribute " + playerToKick.getName().getString() + " minecraft:generic.knockback_resistance base set 0.0";
        source.getServer().getCommands().performPrefixedCommand(source.getServer().createCommandSourceStack(), attributeCmd);

        playerToKick.getInventory().clearContent();

        BlockPos spawnPos = source.getLevel().getSharedSpawnPos();
        playerToKick.teleportTo(source.getLevel(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);

        source.sendSuccess(() -> Component.literal("已将玩家 " + playerToKick.getName().getString() + " 移出比赛 '" + matchName + "'。"), true);
        playerToKick.sendSystemMessage(Component.literal("你已被管理员移出比赛。").withStyle(ChatFormatting.RED));
        return 1;
    }

    public static int setSpawnpoint(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos");
        if ("CT".equals(team)) {
            match.addCtSpawn(pos);
        } else {
            match.addTSpawn(pos);
        }
        context.getSource().sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 添加 " + team + " 方出生点: " + pos.toShortString()), true);
        return 1;
    }

    public static int setNumRounds(CommandContext<CommandSourceStack> context) {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int rounds = context.getArgument("rounds", Integer.class);
        if (rounds % 2 != 0) {
            context.getSource().sendFailure(Component.literal("错误：回合数必须为偶数！"));
            return 0;
        }
        match.setTotalRounds(rounds);
        context.getSource().sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的总回合数设置为 " + rounds), true);
        return 1;
    }

    public static int setRoundTime(CommandContext<CommandSourceStack> context) {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        int seconds = context.getArgument("seconds", Integer.class);
        match.setRoundTimeSeconds(seconds);
        context.getSource().sendSuccess(() -> Component.literal("已将比赛 '" + matchName + "' 的回合时间设置为 " + seconds + " 秒"), true);
        return 1;
    }

    public static int setShopPos(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }
        BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "pos");
        if ("CT".equals(team)) {
            match.setCtShopPos(pos);
        } else {
            match.setTShopPos(pos);
        }
        context.getSource().sendSuccess(() -> Component.literal("已为比赛 '" + matchName + "' 设置 " + team + " 方商店位置: " + pos.toShortString()), true);
        return 1;
    }

    public static int setInitialGear(CommandContext<CommandSourceStack> context, String team) throws CommandSyntaxException {
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

    public static int setBombsite(CommandContext<CommandSourceStack> context, String site) throws CommandSyntaxException {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        if (match == null) {
            context.getSource().sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        BlockPos from = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "from");
        BlockPos to = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(context, "to");

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

    public static int endMatch(CommandContext<CommandSourceStack> context) {
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);
        CommandSourceStack source = context.getSource();

        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        match.forceEnd();
        if (svc != null) svc.removeMatch(matchName); else MatchManager.removeMatch(matchName);

        executeServerCommand(source, "team remove " + match.getCtTeamName());
        executeServerCommand(source, "team remove " + match.getTTeamName());

        source.sendSuccess(() -> Component.literal("已强制结束并清理了比赛 '" + matchName + "'。"), true);
        return 1;
    }

    public static int watchMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer spectator = source.getPlayerOrException();
        String matchName = context.getArgument("name", String.class);
        MatchService svc = ServiceRegistry.get(MatchService.class);
        Match match = svc != null ? svc.getMatch(matchName) : MatchManager.getMatch(matchName);

        if (match == null) {
            source.sendFailure(Component.literal("错误：未找到名为 '" + matchName + "' 的比赛。"));
            return 0;
        }

        // 检查玩家是否已在比赛中，如果在比赛中则不允许切换观战视角
        Match playerMatch = ServiceFallbacks.getPlayerMatch(spectator);
        if (playerMatch != null && playerMatch.getState() == Match.MatchState.IN_PROGRESS) {
            source.sendFailure(Component.literal("错误：游戏进行中不能使用此命令切换观战视角。"));
            return 0;
        }

        List<ServerPlayer> alivePlayers = match.getAlivePlayers().stream()
            .map(uuid -> source.getServer().getPlayerList().getPlayer(uuid))
            .filter(Objects::nonNull)
            .toList();

        if (alivePlayers.isEmpty()) {
            source.sendFailure(Component.literal("错误：比赛 '" + matchName + "' 中当前没有可观战的存活玩家。"));
            return 0;
        }

        ServerPlayer target = alivePlayers.get(new Random().nextInt(alivePlayers.size()));

        spectator.setGameMode(GameType.SPECTATOR);
        spectator.setCamera(target);

        source.sendSuccess(() -> Component.literal("你现在正在观战比赛 '").append(matchName).append("'. 正在跟随玩家 ").append(target.getDisplayName()), false);
        return 1;
    }

    public static int unwatchMatch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer spectator = source.getPlayerOrException();

        spectator.setGameMode(GameType.SURVIVAL);

        BlockPos spawnPos = source.getLevel().getSharedSpawnPos();
        spectator.teleportTo(source.getLevel(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);

        source.sendSuccess(() -> Component.literal("你已退出观战模式。"), false);
        return 1;
    }

    public static int checkBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Match match = ServiceFallbacks.getPlayerMatch(player);

        // 不再强制要求在比赛中，可随时查询余额
        int balance = com.qisumei.csgo.economy.VirtualMoneyManager.getInstance().getMoney(player);
        if (match == null) {
            context.getSource().sendSuccess(() -> Component.literal("当前余额: $" + balance + " （不在比赛中）").withStyle(ChatFormatting.GREEN), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("你的当前余额: $" + balance).withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }


    private static void executeServerCommand(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source.getServer().createCommandSourceStack(), command);
    }
}

