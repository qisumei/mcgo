package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import com.qisumei.csgo.game.preset.MatchPreset;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

import java.util.*;

public class Match {

    public enum MatchState { PREPARING, IN_PROGRESS, FINISHED }
    public enum RoundState { BUY_PHASE, IN_PROGRESS, ROUND_END, PAUSED }

    // --- 比赛基础信息 ---
    private final String name;
    private MatchState state;
    private final int maxPlayers;
    private final MinecraftServer server;
    private final Map<UUID, PlayerStats> playerStats;
    private int totalRounds;
    private int roundTimeSeconds;

    // --- 队伍信息 ---
    private final String ctTeamName;
    private final String tTeamName;

    // --- 地图信息 ---
    private final List<BlockPos> ctSpawns;
    private final List<BlockPos> tSpawns;
    private BlockPos ctShopPos;
    private BlockPos tShopPos;
    private AABB bombsiteA;
    private AABB bombsiteB;

    // --- 回合状态 ---
    private int currentRound;
    private int ctScore;
    private int tScore;
    private RoundState roundState;
    private int tickCounter;
    private final Set<UUID> alivePlayers;
    private String lastRoundWinner;
    private final Set<UUID> roundSurvivors;
    private int scoreboardRebuildCounter = 0;

    // --- C4 相关 ---
    private final C4CountdownHandler c4CountdownHandler;
    private BlockPos c4Pos;
    private boolean c4Planted = false;

    // --- 计分板 ---
    private Scoreboard scoreboard;
    private Objective objective;

    // --- Boss栏计时器 ---
    private final ServerBossEvent bossBar;

    public MatchPreset toPreset() {
        return new MatchPreset(
            this.ctSpawns,
            this.tSpawns,
            this.ctShopPos,
            this.tShopPos,
            this.bombsiteA,
            this.bombsiteB,
            this.totalRounds,
            this.roundTimeSeconds
        );
    }

    public void applyPreset(MatchPreset preset) {
        this.ctSpawns.clear();
        this.ctSpawns.addAll(preset.ctSpawns);
        this.tSpawns.clear();
        this.tSpawns.addAll(preset.tSpawns);
        this.ctShopPos = preset.ctShopPos;
        this.tShopPos = preset.tShopPos;
        this.bombsiteA = preset.bombsiteA;
        this.bombsiteB = preset.bombsiteB;
        this.totalRounds = preset.totalRounds;
        this.roundTimeSeconds = preset.roundTimeSeconds;
    }

    public Match(String name, int maxPlayers, MinecraftServer server) {
        this.name = name;
        this.state = MatchState.PREPARING;
        this.maxPlayers = maxPlayers;
        this.server = server;
        this.playerStats = new HashMap<>();
        this.ctSpawns = new ArrayList<>();
        this.tSpawns = new ArrayList<>();
        this.totalRounds = 12;
        this.roundTimeSeconds = 120; // 2分钟
        this.ctTeamName = name + "_CT";
        this.tTeamName = name + "_T";
        this.currentRound = 0;
        this.ctScore = 0;
        this.tScore = 0;
        this.roundState = RoundState.PAUSED;
        this.tickCounter = 0;
        this.alivePlayers = new HashSet<>();
        this.lastRoundWinner = "";
        this.roundSurvivors = new HashSet<>();
        this.c4CountdownHandler = new C4CountdownHandler(this);
        this.bossBar = new ServerBossEvent(
            Component.literal("等待比赛开始..."), // 初始显示的文字
            BossEvent.BossBarColor.WHITE,          // 初始颜色
            BossEvent.BossBarOverlay.PROGRESS    // 样式为进度条
        );
    }

    public void start() {
        this.state = MatchState.IN_PROGRESS;
        setupScoreboard();
        broadcastToAllPlayersInMatch(Component.literal("比赛开始！"));
        startNewRound();
    }

    private void startNewRound() {
        clearDroppedItems();//在回合开始时清理战场上所有掉落的物品
        this.currentRound++;
        resetC4State(); // 重置C4状态

        // 换边逻辑
        if (this.currentRound == (this.totalRounds / 2) + 1) {
            swapTeams();
        }
        
        // --- 核心修复 #1: 将经济发放移动到回合开始时 ---
        distributeRoundIncome();

        this.roundState = RoundState.BUY_PHASE;
        this.tickCounter = ServerConfig.buyPhaseSeconds * 20;

        int resistanceDuration = ServerConfig.buyPhaseSeconds * 20;
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceDuration, 4, false, false, true));
            }
        }

        broadcastToAllPlayersInMatch(Component.literal("第 " + this.currentRound + " 回合开始！购买阶段！"));
        QisCSGO.LOGGER.info("比赛 '{}': 第 {} 回合开始，进入购买阶段。", name, currentRound);
        
        teleportAndPreparePlayers();
        giveC4ToRandomT();
        spawnShops();
    }

    public void tick() {
        if (state != MatchState.IN_PROGRESS) return;
        
        c4CountdownHandler.tick();

        if (tickCounter > 0) {
            tickCounter--;
            if (tickCounter == 0) {
                if (roundState == RoundState.BUY_PHASE) {
                    beginRoundInProgress();
                } else if (roundState == RoundState.IN_PROGRESS) {
                    // 时间耗尽，T方未安放C4，则CT胜利
                    endRound("CT", "时间耗尽");
                } else if (roundState == RoundState.ROUND_END) {
                    if (this.state == MatchState.IN_PROGRESS) {
                        startNewRound();
                    }
                }
            }

            // --- 修改代码 ---
        // Boss栏进度条
        if (server.getTickCount() % 10 == 0) { 
            if (roundState == RoundState.IN_PROGRESS) {
                checkSurvivorsByGameMode();
            }
            // 计分板还是每秒更新一次
            updateScoreboard();
        }
        // Boss栏在每一tick都更新
        updateBossBar();
        }

        // --- 每秒检测一次 ---
        if (server.getTickCount() % 20 == 0) {
            if (roundState == RoundState.IN_PROGRESS) {
                checkSurvivorsByGameMode();
            }
            updateScoreboard(); // 每秒更新计分板
        }
    }

    // --- 通过游戏模式检测存活玩家 ---
    private void checkSurvivorsByGameMode() {
        long survivalCtCount = 0;
        long survivalTCount = 0;

        for (UUID playerUUID : this.alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
                PlayerStats stats = playerStats.get(playerUUID);
                if (stats != null) {
                    if ("CT".equals(stats.getTeam())) {
                        survivalCtCount++;
                    } else if ("T".equals(stats.getTeam())) {
                        survivalTCount++;
                    }
                }
            }
        }
        
        // 如果有一方没有任何生存模式玩家，则结束回合
        if (!this.alivePlayers.isEmpty()&& survivalCtCount == 0 && survivalTCount > 0) {
            endRound("T", "所有CT玩家死亡");
        } else if (!this.alivePlayers.isEmpty() && survivalTCount == 0 && survivalCtCount > 0) {
            endRound("CT", "所有T玩家死亡");
        }
    }
    
    // --- 队伍与玩家管理 ---
    
    private void swapTeams() {
        broadcastToAllPlayersInMatch(Component.literal("半场换边！队伍已交换。").withStyle(ChatFormatting.YELLOW));
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            PlayerStats stats = entry.getValue();
            String oldTeam = stats.getTeam();
            String newTeam = "CT".equals(oldTeam) ? "T" : "CT";
            stats.setTeam(newTeam);

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                String newTeamName = "CT".equals(newTeam) ? getCtTeamName() : getTTeamName();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team leave " + player.getName().getString());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team join " + newTeamName + " " + player.getName().getString());
                player.sendSystemMessage(Component.literal("你现在是 " + ("CT".equals(newTeam) ? "反恐精英 (CT)" : "恐怖分子 (T)") + " 队的一员！").withStyle(ChatFormatting.AQUA));
            }
        }
    }
    
    // --- 重构：传送和准备玩家的逻辑 ---
    private void teleportAndPreparePlayers() {
        Random random = new Random();
        boolean isPistolRound = (currentRound == 1 || currentRound == (totalRounds / 2) + 1);

        this.alivePlayers.clear();
        this.alivePlayers.addAll(playerStats.keySet());

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            PlayerStats stats = getPlayerStats().get(playerUUID);
            if (stats == null) continue;

            String team = stats.getTeam();
            boolean wasWinner = team.equals(this.lastRoundWinner);
            boolean wasSurvivor = this.roundSurvivors.contains(playerUUID);

            // 总是先清空背包（除了受保护物品）
            performSelectiveClear(player);

            if (isPistolRound) {
                // --- 核心修复 #2: 确保手枪局给予初始装备 ---
                giveInitialGear(player, team);
            } else {
                // --- 核心修复 #3: 为存活的胜利者刷新装备 ---
                if (wasSurvivor && wasWinner) {
                    // 重新发放上一回合记录的装备
                    for (ItemStack gearStack : stats.getRoundGear()) {
                        player.getInventory().add(gearStack.copy());
                    }
                }
            }
            
            // 清空上一回合的装备记录，为本回合做准备
            stats.clearRoundGear();

            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects(); // 清除旧效果，避免无限叠加
            
            // 给予准备阶段的减速效果
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (ServerConfig.buyPhaseSeconds * 20) + 10, 255, false, false, false));
            // 给予准备阶段的无敌效果 (冗余保证，因为 startNewRound 里也加了)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ServerConfig.buyPhaseSeconds * 20, 4, false, false, true));

            List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;
            
            if (spawns.isEmpty()) {
                QisCSGO.LOGGER.error("比赛 '{}' 无法传送 {} 队玩家，因为没有设置出生点！", this.name, team);
                player.sendSystemMessage(Component.literal("错误: " + team + " 队没有设置出生点！请联系管理员。").withStyle(ChatFormatting.RED));
                continue;
            }
            
            BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
            player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
    }

    private void performSelectiveClear(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            boolean isProtected = false;
            // --- 使用新的工具方法来比较物品ID (忽略NBT) ---
            for (String protectedItemString : ServerConfig.inventoryProtectedItems) {
                if (ItemNBTHelper.idMatches(stack, protectedItemString)) {
                    isProtected = true;
                    break;
                }
            }

            if (!isProtected) {
                player.getInventory().setItem(i, ItemStack.EMPTY); // 如果不受保护，则清除
            }
        }
    }

    private void giveInitialGear(ServerPlayer player, String team) {
        List<String> gearList = "CT".equals(team) ? ServerConfig.ctPistolRoundGear : ServerConfig.tPistolRoundGear;
        for (String itemId : gearList) {
            String command = "give " + player.getName().getString() + " " + itemId;
            player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), command);
        }
    }

    private void giveC4ToRandomT() {
        List<ServerPlayer> tPlayers = playerStats.entrySet().stream()
            .filter(e -> "T".equals(e.getValue().getTeam()))
            .map(e -> server.getPlayerList().getPlayer(e.getKey()))
            .filter(Objects::nonNull)
            .toList();

        if (!tPlayers.isEmpty()) {
            ServerPlayer c4Carrier = tPlayers.get(new Random().nextInt(tPlayers.size()));
            c4Carrier.getInventory().add(new ItemStack(QisCSGO.C4_ITEM.get()));
            c4Carrier.sendSystemMessage(Component.literal("§e你携带了C4炸弹！").withStyle(ChatFormatting.BOLD));
        }
    }
    
    // --- 商店管理 ---
    
    private void spawnShops() {
        removeShops();
        int duration = ServerConfig.buyPhaseSeconds * 20;
        if (ctShopPos != null) {
            String command = "summon villager " + ctShopPos.getX() + " " + ctShopPos.getY() + " " + ctShopPos.getZ() + " " + ShopManager.getCtVillagerNbt(duration);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        }
        if (tShopPos != null) {
            String command = "summon villager " + tShopPos.getX() + " " + tShopPos.getY() + " " + tShopPos.getZ() + " " + ShopManager.getTVillagerNbt(duration);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        }
    }

    private void removeShops() {
        if (ctShopPos != null) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..2,x=" + ctShopPos.getX() + ",y=" + ctShopPos.getY() + ",z=" + ctShopPos.getZ() + "]");
        if (tShopPos != null) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..2,x=" + tShopPos.getX() + ",y=" + tShopPos.getY() + ",z=" + tShopPos.getZ() + "]");
    }

    // --- 重构：战斗阶段开始时，记录装备 ---
    private void beginRoundInProgress() {
        this.roundState = RoundState.IN_PROGRESS;
        this.tickCounter = this.roundTimeSeconds * 20;
        
        // --- 核心修复 #3: 在此记录所有玩家的装备 ---
        recordAllPlayerGear();

        broadcastToAllPlayersInMatch(Component.literal("战斗开始！"));
        QisCSGO.LOGGER.info("比赛 '{}': 进入战斗阶段。", name);
        removeShops();
    }

    // --- 新增方法：记录所有玩家的装备 ---
    private void recordAllPlayerGear() {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;
            
            PlayerStats stats = playerStats.get(playerUUID);
            if (stats == null) continue;

            List<ItemStack> currentGear = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                
                // 检查是否是受保护的物品（如货币、护甲）或C4，这些不记录
                boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                                        .anyMatch(id -> ItemNBTHelper.idMatches(stack, id));
                boolean isC4 = stack.getItem() == QisCSGO.C4_ITEM.get();

                if (!isProtected && !isC4) {
                    currentGear.add(stack.copy());
                }
            }
            stats.setRoundGear(currentGear);
        }
    }

    // --- 回合结束逻辑 ---
    
    // --- 重构：回合结束时不再处理经济 ---
    private void endRound(String winningTeam, String reason) {
        if (this.roundState == RoundState.ROUND_END) return;
        this.roundState = RoundState.ROUND_END;
        this.lastRoundWinner = winningTeam;
        
        this.roundSurvivors.clear();
        this.roundSurvivors.addAll(this.alivePlayers);
        
        if (winningTeam.equals("CT")) {
            ctScore++;
            broadcastToAllPlayersInMatch(Component.literal("CT方 胜利！ (" + reason + ")"));
            // 胜利方更新连败记录
            playerStats.values().stream().filter(s -> "CT".equals(s.getTeam())).forEach(PlayerStats::resetConsecutiveLosses);
            playerStats.values().stream().filter(s -> "T".equals(s.getTeam())).forEach(PlayerStats::incrementConsecutiveLosses);
        } else {
            tScore++;
            broadcastToAllPlayersInMatch(Component.literal("T方 胜利！ (" + reason + ")"));
            // 胜利方更新连败记录
            playerStats.values().stream().filter(s -> "T".equals(s.getTeam())).forEach(PlayerStats::resetConsecutiveLosses);
            playerStats.values().stream().filter(s -> "CT".equals(s.getTeam())).forEach(PlayerStats::incrementConsecutiveLosses);
        }
        
        // 此处不再调用 distributeRoundIncome()
        QisCSGO.LOGGER.info("比赛 '{}': 第 {} 回合结束, {}方胜利. 比分 CT {}:{} T", name, currentRound, winningTeam, ctScore, tScore);

        int roundsToWin = (this.totalRounds / 2) + 1;
        if (ctScore >= roundsToWin || tScore >= roundsToWin) {
            finishMatch(ctScore > tScore ? "CT" : "T");
        } else if (currentRound >= totalRounds) {
            if (ctScore == tScore) {
                handleTie();
            } else {
                finishMatch(ctScore > tScore ? "CT" : "T");
            }
        } else {
            this.tickCounter = ServerConfig.roundEndSeconds * 20;
        }
    }
    
    // --- 重构：经济发放方法不再需要 winningTeam 参数 ---
    private void distributeRoundIncome() {
        boolean isPistolRound = (currentRound == 1 || currentRound == (totalRounds / 2) + 1);
        
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PlayerStats stats = entry.getValue();
            if (player == null) continue;

            if (isPistolRound) {
                // 清空钻石并发放起始资金
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "clear " + player.getName().getString() + " minecraft:diamond");
                EconomyManager.giveMoney(player, ServerConfig.pistolRoundStartingMoney);
                player.sendSystemMessage(Component.literal("手枪局！起始资金: " + ServerConfig.pistolRoundStartingMoney + " diamonds.").withStyle(ChatFormatting.AQUA));
            } else { // 常规局
                boolean wasWinner = stats.getTeam().equals(this.lastRoundWinner);
                int income;
                if (wasWinner) {
                    income = ServerConfig.winReward;
                    player.sendSystemMessage(Component.literal("回合胜利！获得 " + income + " diamonds.").withStyle(ChatFormatting.GREEN));
                } else {
                    int lossBonus = Math.min(stats.getConsecutiveLosses() * ServerConfig.lossStreakBonus, ServerConfig.maxLossStreakBonus);
                    income = ServerConfig.lossReward + lossBonus;
                    player.sendSystemMessage(Component.literal("回合失败。获得 " + income + " diamonds (含连败奖励).").withStyle(ChatFormatting.RED));
                }
                EconomyManager.giveMoney(player, income);
            }
        }
    }

    private void finishMatch(String winningTeam) {
        this.state = MatchState.FINISHED;
        Component winner = Component.literal(winningTeam).withStyle(winningTeam.equals("CT") ? ChatFormatting.BLUE : ChatFormatting.GOLD);
        broadcastToAllPlayersInMatch(Component.literal("比赛结束！胜利者是 ").append(winner).append("!"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, {}方胜利.", name, winningTeam);
        removeScoreboard();
    }

    private void handleTie() {
        this.state = MatchState.FINISHED;
        broadcastToAllPlayersInMatch(Component.literal("比赛平局！"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, 平局.", name);
        removeScoreboard();
    }

    // --- 玩家死亡和重生处理 ---
    
    public void markPlayerAsDead(ServerPlayer deadPlayer) {
        if (!this.alivePlayers.contains(deadPlayer.getUUID())) return;
        
        this.alivePlayers.remove(deadPlayer.getUUID());
        PlayerStats stats = playerStats.get(deadPlayer.getUUID());
        if(stats != null) stats.incrementDeaths();
        
        QisCSGO.LOGGER.info("玩家 {} 在比赛 '{}' 中阵亡。", deadPlayer.getName().getString(), name);
        this.checkRoundEndCondition();
    }
    
    public void handlePlayerRespawn(ServerPlayer respawningPlayer) {
        respawningPlayer.setGameMode(GameType.SPECTATOR);
    }
    
    private void checkRoundEndCondition() {
        if (roundState != RoundState.IN_PROGRESS) return;

        long aliveCtCount = alivePlayers.stream().filter(uuid -> "CT".equals(playerStats.get(uuid).getTeam())).count();
        long aliveTCount = alivePlayers.stream().filter(uuid -> "T".equals(playerStats.get(uuid).getTeam())).count();

        if (aliveTCount == 0 && !playerStats.isEmpty() && currentRound > 0) {
            endRound("CT", "所有T玩家死亡");
        } else if (aliveCtCount == 0 && !playerStats.isEmpty() && currentRound > 0) {
            endRound("T", "所有CT玩家死亡");
        }
    }
    
    // --- C4 逻辑 ---

    public void onC4Planted(BlockPos pos) {
        this.c4Planted = true;
        this.c4Pos = pos;
        this.roundTimeSeconds = 40; // 重置回合时间为C4倒计时
        this.tickCounter = 40 * 20;
        c4CountdownHandler.start(pos);
    }
    
    public void onC4Defused() {
        endRound("CT", "炸弹已被拆除");
    }

    public void onC4Exploded() {
        if (c4Pos != null) {
            server.overworld().explode(null, c4Pos.getX() + 0.5, c4Pos.getY() + 0.5, c4Pos.getZ() + 0.5, 20.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        }
        endRound("T", "炸弹已爆炸");
    }
    
    private void resetC4State() {
        if (c4Planted && c4Pos != null) {
            server.overworld().removeBlock(c4Pos, false);
        }
        c4CountdownHandler.stop();
        this.c4Planted = false;
        this.c4Pos = null;
    }
    
    public boolean isPlayerInBombsite(ServerPlayer player) {
        return (bombsiteA != null && bombsiteA.contains(player.position())) || (bombsiteB != null && bombsiteB.contains(player.position()));
    }
    
    public boolean isPosInBombsite(BlockPos pos) {
        return (bombsiteA != null && bombsiteA.contains(pos.getX(), pos.getY(), pos.getZ())) || (bombsiteB != null && bombsiteB.contains(pos.getX(), pos.getY(), pos.getZ()));
    }


    private void clearDroppedItems() {
        // 创建一个列表，用来收集比赛中所有的关键坐标点。
        List<BlockPos> allPositions = new ArrayList<>();
        // 将CT和T的出生点都添加进去。
        allPositions.addAll(ctSpawns);
        allPositions.addAll(tSpawns);
        // 如果包点A和B已经设置，也将它们的边界坐标添加进去。
        if (bombsiteA != null) {
            allPositions.add(BlockPos.containing(bombsiteA.minX, bombsiteA.minY, bombsiteA.minZ));
            allPositions.add(BlockPos.containing(bombsiteA.maxX, bombsiteA.maxY, bombsiteA.maxZ));
        }
        if (bombsiteB != null) {
            allPositions.add(BlockPos.containing(bombsiteB.minX, bombsiteB.minY, bombsiteB.minZ));
            allPositions.add(BlockPos.containing(bombsiteB.maxX, bombsiteB.maxY, bombsiteB.maxZ));
        }

        // 如果没有任何坐标点（比如比赛还未完全设置），则直接返回，避免出错。
        if (allPositions.isEmpty()) {
            return;
        }

        // --- 计算整个比赛区域的边界框 (AABB) ---
        // 初始化最小和最大坐标。
        double minX = allPositions.get(0).getX();
        double minY = allPositions.get(0).getY();
        double minZ = allPositions.get(0).getZ();
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;

        // 遍历所有坐标点，找出整个区域的最小和最大坐标。
        for (BlockPos pos : allPositions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // 基于找到的坐标创建一个覆盖整个比赛区域的边界框，并稍微扩大一点范围以确保覆盖。
        AABB matchArea = new AABB(minX - 50, minY - 20, minZ - 50, maxX + 50, maxY + 20, maxZ + 50);

        // --- 清除物品 ---
        // 获取在上述边界框内的所有掉落物品实体（ItemEntity）。
        List<ItemEntity> itemsToRemove = server.overworld().getEntitiesOfClass(ItemEntity.class, matchArea, (entity) -> true);

        // 遍历列表，并移除每一个物品实体。
        for (ItemEntity itemEntity : itemsToRemove) {
            // .discard() 方法会安全地将实体从世界中移除。
            itemEntity.discard();
        }

        // 在服务器日志中记录本次清理操作，方便调试。
        QisCSGO.LOGGER.info("比赛 '{}': 清理了 {} 个掉落物品。", this.name, itemsToRemove.size());
    }

    // --- 广播和强制结束 ---
    
    public void broadcastToAllPlayersInMatch(Component message) {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.sendSystemMessage(message, false);
            }
        }
    }
    
    public void forceEnd() {
        this.state = MatchState.FINISHED;
        broadcastToAllPlayersInMatch(Component.literal("比赛已被管理员强制结束。"));
        removeShops();
        removeScoreboard();
        this.bossBar.removeAllPlayers();

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.setGameMode(server.getDefaultGameType());
                player.removeAllEffects();
            }
        }
    }

    // --- 计分板管理 ---
    
    private void setupScoreboard() {
        this.scoreboard = server.getScoreboard();
        String safeMatchName = name.replaceAll("[^a-zA-Z0-9_.-]", "");
        String objectiveName = "kda_" + safeMatchName.substring(0, Math.min(safeMatchName.length(), 12));
        
        Objective oldObjective = this.scoreboard.getObjective(objectiveName);
        if (oldObjective != null) {
            this.scoreboard.removeObjective(oldObjective);
        }

        this.objective = this.scoreboard.addObjective(
            objectiveName, 
            ObjectiveCriteria.DUMMY, 
            Component.literal("比赛排名").withStyle(ChatFormatting.YELLOW), 
            ObjectiveCriteria.RenderType.INTEGER, 
            true, 
            null
        );
        this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
    }

    private void updateScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;

        // 每10秒重建一次计分板来清理离开的玩家
        scoreboardRebuildCounter++;
        if (scoreboardRebuildCounter >= 200) { // 10秒 * 20ticks/秒
            rebuildScoreboard();
            scoreboardRebuildCounter = 0;
            return;
        }

        // 正常更新分数
        List<Map.Entry<UUID, PlayerStats>> sortedPlayers = playerStats.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getKills()).reversed()
            .thenComparingInt(e -> e.getValue().getDeaths()))
            .limit(15)
            .toList();

        for (Map.Entry<UUID, PlayerStats> entry : sortedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                this.scoreboard.getOrCreatePlayerScore(player, this.objective).set(entry.getValue().getKills());
            }
        }
    }

    private void rebuildScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;
        
        // 保存当前目标信息
        String objectiveName = this.objective.getName();
        Component displayName = this.objective.getDisplayName();
        
        // 移除旧目标
        this.scoreboard.removeObjective(this.objective);
        
        // 创建新目标
        this.objective = this.scoreboard.addObjective(
            objectiveName, 
            ObjectiveCriteria.DUMMY, 
            displayName, 
            ObjectiveCriteria.RenderType.INTEGER, 
            true, 
            null
        );
        this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
        
        QisCSGO.LOGGER.debug("重建计分板: {}", objectiveName);
    }

    private void removeScoreboard() {
        if (this.scoreboard != null && this.objective != null) {
            // 先移除显示
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            
            // 然后移除计分板目标
            Objective currentObjective = this.scoreboard.getObjective(this.objective.getName());
            if (currentObjective != null) {
                this.scoreboard.removeObjective(currentObjective);
            }
            this.objective = null;
        }
    }
    /**
     * 新增方法：为指定玩家重新应用计分板。
     * 当新玩家加入或断线重连时调用，确保他们能看到计分板。
     * @param player 需要接收计分板信息的玩家。
     */
    public void reapplyScoreboardToPlayer(ServerPlayer player) {
        // 确保计分板和计分项 (objective) 已经创建。
        if (this.scoreboard != null && this.objective != null) {
            // 为所有玩家（包括新玩家）重新设置侧边栏的显示目标。
            // 服务端会自动将这个信息同步给客户端。
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);

            // 同时，更新该玩家在计分板上的分数，以确保信息同步。
            PlayerStats stats = this.playerStats.get(player.getUUID());
            int currentKills = (stats != null) ? stats.getKills() : 0;
            
            // 获取或创建该玩家的分数记录，并设置其值为当前的击杀数。
            this.scoreboard.getOrCreatePlayerScore(player, this.objective).set(currentKills);
        }
    }


    /**
     * 新增方法：更新Boss栏的显示内容和进度。
     * 根据当前的回合状态（购买、进行中、C4倒计时等）来改变Boss栏。
     */
    private void updateBossBar() {
        // 根据当前的回合状态来决定显示什么内容
        switch (this.roundState) {
            case BUY_PHASE:
                // 购买阶段
                int buyPhaseTotalTicks = ServerConfig.buyPhaseSeconds * 20;
                float buyProgress = (float) this.tickCounter / buyPhaseTotalTicks;
                this.bossBar.setName(Component.literal("购买阶段剩余: " + (this.tickCounter / 20 + 1) + "s"));
                this.bossBar.setColor(BossEvent.BossBarColor.GREEN);
                this.bossBar.setProgress(buyProgress);
                break;

            case IN_PROGRESS:
                // 战斗阶段
                if (this.c4Planted) {
                    // 如果C4已经安放
                    int c4TotalTicks = 40 * 20; // C4总共40秒
                    // 注意：C4的倒计时是从C4CountdownHandler里获取的，但为了同步显示，我们这里也用tickCounter
                    float c4Progress = (float) this.tickCounter / c4TotalTicks;
                    this.bossBar.setName(Component.literal("C4即将爆炸: " + (this.tickCounter / 20 + 1) + "s").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    this.bossBar.setColor(BossEvent.BossBarColor.RED);
                    this.bossBar.setProgress(c4Progress);
                } else {
                    // 如果C4未安放
                    int roundTotalTicks = this.roundTimeSeconds * 20;
                    float roundProgress = (float) this.tickCounter / roundTotalTicks;
                    this.bossBar.setName(Component.literal("回合剩余时间: " + (this.tickCounter / 20 + 1) + "s"));
                    this.bossBar.setColor(BossEvent.BossBarColor.WHITE);
                    this.bossBar.setProgress(roundProgress);
                }
                break;

            case ROUND_END:
                // 回合结束阶段
                this.bossBar.setName(Component.literal("回合结束"));
                this.bossBar.setColor(BossEvent.BossBarColor.YELLOW);
                this.bossBar.setProgress(1.0f);
                break;

            default:
                // 其他情况（如暂停）
                this.bossBar.setName(Component.literal("比赛暂停"));
                this.bossBar.setColor(BossEvent.BossBarColor.PURPLE);
                this.bossBar.setProgress(1.0f);
                break;
        }
    }
    
    // --- Getters and Setters ---
    
    public String getName() { return name; }
    public MatchState getState() { return state; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getPlayerCount() { return playerStats.size(); }
    public String getCtTeamName() { return ctTeamName; }
    public String getTTeamName() { return tTeamName; }
    public Map<UUID, PlayerStats> getPlayerStats() { return playerStats; }
    public long getCtCount() { return playerStats.values().stream().filter(s -> s.getTeam().equals("CT")).count(); }
    public long getTCount() { return playerStats.values().stream().filter(s -> s.getTeam().equals("T")).count(); }
    public MinecraftServer getServer() { return this.server; }
    public boolean isC4Planted() { return this.c4Planted; }
    public BlockPos getC4Pos() { return this.c4Pos; }
    public void setBombsiteA(AABB area) { this.bombsiteA = area; }
    public void setBombsiteB(AABB area) { this.bombsiteB = area; }
    
    /**
     * 将一名玩家添加到比赛中。
     * @param player 要添加的玩家。
     * @param team   玩家要加入的队伍 ("CT" 或 "T")。
     */
    public void addPlayer(ServerPlayer player, String team) { 
        // 将玩家的UUID和新的统计数据存入Map中。
        playerStats.put(player.getUUID(), new PlayerStats(team)); 
        // 为这名新加入的玩家应用计分板，确保他能立即看到。
        reapplyScoreboardToPlayer(player);
        this.bossBar.addPlayer(player);
    }
    
    public void removePlayer(ServerPlayer player) { 
        playerStats.remove(player.getUUID()); 
        this.bossBar.removePlayer(player);
    }
    
    public void addCtSpawn(BlockPos pos) { 
        this.ctSpawns.add(pos); 
    }
    
    public void addTSpawn(BlockPos pos) { 
        this.tSpawns.add(pos); 
    }
    
    public void setTotalRounds(int rounds) { 
        this.totalRounds = rounds; 
    }
    
    public void setRoundTimeSeconds(int seconds) { 
        this.roundTimeSeconds = seconds; 
    }
    
    public void setCtShopPos(BlockPos pos) { 
        this.ctShopPos = pos; 
    }
    
    public void setTShopPos(BlockPos pos) { 
        this.tShopPos = pos; 
    }
    /**
     * 获取当前的回合状态。
     * @return 当前的 RoundState 枚举值 (例如 BUY_PHASE, IN_PROGRESS, ROUND_END)。
     */
    public RoundState getRoundState() {
        return this.roundState;
    }
    /**
     * 获取比赛的Boss栏实例。
     * @return ServerBossEvent 实例
     */
    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }
    
}