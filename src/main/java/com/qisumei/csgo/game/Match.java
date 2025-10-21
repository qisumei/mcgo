package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.C4Manager;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.preset.MatchPreset;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Match类，管理一场CSGO比赛的整个生命周期和所有核心逻辑。
 */
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

    // --- C4 管理 ---
    private final C4Manager c4Manager;

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
    // 记录每个队伍最后一名死亡玩家的位置，用于全队死亡后的观察视角
    private final Map<String, BlockPos> lastTeammateDeathPos = new HashMap<>();

    // --- Boss栏计时器 ---
    private final ServerBossEvent bossBar;

    // --- 计分板 ---
    private Scoreboard scoreboard;
    private Objective objective;

    /**
     * Match类的构造函数，用于初始化一场新的比赛。
     * @param name 比赛的唯一名称。
     * @param maxPlayers 比赛的最大玩家数。
     * @param server Minecraft服务器实例。
     */
    public Match(String name, int maxPlayers, MinecraftServer server) {
        this.name = name;
        this.state = MatchState.PREPARING;
        this.maxPlayers = maxPlayers;
        this.server = server;
        this.playerStats = new HashMap<>();
        this.ctSpawns = new ArrayList<>();
        this.tSpawns = new ArrayList<>();
        this.totalRounds = 12;
        this.roundTimeSeconds = 120; // 默认2分钟
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
        
        
        // 初始化 C4Manager，将自身引用传入
        this.c4Manager = new C4Manager(this);

        // 初始化Boss栏
        this.bossBar = new ServerBossEvent(
            Component.literal("等待比赛开始..."),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
        );
    }

    /**
     * 将当前比赛的设置转换为一个MatchPreset对象，用于保存。
     * @return 包含当前比赛设置的MatchPreset实例。
     */
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

    /**
     * 从一个MatchPreset对象加载比赛设置。
     * @param preset 包含比赛设置的预设对象。
     */
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

    /**
     * 正式开始比赛。
     */
    public void start() {
        this.state = MatchState.IN_PROGRESS;

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            setPlayerKnockbackResistance(player, 1000.0);
        }
        setupScoreboard();
        broadcastToAllPlayersInMatch(Component.literal("比赛开始！"));
        startNewRound();
    }

    /**
     * 开始一个新回合的完整流程。
     */
    private void startNewRound() {
        // 1. 清理战场上的掉落物
        clearDroppedItems();
        
        // 2. 推进回合数并重置C4状态
        this.currentRound++;
        c4Manager.reset();

        // 3. 检查是否需要换边
        if (this.currentRound == (this.totalRounds / 2) + 1) {
            swapTeams();
        }
        
        // 给玩家广播当前比分
        String titleJson = String.format(
            "[{\"text\":\"CT \",\"color\":\"blue\"},{\"text\":\"%d - %d\",\"color\":\"white\"},{\"text\":\" T\",\"color\":\"gold\"}]",
            ctScore, tScore
        );
        
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "title " + player.getName().getString() + " title " + titleJson);
            }
        }

        // 全局聊天广播比分
        String chatJson = String.format(
            "[{\"text\":\"%s：\",\"color\":\"yellow\"},{\"text\":\"CT \",\"color\":\"blue\"},{\"text\":\"%d:%d\",\"color\":\"white\",\"bold\":true},{\"text\":\" T\",\"color\":\"gold\"}]",
            this.name, ctScore, tScore
        );
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tellraw @a " + chatJson);

        // 4. 设置回合状态为购买阶段
        this.roundState = RoundState.BUY_PHASE;
        this.tickCounter = ServerConfig.buyPhaseSeconds * 20;

        // 5. 传送玩家、清空背包、并发放该回合应有的装备
        teleportAndPreparePlayers();
        
        // 6. 处理经济，发放金钱
        distributeRoundIncome();
        
        // 在发放金钱后，生成商店村民
        spawnShops();

        // 7. 为所有玩家添加购买阶段的无敌效果
        int resistanceDuration = ServerConfig.buyPhaseSeconds * 20;
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceDuration, 4, false, false, true));
            }
        }

        // 8. 广播回合开始的消息
        broadcastToAllPlayersInMatch(Component.literal("第 " + this.currentRound + " 回合开始！购买阶段！"));
        QisCSGO.LOGGER.info("比赛 '{}': 第 {} 回合开始，进入购买阶段。", name, currentRound);
        
        // 9. 最后再给T发放C4
        c4Manager.giveC4ToRandomT();
    }

    /**
     * 比赛的核心更新方法，每tick被MatchManager调用。
     */
    public void tick() {
        if (state != MatchState.IN_PROGRESS) return;
        
        // 更新C4管理器
        c4Manager.tick();

        // 购买阶段区域限制逻辑
        if (roundState == RoundState.BUY_PHASE) {
            checkPlayerBuyZone();
        }

        // 处理主计时器
        if (tickCounter > 0) {
            tickCounter--;
            if (tickCounter == 0) {
                if (roundState == RoundState.BUY_PHASE) {
                    beginRoundInProgress(); // 购买阶段结束，开始战斗
                } else if (roundState == RoundState.IN_PROGRESS) {
                    endRound("CT", "时间耗尽"); // 回合时间到，CT胜利
                } else if (roundState == RoundState.ROUND_END) {
                    if (this.state == MatchState.IN_PROGRESS) {
                        startNewRound(); // 回合结束展示时间到，开始新回合
                    }
                }
            }
        }
        
        // 在每秒的 tick 中，更新计分板
        if (server.getTickCount() % 20 == 0) {
            updateScoreboard();
        }
        if (server.getTickCount() % 5 == 0) {
            updateSpectatorCameras();
        }
        
        // 每一tick都更新Boss栏，以保证进度条平滑
        updateBossBar();
    }

    /**
     * 【新增】计算并返回一个包围了比赛所有关键点的大致区域。
     * 用于在大范围内搜索实体，如掉落的C4。
     * @return AABB 区域包围盒，如果没有设置任何点则返回null。
     */
    public AABB getMatchAreaBoundingBox() {
        List<BlockPos> allPositions = new ArrayList<>();
        allPositions.addAll(ctSpawns);
        allPositions.addAll(tSpawns);
        if (bombsiteA != null) {
            allPositions.add(BlockPos.containing(bombsiteA.getCenter()));
        }
        if (bombsiteB != null) {
            allPositions.add(BlockPos.containing(bombsiteB.getCenter()));
        }

        if (allPositions.isEmpty()) {
            return null;
        }

        AABB boundingBox = new AABB(allPositions.get(0));
        for (BlockPos pos : allPositions) {
            boundingBox = boundingBox.minmax(new AABB(pos));
        }
        return boundingBox;
    }

    /**
     * 在购买阶段检查玩家是否超出购买区域，如果超出则传送回出生点。
     */
    private void checkPlayerBuyZone() {
        final double maxDistance = 10.0;
        Random random = new Random();

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            PlayerStats stats = playerStats.get(playerUUID);
            if (stats == null) continue;

            String team = stats.getTeam();
            BlockPos shopPos = "CT".equals(team) ? ctShopPos : tShopPos;
            List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;

            if (shopPos == null || spawns.isEmpty()) continue;

            double distance = Math.sqrt(player.distanceToSqr(shopPos.getX() + 0.5, player.getY(), shopPos.getZ() + 0.5));

            if (distance > maxDistance) {
                BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
                player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                player.sendSystemMessage(Component.literal("购买阶段请不要离开购买区域！").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /**
     * 在半场时交换双方队伍。
     */
    private void swapTeams() {
        broadcastToAllPlayersInMatch(Component.literal("半场换边！队伍已交换。").withStyle(ChatFormatting.YELLOW));
        int tempScore = this.ctScore;
        this.ctScore = this.tScore;
        this.tScore = tempScore;
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
                performSelectiveClear(player);
                player.sendSystemMessage(Component.literal("你现在是 " + ("CT".equals(newTeam) ? "反恐精英 (CT)" : "恐怖分子 (T)") + " 队的一员！").withStyle(ChatFormatting.AQUA));
            }
        }
    }
    
    /**
     * 在回合开始时，传送所有玩家到各自的出生点并准备他们的状态。
     */
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

            performSelectiveClear(player);

            if (isPistolRound) {
                giveInitialGear(player, team);
            } else {
                if (wasSurvivor && wasWinner) {
                    for (ItemStack gearStack : stats.getRoundGear()) {
                        player.getInventory().add(gearStack.copy());
                    }
                }
            }
            
            stats.clearRoundGear();

            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects();
            
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

    /**
     * 选择性地清空玩家背包，保留货币、护甲等受保护物品。
     * @param player 需要被清空背包的玩家。
     */
    private void performSelectiveClear(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            boolean isProtected = false;
            for (String protectedItemString : ServerConfig.inventoryProtectedItems) {
                if (ItemNBTHelper.idMatches(stack, protectedItemString)) {
                    isProtected = true;
                    break;
                }
            }

            if (!isProtected) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 在手枪局为玩家发放初始装备。
     * @param player 接收装备的玩家。
     * @param team 玩家所属队伍。
     */
    private void giveInitialGear(ServerPlayer player, String team) {
        List<String> gearList = "CT".equals(team) ? ServerConfig.ctPistolRoundGear : ServerConfig.tPistolRoundGear;
        for (String itemId : gearList) {
            String command = "give " + player.getName().getString() + " " + itemId;
            player.server.getCommands().performPrefixedCommand(player.server.createCommandSourceStack(), command);
        }
    }
    /**
     * 在购买阶段生成商店村民。
     */
    private void spawnShops() {
        removeShops();
        int duration = ServerConfig.buyPhaseSeconds * 20;
        Random random = new Random();

        // CT 商店生成逻辑
        if (ctShopPos != null) {
            int ctShops = Math.max(1, (int) Math.ceil(getCtCount()));
            for (int i = 0; i < ctShops; i++) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                double spawnX = ctShopPos.getX() + 0.5 + offsetX;
                double spawnY = ctShopPos.getY();
                double spawnZ = ctShopPos.getZ() + 0.5 + offsetZ;

                String command = String.format(Locale.US, "summon villager %.2f %.2f %.2f %s", 
                                             spawnX, spawnY, spawnZ, ShopManager.getCtVillagerNbt(duration));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        } else {
            QisCSGO.LOGGER.warn("比赛 '{}': 尝试生成商店失败，因为CT商店位置未设置。", this.name);
        }

        // T 商店生成逻辑
        if (tShopPos != null) {
            int tShops = Math.max(1, (int) Math.ceil(getTCount()));
            for (int i = 0; i < tShops; i++) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                double spawnX = tShopPos.getX() + 0.5 + offsetX;
                double spawnY = tShopPos.getY();
                double spawnZ = tShopPos.getZ() + 0.5 + offsetZ;
                
                String command = String.format(Locale.US, "summon villager %.2f %.2f %.2f %s", 
                                             spawnX, spawnY, spawnZ, ShopManager.getTVillagerNbt(duration));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        } else {
            QisCSGO.LOGGER.warn("比赛 '{}': 尝试生成商店失败，因为T商店位置未设置。", this.name);
        }
    }

    /**
     * 移除商店村民。
     */
    private void removeShops() {
        if (ctShopPos != null) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..2,x=" + ctShopPos.getX() + ",y=" + ctShopPos.getY() + ",z=" + ctShopPos.getZ() + "]");
        if (tShopPos != null) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..2,x=" + tShopPos.getX() + ",y=" + tShopPos.getY() + ",z=" + tShopPos.getZ() + "]");
    }

    /**
     * 结束购买阶段，进入战斗阶段。
     */
    private void beginRoundInProgress() {
        this.roundState = RoundState.IN_PROGRESS;
        this.tickCounter = this.roundTimeSeconds * 20;

        // 传送逻辑防止偷跑
        Random random = new Random();
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            PlayerStats stats = getPlayerStats().get(playerUUID);
            if (stats == null) continue;

            String team = stats.getTeam();
            List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;
            
            if (!spawns.isEmpty()) {
                BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
                player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        }
        
        recordAllPlayerGear();
        
        broadcastToAllPlayersInMatch(Component.literal("战斗开始！"));
        QisCSGO.LOGGER.info("比赛 '{}': 进入战斗阶段。", name);
        removeShops();
    }

    /**
     * 记录所有玩家在战斗阶段开始时的装备，用于胜利后保留装备。
     */
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

    /**
     * 结束当前回合的逻辑。
     * @param winningTeam 获胜的队伍 ("CT" 或 "T")。
     * @param reason 获胜的原因。
     */
    public void endRound(String winningTeam, String reason) {
        if (this.roundState == RoundState.ROUND_END) return;
        this.roundState = RoundState.ROUND_END;
        this.lastRoundWinner = winningTeam;
        
        this.roundSurvivors.clear();
        this.roundSurvivors.addAll(this.alivePlayers);
        
        if (winningTeam.equals("CT")) {
            ctScore++;
            broadcastToAllPlayersInMatch(Component.literal("CT方 胜利！ (" + reason + ")"));
            playerStats.values().stream().filter(s -> "CT".equals(s.getTeam())).forEach(PlayerStats::resetConsecutiveLosses);
            playerStats.values().stream().filter(s -> "T".equals(s.getTeam())).forEach(PlayerStats::incrementConsecutiveLosses);
        } else {
            tScore++;
            broadcastToAllPlayersInMatch(Component.literal("T方 胜利！ (" + reason + ")"));
            playerStats.values().stream().filter(s -> "T".equals(s.getTeam())).forEach(PlayerStats::resetConsecutiveLosses);
            playerStats.values().stream().filter(s -> "CT".equals(s.getTeam())).forEach(PlayerStats::incrementConsecutiveLosses);
        }
        
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
    
    /**
     * 在回合开始时为玩家发放收入。
     */
    private void distributeRoundIncome() {
        boolean isPistolRound = (currentRound == 1 || currentRound == (totalRounds / 2) + 1);
        
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PlayerStats stats = entry.getValue();
            if (player == null) continue;

            if (isPistolRound) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "clear " + player.getName().getString() + " minecraft:diamond");
                EconomyManager.giveMoney(player, ServerConfig.pistolRoundStartingMoney);
                player.sendSystemMessage(Component.literal("手枪局！起始资金: " + ServerConfig.pistolRoundStartingMoney + " diamonds.").withStyle(ChatFormatting.AQUA));
            } else {
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

    /**
     * 根据击杀数（主要）和死亡数（次要，越少越好）获取指定队伍的顶尖玩家列表。
     * @param team 要查找的队伍 ("CT" 或 "T")。
     * @param limit 返回的玩家数量上限。
     * @return 包含玩家UUID和其统计数据的有序列表。
     */
    private List<Map.Entry<UUID, PlayerStats>> getRankedPlayers(String team, int limit) {
        return playerStats.entrySet().stream()
            .filter(entry -> team.equals(entry.getValue().getTeam()))
            .sorted(Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getKills())
                          .reversed()
                          .thenComparingInt(e -> e.getValue().getDeaths()))
            .limit(limit)
            .toList();
    }
    
    /**
     * 查找并以特定格式广播比赛结束时的统计数据。
     */
    private void broadcastEndGameStats() {
        final String SEPARATOR = "============================";
        final String COLUMN_SPACER = "           ";
        final int NAME_PADDING = 12;

        List<Map.Entry<UUID, PlayerStats>> topCtPlayers = getRankedPlayers("CT", 3);
        List<Map.Entry<UUID, PlayerStats>> topTPlayers = getRankedPlayers("T", 3);

        broadcastToAllPlayersInMatch(Component.literal(""));
        broadcastToAllPlayersInMatch(Component.literal(SEPARATOR).withStyle(ChatFormatting.GOLD));

        String scoreText = "                本场比分: " + ctScore + ":" + tScore;
        broadcastToAllPlayersInMatch(Component.literal(scoreText).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        Component header = Component.literal("CT击杀数排名").withStyle(ChatFormatting.BLUE)
            .append(Component.literal(COLUMN_SPACER))
            .append(Component.literal("T击杀数排名").withStyle(ChatFormatting.GOLD));
        broadcastToAllPlayersInMatch(header);

        int maxRows = Math.max(topCtPlayers.size(), topTPlayers.size());
        if (maxRows == 0) {
             broadcastToAllPlayersInMatch(Component.literal("本场比赛没有玩家数据。").withStyle(ChatFormatting.GRAY));
        }

        for (int i = 0; i < maxRows; i++) {
            String ctPlayerLine = "";
            if (i < topCtPlayers.size()) {
                Map.Entry<UUID, PlayerStats> entry = topCtPlayers.get(i);
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    ctPlayerLine = String.format("%-" + NAME_PADDING + "." + NAME_PADDING + "s  %d", 
                                                 player.getName().getString(), 
                                                 entry.getValue().getKills());
                }
            }

            String tPlayerLine = "";
            if (i < topTPlayers.size()) {
                Map.Entry<UUID, PlayerStats> entry = topTPlayers.get(i);
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    tPlayerLine = String.format("%-" + NAME_PADDING + "." + NAME_PADDING + "s  %d", 
                                                player.getName().getString(), 
                                                entry.getValue().getKills());
                }
            }
            
            String leftPadded = String.format("%-18s", ctPlayerLine);
            Component fullLine = Component.literal(leftPadded).withStyle(ChatFormatting.WHITE)
                                    .append(Component.literal(tPlayerLine).withStyle(ChatFormatting.WHITE));

            broadcastToAllPlayersInMatch(fullLine);
        }

        broadcastToAllPlayersInMatch(Component.literal(SEPARATOR).withStyle(ChatFormatting.GOLD));
    }
    
    /**
     * 比赛结束后，重置所有玩家状态并将其传送至世界出生点。
     */
    private void resetAndTeleportPlayers() {
        BlockPos spawnPos = server.overworld().getSharedSpawnPos();

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.setGameMode(GameType.SURVIVAL);
                player.removeAllEffects();
                setPlayerKnockbackResistance(player, 0.0);
                player.getInventory().clearContent();
                player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            }
        }
    }

    /**
     * 清理所有与本场比赛相关的服务器数据，如队伍和比赛实例。
     */
    private void cleanupMatchData() {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team remove " + ctTeamName);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team remove " + tTeamName);

        MatchManager.removeMatch(this.name);
        
        QisCSGO.LOGGER.info("比赛 '{}' 的数据已清理。", this.name);
    }

    /**
     * 结束整场比赛。
     * @param winningTeam 最终获胜的队伍。
     */
    private void finishMatch(String winningTeam) {
        this.state = MatchState.FINISHED;
        Component winner = Component.literal(winningTeam).withStyle(winningTeam.equals("CT") ? ChatFormatting.BLUE : ChatFormatting.GOLD);
        broadcastToAllPlayersInMatch(Component.literal("比赛结束！胜利者是 ").append(winner).append("!"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, {}方胜利.", name, winningTeam);
        broadcastEndGameStats();
        removeScoreboard();
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            setPlayerKnockbackResistance(player, 0.0);
        }
        resetAndTeleportPlayers();
        cleanupMatchData();
    }

    /**
     * 处理平局情况。
     */
    private void handleTie() {
        this.state = MatchState.FINISHED;
        broadcastToAllPlayersInMatch(Component.literal("比赛平局！"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, 平局.", name);
        broadcastEndGameStats();
        removeScoreboard();
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            setPlayerKnockbackResistance(player, 0.0);
        }
        this.bossBar.removeAllPlayers();
        resetAndTeleportPlayers();
        cleanupMatchData();
    }

    /**
     * 标记一名玩家在本回合中死亡。
     * @param deadPlayer 死亡的玩家。
     */
    public void markPlayerAsDead(ServerPlayer deadPlayer) {
        if (!this.alivePlayers.contains(deadPlayer.getUUID())) return;
        
        this.lastTeammateDeathPos.put(getPlayerStats().get(deadPlayer.getUUID()).getTeam(), deadPlayer.blockPosition());
        
        this.alivePlayers.remove(deadPlayer.getUUID());
        PlayerStats stats = playerStats.get(deadPlayer.getUUID());
        if(stats != null) stats.incrementDeaths();
        
        QisCSGO.LOGGER.info("玩家 {} 在比赛 '{}' 中阵亡。", deadPlayer.getName().getString(), name);
        this.checkRoundEndCondition();
        
        deadPlayer.setGameMode(GameType.SPECTATOR);
        findAndSetSpectatorTarget(deadPlayer);
        
        this.checkRoundEndCondition();
    }
    
    /**
     * 锁定观察者视角到队友或最后死亡位置
     * @param deadPlayer 死亡的玩家
     */
    private void lockSpectatorToTeammateOrLastPos(ServerPlayer deadPlayer) {
        PlayerStats deadPlayerStats = getPlayerStats().get(deadPlayer.getUUID());
        if (deadPlayerStats == null) return;
        
        String team = deadPlayerStats.getTeam();
        
        ServerPlayer targetPlayer = null;
        for (UUID playerUUID : this.alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
                PlayerStats stats = playerStats.get(playerUUID);
                if (stats != null && team.equals(stats.getTeam())) {
                    targetPlayer = player;
                    break;
                }
            }
        }
        
        if (targetPlayer != null) {
            deadPlayer.setCamera(targetPlayer);
        } else {
            BlockPos lastDeathPos = this.lastTeammateDeathPos.get(team);
            if (lastDeathPos != null) {
                deadPlayer.teleportTo(lastDeathPos.getX(), lastDeathPos.getY() + 10, lastDeathPos.getZ());
            }
        }
    }
    
    /**
     * 更新所有观察者的视角目标
     */
    public void updateSpectatorCameras() {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            
            if (player != null && player.isSpectator()) {
                if (player.getCamera() == player) {
                    findAndSetSpectatorTarget(player);
                }
            }
        }
    }

    /**
     * 为指定的观察者寻找并设置一个新的观战目标。
     * 逻辑：随机附身存活队友 -> C4位置 -> 队伍出生点。
     * @param spectator 需要设置目标的观察者玩家。
     */
    private void findAndSetSpectatorTarget(ServerPlayer spectator) {
        PlayerStats spectatorStats = getPlayerStats().get(spectator.getUUID());
        if (spectatorStats == null) return;
        
        String team = spectatorStats.getTeam();

        // 1. 尝试寻找一个随机的、存活的队友
        List<ServerPlayer> aliveTeammates = alivePlayers.stream()
            .map(uuid -> server.getPlayerList().getPlayer(uuid))
            .filter(p -> p != null && team.equals(getPlayerStats().get(p.getUUID()).getTeam()))
            .collect(Collectors.toList());

        if (!aliveTeammates.isEmpty()) {
            ServerPlayer target = aliveTeammates.get(new Random().nextInt(aliveTeammates.size()));
            spectator.setCamera(target);
            return;
        }

        // 2. 如果没有存活的队友，检查C4是否已安放
        if (c4Manager.isC4Planted() && c4Manager.getC4Pos() != null) {
            BlockPos c4Pos = c4Manager.getC4Pos();
            spectator.teleportTo(server.overworld(), c4Pos.getX() + 0.5, c4Pos.getY() + 10, c4Pos.getZ() + 0.5, spectator.getYRot(), 90);
            return;
        }

        // 3. 如果C4也未安放，则传送到队伍的一个随机出生点
        List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;
        if (!spawns.isEmpty()) {
            BlockPos spawnPos = spawns.get(new Random().nextInt(spawns.size()));
            spectator.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, spectator.getYRot(), spectator.getXRot());
        }
    }
    
    /**
     * 处理玩家在比赛中重生的情况（强制设为观察者）。
     * @param respawningPlayer 重生的玩家。
     */
    public void handlePlayerRespawn(ServerPlayer respawningPlayer) {
        respawningPlayer.setGameMode(GameType.SPECTATOR);
        lockSpectatorToTeammateOrLastPos(respawningPlayer);
    }
    
    /**
     * 检查回合是否因一方全部阵亡而结束。
     */
    private void checkRoundEndCondition() {
        if (roundState != RoundState.IN_PROGRESS || playerStats.isEmpty() || currentRound == 0) {
            return;
        }

        long aliveCtCount = alivePlayers.stream().filter(uuid -> {
            PlayerStats stats = playerStats.get(uuid);
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            return stats != null && "CT".equals(stats.getTeam()) && p != null && p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
        }).count();

        long aliveTCount = alivePlayers.stream().filter(uuid -> {
            PlayerStats stats = playerStats.get(uuid);
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            return stats != null && "T".equals(stats.getTeam()) && p != null && p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
        }).count();

        // 如果C4已经安放，胜利逻辑会改变
        if (c4Manager.isC4Planted()) {
            // C4安放后，如果所有CT都阵亡了，T方立即获胜
            if (aliveCtCount == 0) {
                endRound("T", "所有CT玩家阵亡");
            }
        } else {
            // 如果所有T都阵亡了，CT方获胜
            if (aliveTCount == 0) {
                endRound("CT", "所有T玩家阵亡");
            } 
            // 如果所有CT都阵亡了，T方获胜
            else if (aliveCtCount == 0) {
                endRound("T", "所有CT玩家阵亡");
            }
        }
    }

    /**
     * 向指定队伍广播消息。
     * @param message 要广播的消息。
     * @param team 目标队伍 ("CT" 或 "T")。
     */
    public void broadcastToTeam(Component message, String team) {
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            if (team.equals(entry.getValue().getTeam())) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }

    // --- C4 逻辑委托 ---

    /**
     * 当C4被安放时，由C4Item调用此方法，再由Match委托给C4Manager处理
     * @param pos C4安放的位置
     */
    public void onC4Planted(BlockPos pos) {
        c4Manager.onC4Planted(pos);
    }
    
    /**
     * 当C4被安放后，C4Manager会调用此方法来更新Match的回合计时器
     */
    public void onC4PlantedUpdateMatchTimer(){
        // C4安放后，回合时间重置为40秒爆炸倒计时
        this.tickCounter = 40 * 20;
    }
    
    /**
     * 获取C4管理器实例
     * @return C4Manager实例
     */
    public C4Manager getC4Manager() {
        return this.c4Manager;
    }

    /**
     * 向比赛中的所有玩家广播一条消息。
     * @param message 要广播的消息组件。
     */
    public void broadcastToAllPlayersInMatch(Component message) {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.sendSystemMessage(message, false);
            }
        }
    }
    
    /**
     * 由管理员强制结束比赛。
     */
    public void forceEnd() {
        this.state = MatchState.FINISHED;
        broadcastToAllPlayersInMatch(Component.literal("比赛已被管理员强制结束。"));
        c4Manager.reset();
        removeShops();
        removeScoreboard();
        this.bossBar.removeAllPlayers();

        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.setGameMode(server.getDefaultGameType());
                setPlayerKnockbackResistance(player, 0.0);
                player.removeAllEffects();
            }
        }
        resetAndTeleportPlayers();
        cleanupMatchData();
    }

    /**
     * 创建比赛计分板。
     */
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

    /**
     * 每秒更新计分板上的玩家分数。
     */
    private void updateScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;

        scoreboardRebuildCounter++;
        if (scoreboardRebuildCounter >= 200) { 
            rebuildScoreboard();
            scoreboardRebuildCounter = 0;
            return;
        }

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

    /**
     * 定期重建计分板以移除已离开玩家的条目。
     */
    private void rebuildScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;
        
        String objectiveName = this.objective.getName();
        Component displayName = this.objective.getDisplayName();
        
        this.scoreboard.removeObjective(this.objective);
        
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

    /**
     * 在比赛结束时移除计分板。
     */
    private void removeScoreboard() {
        if (this.scoreboard != null && this.objective != null) {
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            Objective currentObjective = this.scoreboard.getObjective(this.objective.getName());
            if (currentObjective != null) {
                this.scoreboard.removeObjective(currentObjective);
            }
            this.objective = null;
        }
    }

    /**
     * 清理比赛区域内所有掉落的物品实体。
     */
    private void clearDroppedItems() {
        List<BlockPos> allPositions = new ArrayList<>();
        allPositions.addAll(ctSpawns);
        allPositions.addAll(tSpawns);
        if (bombsiteA != null) {
            allPositions.add(BlockPos.containing(bombsiteA.minX, bombsiteA.minY, bombsiteA.minZ));
            allPositions.add(BlockPos.containing(bombsiteA.maxX, bombsiteA.maxY, bombsiteA.maxZ));
        }
        if (bombsiteB != null) {
            allPositions.add(BlockPos.containing(bombsiteB.minX, bombsiteB.minY, bombsiteB.minZ));
            allPositions.add(BlockPos.containing(bombsiteB.maxX, bombsiteB.maxY, bombsiteB.maxZ));
        }

        if (allPositions.isEmpty()) {
            return;
        }

        double minX = allPositions.getFirst().getX();
        double minY = allPositions.getFirst().getY();
        double minZ = allPositions.getFirst().getZ();
        
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;

        for (BlockPos pos : allPositions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        AABB matchArea = new AABB(minX - 50, minY - 20, minZ - 50, maxX + 50, maxY + 20, maxZ + 50);

        List<ItemEntity> itemsToRemove = server.overworld().getEntitiesOfClass(ItemEntity.class, matchArea, (entity) -> true);

        for (ItemEntity itemEntity : itemsToRemove) {
            itemEntity.discard();
        }

        QisCSGO.LOGGER.info("比赛 '{}': 清理了 {} 个掉落物品。", this.name, itemsToRemove.size());
    }

    /**
     * 为指定玩家重新应用计分板。
     * @param player 需要接收计分板信息的玩家。
     */
    public void reapplyScoreboardToPlayer(ServerPlayer player) {
        if (this.scoreboard != null && this.objective != null) {
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
            PlayerStats stats = this.playerStats.get(player.getUUID());
            int currentKills = (stats != null) ? stats.getKills() : 0;
            this.scoreboard.getOrCreatePlayerScore(player, this.objective).set(currentKills);
        }
    }
    
    /**
     * 更新Boss栏的显示内容和进度。
     */
    private void updateBossBar() {
        switch (this.roundState) {
            case BUY_PHASE:
                int buyPhaseTotalTicks = ServerConfig.buyPhaseSeconds * 20;
                float buyProgress = (float) this.tickCounter / buyPhaseTotalTicks;
                this.bossBar.setName(Component.literal("购买阶段剩余: " + (this.tickCounter / 20 + 1) + "s"));
                this.bossBar.setColor(BossEvent.BossBarColor.GREEN);
                this.bossBar.setProgress(buyProgress);
                break;

            case IN_PROGRESS:
                if (c4Manager.isC4Planted()) {
                    int c4TotalTicks = 40 * 20;
                    float c4Progress = (float) this.tickCounter / c4TotalTicks;
                    this.bossBar.setName(Component.literal("C4即将爆炸: " + (this.tickCounter / 20 + 1) + "s").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    this.bossBar.setColor(BossEvent.BossBarColor.RED);
                    this.bossBar.setProgress(c4Progress);
                } else {
                    int roundTotalTicks = this.roundTimeSeconds * 20;
                    float roundProgress = (float) this.tickCounter / roundTotalTicks;
                    this.bossBar.setName(Component.literal("回合剩余时间: " + (this.tickCounter / 20 + 1) + "s"));
                    this.bossBar.setColor(BossEvent.BossBarColor.WHITE);
                    this.bossBar.setProgress(roundProgress);
                }
                break;

            case ROUND_END:
                this.bossBar.setName(Component.literal("回合结束"));
                this.bossBar.setColor(BossEvent.BossBarColor.YELLOW);
                this.bossBar.setProgress(1.0f);
                break;

            default:
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
    
    /**
     * 获取C4是否已安放的状态（委托给C4Manager）
     * @return 如果C4已安放返回true，否则返回false
     */
    public boolean isC4Planted() {
        return c4Manager.isC4Planted();
    }
    
    /**
     * 获取C4方块的位置（委托给C4Manager）
     * @return C4方块的位置，如果未安放返回null
     */
    public BlockPos getC4Pos() {
        return c4Manager.getC4Pos();
    }
    
    public void setBombsiteA(AABB area) { this.bombsiteA = area; }
    public void setBombsiteB(AABB area) { this.bombsiteB = area; }
    public AABB getBombsiteB() {return this.bombsiteB;}
    public AABB getBombsiteA() {return this.bombsiteA;}

    
    /**
     * 检查一个玩家是否在任何一个包点内。
     * @param player 要检查的玩家。
     * @return 如果玩家在包点内则返回true。
     */
    public boolean isPlayerInBombsite(ServerPlayer player) {
        return (bombsiteA != null && bombsiteA.contains(player.position())) || (bombsiteB != null && bombsiteB.contains(player.position()));
    }
    
    /**
     * 检查一个坐标是否在任何一个包点内。
     * @param pos 要检查的坐标。
     * @return 如果坐标在包点内则返回true。
     */
    public boolean isPosInBombsite(BlockPos pos) {
        return (bombsiteA != null && bombsiteA.contains(pos.getX(), pos.getY(), pos.getZ())) || (bombsiteB != null && bombsiteB.contains(pos.getX(), pos.getY(), pos.getZ()));
    }
    
    /**
     * 将一名玩家添加到比赛中。
     * @param player 要添加的玩家。
     * @param team   玩家要加入的队伍 ("CT" 或 "T")。
     */
    public void addPlayer(ServerPlayer player, String team) { 
        playerStats.put(player.getUUID(), new PlayerStats(team)); 
        reapplyScoreboardToPlayer(player);
        this.bossBar.addPlayer(player);
        setPlayerKnockbackResistance(player, 1000.0);
    }
    
    public void removePlayer(ServerPlayer player) { 
        playerStats.remove(player.getUUID()); 
        this.bossBar.removePlayer(player);
        setPlayerKnockbackResistance(player, 0.0);
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
    
    /**
     * 为一个指定的玩家设置击退抗性属性。
     * @param player 目标玩家。
     * @param amount 击退抗性的值（1000.0 为 100% 抗性, 0.0 为默认值）。
     */
    private void setPlayerKnockbackResistance(ServerPlayer player, double amount) {
        if (player != null) {
            String command = "attribute " + player.getName().getString() + " minecraft:generic.knockback_resistance base set " + amount;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        }
    }
    
    /**
     * 获取当前回合所有存活玩家的UUID集合。
     * @return 一个包含存活玩家UUID的Set。
     */
    public Set<UUID> getAlivePlayers() {
        return this.alivePlayers;
    }
}