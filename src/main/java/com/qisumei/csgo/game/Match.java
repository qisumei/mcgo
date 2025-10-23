package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.C4Controller;
import com.qisumei.csgo.c4.C4Manager;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.service.EconomyService;
import com.qisumei.csgo.game.preset.MatchPreset;
import com.qisumei.csgo.service.ServiceRegistry;
import com.qisumei.csgo.server.ServerCommandExecutor;
import com.qisumei.csgo.server.ServerCommandExecutorImpl;
import com.qisumei.csgo.events.match.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.Objects;

/**
 * Match类，管理一场CSGO比赛的整个生命周期和所有核心逻辑。
 * 包括比赛状态、队伍信息、地图设置、回合管理、玩家统计等。
 */
public class Match implements MatchContext {

    /**
     * 比赛状态枚举。
     * 使用枚举而非常量提供类型安全。
     */
    public enum MatchState { 
        /** 准备阶段 - 玩家加入中 */
        PREPARING, 
        /** 进行中 - 比赛正在进行 */
        IN_PROGRESS, 
        /** 已结束 - 比赛已完成 */
        FINISHED 
    }
    
    /**
     * 回合状态枚举。
     * 使用枚举而非常量提供类型安全。
     */
    public enum RoundState { 
        /** 购买阶段 - 玩家可以购买装备 */
        BUY_PHASE, 
        /** 进行中 - 回合战斗阶段 */
        IN_PROGRESS, 
        /** 回合结束 - 显示结果阶段 */
        ROUND_END, 
        /** 暂停 - 比赛暂停状态 */
        PAUSED 
    }

    // --- 比赛基础信息（使用 final 提高代码安全性和可读性）---
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

    // --- C4 管理---
    private final C4Controller c4Manager;

    // --- 回合状态 ---
    private int currentRound;
    private int ctScore;
    private int tScore;
    private RoundState roundState;
    private int tickCounter;
    private final Set<UUID> alivePlayers;
    private String lastRoundWinner;
    private final Set<UUID> roundSurvivors;
    // 记录每个队伍最后一名死亡玩家的位置，用于全队死亡后的观察视角
    private final Map<String, BlockPos> lastTeammateDeathPos = new HashMap<>();

    // --- Boss栏计时器 ---
    private final ServerBossEvent bossBar;

    // Area manager for map-related utilities
    private final MatchAreaManager areaManager;

    // --- 计分板管理器（职责委托） ---
    private final MatchScoreboard scoreboardManager;

    // --- 命令执行器，抽象化外部命令调用，降低耦合 ---
    private final ServerCommandExecutor commandExecutor;

    // --- 玩家相关服务（可注入） ---
    private final PlayerService playerService;

    // --- 经济服务（可注入） ---
    private final EconomyService economyService;

    // --- 事件总线，用于解耦各系统 ---
    private final MatchEventBus eventBus;
    
    // --- 换边服务，处理队伍交换逻辑 ---
    private final TeamSwapService teamSwapService;
    
    // --- 回合经济服务，处理经济分配逻辑 ---
    private final RoundEconomyService roundEconomyService;


    /**
     * Match类的构造函数，用于初始化一场新的比赛。
     * 保留一个便捷的公共构造器（向后兼容），并提供一个私有的全参构造器用于依赖注入。
     */
    public Match(String name, int maxPlayers, MinecraftServer server) {
        // 默认行为：创建默认实现的依赖并委托给全参构造器
        this(name, maxPlayers, server,
             null, // 不传入 c4Manager，私有构造器会创建绑定当前 Match 的默认实现
             null,
             null,
             null);
    }

    /**
     * 全依赖构造器，便于在测试或替代实现中注入自定义依赖（例如 C4Controller、MatchScoreboard、ServerCommandExecutor、PlayerService）。
     */
    public Match(String name, int maxPlayers, MinecraftServer server,
                 C4Controller c4Manager,
                 MatchScoreboard scoreboardManager,
                 ServerCommandExecutor commandExecutor,
                 PlayerService playerService) {
        this.name = name;
        this.state = MatchState.PREPARING;
        this.maxPlayers = maxPlayers;
        this.server = server;
        this.playerStats = new HashMap<>();
        this.ctSpawns = new ArrayList<>();
        this.tSpawns = new ArrayList<>();
        this.totalRounds = 12;
        this.roundTimeSeconds = 120; // 默认2分钟
        String safeName = name.replaceAll("[^a-zA-Z0-9_.-]", "");
        this.ctTeamName = safeName + "_CT";
        this.tTeamName = safeName + "_T";
        this.currentRound = 0;
        this.ctScore = 0;
        this.tScore = 0;
        this.roundState = RoundState.PAUSED;
        this.tickCounter = 0;
        this.alivePlayers = new HashSet<>();
        this.lastRoundWinner = "";
        this.roundSurvivors = new HashSet<>();

        // 初始化 MatchAreaManager（负责区域/掉落物逻辑）
        this.areaManager = new MatchAreaManager(this, () -> this.ctSpawns, () -> this.tSpawns);

        // 初始化 C4 管理器，如果外部没有提供则创建默认实现并注入当前 Match
        this.c4Manager = Objects.requireNonNullElseGet(c4Manager, () -> new C4Manager(this));

        // 初始化Boss栏
        this.bossBar = new ServerBossEvent(
            Component.literal("等待比赛开始..."),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
        );

        // 初始化计分板管理器，允许注入
        this.scoreboardManager = Objects.requireNonNullElseGet(scoreboardManager, () -> new MatchScoreboardManager(this));

        // 初始化命令执行器（允许注入）
        this.commandExecutor = Objects.requireNonNullElseGet(commandExecutor, () -> {
            ServerCommandExecutor svcExecutor = ServiceRegistry.get(ServerCommandExecutor.class);
            return svcExecutor != null ? svcExecutor : new ServerCommandExecutorImpl(server);
        });

        // 初始化玩家相关服务（允许注入），使用已准备好的 commandExecutor
        this.playerService = Objects.requireNonNullElseGet(playerService, () -> new MatchPlayerService(this.commandExecutor));

        // 初始化经济服务（优先使用 ServiceRegistry 中注册的实现）
        this.economyService = ServiceRegistry.get(EconomyService.class);
        
        // 初始化事件总线
        this.eventBus = new MatchEventBus();
        
        // 注册经济事件处理器，根据配置选择资金清空策略
        EconomyEventHandler.MoneyClearStrategy strategy = parseMoneyStrategy(ServerConfig.teamSwapMoneyStrategy);
        this.eventBus.registerListener(new EconomyEventHandler(strategy));
        
        // 初始化换边服务
        this.teamSwapService = new TeamSwapService(this.commandExecutor, this.playerService);
        
        // 初始化回合经济服务
        this.roundEconomyService = new RoundEconomyService(
            this.economyService != null ? this.economyService : new com.qisumei.csgo.service.EconomyServiceImpl()
        );
    }
    
    /**
     * 解析资金清空策略配置字符串
     */
    private static EconomyEventHandler.MoneyClearStrategy parseMoneyStrategy(String strategyStr) {
        try {
            return EconomyEventHandler.MoneyClearStrategy.valueOf(strategyStr);
        } catch (IllegalArgumentException e) {
            QisCSGO.LOGGER.warn("无效的换边资金策略配置: {}，使用默认策略 RESET_TO_PISTOL_ROUND", strategyStr);
            return EconomyEventHandler.MoneyClearStrategy.RESET_TO_PISTOL_ROUND;
        }
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
     * 防御性编程：检查必要的前置条件，避免在不完整的状态下开始比赛。
     */
    public void start() {
        // 防御性检查：如果比赛没有玩家，直接返回并记录日志，避免进入不安全的运行时代码路径。
        if (this.playerStats.isEmpty()) {
            QisCSGO.LOGGER.warn("尝试开始比赛 '{}'，但没有玩家注册；取消开始。", this.name);
            this.bossBar.setName(Component.literal("比赛无法开始：没有玩家"));
            return;
        }
        
        // 检查是否设置了出生点
        if (this.ctSpawns.isEmpty() || this.tSpawns.isEmpty()) {
            QisCSGO.LOGGER.error("尝试开始比赛 '{}'，但未设置完整的出生点（CT: {}, T: {}）", 
                this.name, this.ctSpawns.size(), this.tSpawns.size());
            broadcastToAllPlayersInMatch(Component.literal("§c比赛无法开始：未设置完整的出生点！").withStyle(ChatFormatting.RED));
            return;
        }

        this.state = MatchState.IN_PROGRESS;

        // 使用辅助方法遍历所有在线玩家，减少重复代码
        forEachOnlinePlayer((player, stats) -> setPlayerKnockbackResistance(player, 1000.0));
        // 委托计分板初始化
        if (scoreboardManager != null) {
            scoreboardManager.setupScoreboard();
        }
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
        
        forEachOnlinePlayer((player, stats) -> commandExecutor.executeGlobal("title " + player.getName().getString() + " title " + titleJson));

        // 全局聊天广播比分
        String chatJson = String.format(
            "[{\"text\":\"%s：\",\"color\":\"yellow\"},{\"text\":\"CT \",\"color\":\"blue\"},{\"text\":\"%d:%d\",\"color\":\"white\",\"bold\":true},{\"text\":\" T\",\"color\":\"gold\"}]",
            this.name, ctScore, tScore
        );
        commandExecutor.executeGlobal("tellraw @a " + chatJson);

        // 4. 设置回合状态为购买阶段
        this.roundState = RoundState.BUY_PHASE;
        this.tickCounter = ServerConfig.buyPhaseSeconds * 20;

        // 5. 传送玩家、清空背包、并发放该回合应有的装备
        teleportAndPreparePlayers();
        
        // 6. 处理经济，发放金钱
        distributeRoundIncome();


        // 7. 为所有玩家添加购买阶段的无敌效果
        int resistanceDuration = ServerConfig.buyPhaseSeconds * 20;
        forEachOnlinePlayer((player, stats) -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceDuration, 4, false, false, true)));

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
                    beginRoundInProgress(); // 1. 处理购买阶段结束
                } 
                // 2. 处理战斗阶段结束，并加入C4检查
                else if (roundState == RoundState.IN_PROGRESS && !c4Manager.isC4Planted()) {
                    endRound("CT", "时间耗尽");
                } 
                else if (roundState == RoundState.ROUND_END) {
                    if (this.state == MatchState.IN_PROGRESS) {
                        startNewRound(); // 3. 处理回合间歇结束
                    }
                }
            }
        }
        // 在每秒的 tick 中，更新计分板
        if (server.getTickCount() % 20 == 0) {
            scoreboardManager.updateScoreboard();
        }
        if (server.getTickCount() % 5 == 0) {
            updateSpectatorCameras();
        }
        
        // 每一tick都更新Boss栏，以保证进度条平滑
        updateBossBar();
    }

    /**
     * 【新增】计算并返回一个包围了比赛所有关键点的大致区域。
     * @return AABB 区域包围盒，如果没有设置任何点则返回null。
     */
    public AABB getMatchAreaBoundingBox() {
        return this.areaManager.getMatchAreaBoundingBox();
    }

    /**
     * 【重构】清理比赛区域内所有掉落的物品实体（委托给 areaManager）。
     */
    private void clearDroppedItems() {
        this.areaManager.clearDroppedItems();
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
                // Reset velocity to prevent "moved too quickly" warnings
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;
                player.sendSystemMessage(Component.literal("购买阶段请不要离开购买区域！").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /**
     * 在半场时交换双方队伍。
     * 使用事件系统解耦换边逻辑与经济系统，提高代码可维护性。
     */
    private void swapTeams() {
        broadcastToAllPlayersInMatch(Component.literal("半场换边！队伍已交换。").withStyle(ChatFormatting.YELLOW));
        
        // 交换比分
        int tempScore = this.ctScore;
        this.ctScore = this.tScore;
        this.tScore = tempScore;
        
        // 收集受影响的玩家
        Map<UUID, ServerPlayer> affectedPlayers = new HashMap<>();
        
        // 更新玩家队伍信息
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            PlayerStats stats = entry.getValue();
            String oldTeam = stats.getTeam();
            String newTeam = "CT".equals(oldTeam) ? "T" : "CT";
            stats.setTeam(newTeam);
            
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                affectedPlayers.put(entry.getKey(), player);
            }
        }
        
        // 使用TeamSwapService批量更新玩家队伍信息
        teamSwapService.updatePlayersTeam(affectedPlayers, playerStats, getCtTeamName(), getTTeamName());
        
        // 触发换边事件，让经济系统等监听器处理资金清空等逻辑
        // 这样实现了解耦：Match不需要知道经济系统如何处理资金
        if (!affectedPlayers.isEmpty()) {
            TeamSwapEvent event = new TeamSwapEvent(this, affectedPlayers, this.currentRound);
            eventBus.fireTeamSwapEvent(event);
            QisCSGO.LOGGER.info("比赛 '{}' 触发换边事件，影响 {} 名玩家", name, affectedPlayers.size());
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
            // 去重近战，避免重复给予铁剑等近战
            purgeDuplicateMelee(player);

            stats.clearRoundGear();

            player.setGameMode(GameType.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects();
            
            // 回合开始时确保玩家可见
            player.setInvisible(false);
            
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ServerConfig.buyPhaseSeconds * 20, 4, false, false, true));

            List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;
            
            if (spawns.isEmpty()) {
                QisCSGO.LOGGER.error("比赛 '{}' 无法传送 {} 队玩家，因为没有设置出生点！", this.name, team);
                player.sendSystemMessage(Component.literal("错误: " + team + " 队没有设置出生点！请联系管理员。").withStyle(ChatFormatting.RED));
                continue;
            }
            
            BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
            player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            // Reset velocity to prevent "moved too quickly" warnings
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
        }
    }

    /**
     * 选择性地清空玩家背包，保留货币、护甲等受保护物品。
     * @param player 需要被清空背包的玩家。
     */
    private void performSelectiveClear(ServerPlayer player) {
        this.playerService.performSelectiveClear(player);
    }

    // 去重近战：只保留第一把铁剑，其余清除
    private void purgeDuplicateMelee(ServerPlayer player) {
        int kept = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() == net.minecraft.world.item.Items.IRON_SWORD) {
                if (kept == 0) {
                    kept = 1; // 保留第一把
                } else {
                    player.getInventory().setItem(i, ItemStack.EMPTY); // 清除多余
                }
            }
        }
        player.getInventory().setChanged();
    }

    /**
     * 在手枪局为玩家发放初始装备。
     * @param player 接收装备的玩家。
     * @param team 玩家所属队伍。
     */
    private void giveInitialGear(ServerPlayer player, String team) {
        // 使用已注入的 commandExecutor，通过 MatchPlayerHelper 发放装备
        this.playerService.giveInitialGear(player, team);
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
            // 切换为生存模式并移除购买阶段的无敌效果，确保击杀判定正常
            player.setGameMode(GameType.SURVIVAL);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        
        recordAllPlayerGear();
        
        broadcastToAllPlayersInMatch(Component.literal("战斗开始！"));
        QisCSGO.LOGGER.info("比赛 '{}': 进入战斗阶段。", name);
    }

    /**
     * 记录所有玩家在战斗阶段开始时的装备，用于胜利后保留装备。
     */
    private void recordAllPlayerGear() {
        forEachOnlinePlayer((player, stats) -> stats.setRoundGear(this.playerService.capturePlayerGear(player)));
    }

    /**
     * 在回合开始时为玩家发放收入。
     * 使用 RoundEconomyService 处理经济分配逻辑，实现解耦。
     */
    private void distributeRoundIncome() {
         boolean isPistolRound = (currentRound == 1 || currentRound == (totalRounds / 2) + 1);

         forEachOnlinePlayer((player, stats) -> {
            if (isPistolRound) {
                // 手枪局：使用服务设置起始资金
                roundEconomyService.distributePistolRoundMoney(player);
            } else {
                // 普通回合：根据上回合结果分配收入
                roundEconomyService.distributeRoundIncome(player, stats, this.lastRoundWinner);
            }
        });
     }

    /**
     * 根据击杀数（主要）和死亡数（次要，越少越好）获取指定队伍的顶尖玩家列表。
     * @param team 要查找的队伍 ("CT" 或 "T")。
     * @return 包含玩家UUID和其统计数据的有序列表。
     */
    private List<Map.Entry<UUID, PlayerStats>> getRankedPlayers(String team) {
        return playerStats.entrySet().stream()
            .filter(entry -> team.equals(entry.getValue().getTeam()))
            .sorted(Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getKills())
                          .reversed()
                          .thenComparingInt(e -> e.getValue().getDeaths()))
            .limit(3)
            .toList();
    }
    
    /**
     * 查找并以特定格式广播比赛结束时的统计数据。
     */
    private void broadcastEndGameStats() {
        final String SEPARATOR = "============================";
        final String COLUMN_SPACER = "           ";
        final int NAME_PADDING = 12;

        List<Map.Entry<UUID, PlayerStats>> topCtPlayers = getRankedPlayers("CT");
        List<Map.Entry<UUID, PlayerStats>> topTPlayers = getRankedPlayers("T");

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

        forEachOnlinePlayer((player, stats) -> {
            player.setGameMode(GameType.SURVIVAL);
            player.removeAllEffects();
            setPlayerKnockbackResistance(player, 0.0);
            player.getInventory().clearContent();
            player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        });
     }

    /**
     * 清理所有与本场比赛相关的服务器数据，如队伍和比赛实例。
     */
    private void cleanupMatchData() {
        commandExecutor.executeGlobal("team remove " + ctTeamName);
        commandExecutor.executeGlobal("team remove " + tTeamName);

        com.qisumei.csgo.service.ServiceFallbacks.removeMatch(this.name);
        
        QisCSGO.LOGGER.info("比赛 '{}' 的数据已清理。", this.name);
    }

    /**
     * 结束整场比赛。
     * @param winningTeam 最终获胜的队伍。
     */
    @SuppressWarnings("unused")
    private void finishMatch(String winningTeam) {
        this.state = MatchState.FINISHED;
        Component winner = Component.literal(winningTeam).withStyle(winningTeam.equals("CT") ? ChatFormatting.BLUE : ChatFormatting.GOLD);
        broadcastToAllPlayersInMatch(Component.literal("比赛结束！胜利者是 ").append(winner).append("!"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, {}方胜利.", name, winningTeam);
        broadcastEndGameStats();
        scoreboardManager.removeScoreboard();
        forEachOnlinePlayer((player, stats) -> setPlayerKnockbackResistance(player, 0.0));
        this.bossBar.removeAllPlayers();
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
        scoreboardManager.removeScoreboard();
        forEachOnlinePlayer((player, stats) -> setPlayerKnockbackResistance(player, 0.0));

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
        
        // 死亡时清空头盔与胸甲
        try {
            deadPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            deadPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            deadPlayer.getInventory().setChanged();
        } catch (Exception ignored) {}

        this.lastTeammateDeathPos.put(getPlayerStats().get(deadPlayer.getUUID()).getTeam(), deadPlayer.blockPosition());
        
        this.alivePlayers.remove(deadPlayer.getUUID());
        PlayerStats stats = playerStats.get(deadPlayer.getUUID());
        if(stats != null) stats.incrementDeaths();
        
        QisCSGO.LOGGER.info("玩家 {} 在比赛 '{}' 中阵亡。", deadPlayer.getName().getString(), name);
        this.checkRoundEndCondition();
        
        deadPlayer.setGameMode(GameType.SPECTATOR);
        // 隐藏观战者，避免泄露战术信息
        deadPlayer.setInvisible(true);
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
     * 优化：减少频繁切换，提升流畅度
     */
    public void updateSpectatorCameras() {
        forEachOnlinePlayer((player, stats) -> {
            if (player != null && player.isSpectator()) {
                // 只有当前没有有效观战目标时才切换
                Entity currentCamera = player.getCamera();
                if (currentCamera == player || currentCamera == null || 
                    (currentCamera instanceof ServerPlayer target && !target.isAlive())) {
                    findAndSetSpectatorTarget(player);
                }
            }
        });
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
            .toList();

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

        // 【调试日志】记录当前存活情况
        QisCSGO.LOGGER.debug("比赛 '{}' 第{}回合检查结束条件: CT存活={}, T存活={}, C4已安放={}",
            name, currentRound, aliveCtCount, aliveTCount, c4Manager.isC4Planted());

        // 如果C4已经安放，胜利逻辑会改变
        if (c4Manager.isC4Planted()) {
            // C4安放后，如果所有CT都阵亡了，T方立即获胜
            if (aliveCtCount == 0 && aliveTCount > 0) {
                QisCSGO.LOGGER.info("比赛 '{}' 第{}回合: CT全灭，C4已安放，T方获胜", name, currentRound);
                endRound("T", "所有CT玩家阵亡");
            }
            // 如果CT和T同时全灭（理论上不应该发生，但做防御性检查）
            else if (aliveCtCount == 0 && aliveTCount == 0) {
                QisCSGO.LOGGER.warn("比赛 '{}' 第{}回合: 双方同时全灭，C4已安放，T方获胜", name, currentRound);
                endRound("T", "双方同时阵亡，C4已安放");
            }
        } else {
            // C4未安放的情况
            // 【修复】检查T方是否全灭（优先判断，因为没有C4的T方更容易输）
            if (aliveTCount == 0 && aliveCtCount > 0) {
                QisCSGO.LOGGER.info("比赛 '{}' 第{}回合: T全灭，CT方获胜", name, currentRound);
                endRound("CT", "所有T玩家阵亡");
            }
            // 检查CT方是否全灭
            else if (aliveCtCount == 0 && aliveTCount > 0) {
                QisCSGO.LOGGER.info("比赛 '{}' 第{}回合: CT全灭，T方获胜", name, currentRound);
                endRound("T", "所有CT玩家阵亡");
            }
            // 双方同时全灭（极少发生的边界情况）
            // 在没有C4的情况下，双方同时全灭则CT方获胜（因为T方需要完成安放C4的目标）
            else if (aliveCtCount == 0 && aliveTCount == 0) {
                QisCSGO.LOGGER.warn("比赛 '{}' 第{}回合: 双方同时全灭，CT方获胜（T方未完成目标）", name, currentRound);
                endRound("CT", "双方同时阵亡");
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


    /**
     * 当C4被安放时，由C4Item调用此方法，再由Match委托给C4Manager处理
     * @param pos C4安放的位置
     */
    public void onC4Planted(BlockPos pos) {
        c4Manager.onC4Planted(pos);
    }

    /**
     * 获取C4管理器实例
     * @return C4Manager实例
     */
    public C4Controller getC4Manager() {
        return this.c4Manager;
    }

    /**
     * 向比赛中的所有玩家广播一条消息。
     * @param message 要广播的消息组件。
     */
    public void broadcastToAllPlayersInMatch(Component message) {
        forEachOnlinePlayer((player, stats) -> player.sendSystemMessage(message, false));
    }
    
    /**
     * 由管理员强制结束比赛。
     */
     public void forceEnd() {
         this.state = MatchState.FINISHED;
         broadcastToAllPlayersInMatch(Component.literal("比赛已被管理员强制结束。"));
         c4Manager.reset();
         scoreboardManager.removeScoreboard();
         this.bossBar.removeAllPlayers();

         forEachOnlinePlayer((player, stats) -> {
             player.setGameMode(server.getDefaultGameType());
             setPlayerKnockbackResistance(player, 0.0);
             player.removeAllEffects();
         });
         resetAndTeleportPlayers();
         cleanupMatchData();
     }


    /**
     * 更新Boss栏的显示内容和进度。
     * 使用 Java 21 增强的 switch 表达式以提高代码可读性。
     */
    private void updateBossBar() {
        switch (this.roundState) {
            case BUY_PHASE -> {
                int buyPhaseTotalTicks = ServerConfig.buyPhaseSeconds * 20;
                float buyProgress = (float) this.tickCounter / buyPhaseTotalTicks;
                this.bossBar.setName(Component.literal("购买阶段剩余: " + (this.tickCounter / 20 + 1) + "s"));
                this.bossBar.setColor(BossEvent.BossBarColor.GREEN);
                this.bossBar.setProgress(buyProgress);
            }
            case IN_PROGRESS -> {
                if (c4Manager.isC4Planted()) {
                    int c4TotalTicks = 40 * 20;
                    int c4TicksLeft = c4Manager.getC4TicksLeft();
                    float c4Progress = (float) c4TicksLeft / c4TotalTicks;
                    this.bossBar.setName(Component.literal("C4即将爆炸: " + (c4TicksLeft / 20 + 1) + "s").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    this.bossBar.setColor(BossEvent.BossBarColor.RED);
                    this.bossBar.setProgress(c4Progress);
                } else {
                    int roundTotalTicks = this.roundTimeSeconds * 20;
                    float roundProgress = (float) this.tickCounter / roundTotalTicks;
                    this.bossBar.setName(Component.literal("回合剩余时间: " + (this.tickCounter / 20 + 1) + "s"));
                    this.bossBar.setColor(BossEvent.BossBarColor.WHITE);
                    this.bossBar.setProgress(roundProgress);
                }
            }
            case ROUND_END -> {
                this.bossBar.setName(Component.literal("回合结束"));
                this.bossBar.setColor(BossEvent.BossBarColor.YELLOW);
                this.bossBar.setProgress(1.0f);
            }
            case PAUSED -> {
                this.bossBar.setName(Component.literal("比赛暂停"));
                this.bossBar.setColor(BossEvent.BossBarColor.PURPLE);
                this.bossBar.setProgress(1.0f);
            }
        }
    }

    // --- Getters and Setters ---
    
    public String getName() { return name; }
    public MatchState getState() { return state; }
    @SuppressWarnings("unused")
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
        scoreboardManager.reapplyToPlayer(player);
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
            commandExecutor.executeGlobal(command);
        }
    }
    
    /**
     * 获取当前回合所有存活玩家的UUID集合。
     * @return 一个包含存活玩家UUID的Set。
     */
    public Set<UUID> getAlivePlayers() {
        return this.alivePlayers;
    }

    /**
     * 重新给单个玩家应用计分板（对外桥接方法，委托给 MatchScoreboardManager）。
     * @param player 目标玩家
     */
    public void reapplyScoreboardToPlayer(ServerPlayer player) {
        if (this.scoreboardManager != null) this.scoreboardManager.reapplyToPlayer(player);
    }

    /**
     * 遍历所有已登记并且在线的玩家，将 ServerPlayer 和 对应的 PlayerStats 传给 action。
     */
    private void forEachOnlinePlayer(BiConsumer<ServerPlayer, PlayerStats> action) {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;
            PlayerStats stats = playerStats.get(playerUUID);
            if (stats == null) continue;
            action.accept(player, stats);
        }
    }

    /**
     * 结束当前回合的逻辑。
     * @param winningTeam 获胜的队伍 ("CT" 或 "T")。
     * @param reason 获胜的原因。
     */
    @Override
    public void endRound(String winningTeam, String reason) {
         if (this.roundState == RoundState.ROUND_END) return;
         this.roundState = RoundState.ROUND_END;
         this.lastRoundWinner = winningTeam;

         // 结算比分
         if ("CT".equals(winningTeam)) {
             this.ctScore++;
         } else {
             this.tScore++;
         }

         // 记录本回合幸存者，供下回合发放装备时使用
         this.roundSurvivors.clear();
         this.roundSurvivors.addAll(this.alivePlayers);

         // 记录连胜/连败
         for (UUID playerUUID : playerStats.keySet()) {
             PlayerStats stats = playerStats.get(playerUUID);
             if (stats != null) {
                 if (stats.getTeam().equals(winningTeam)) {
                     stats.resetConsecutiveLosses();
                 } else {
                     stats.incrementConsecutiveLosses();
                 }
             }
         }
         
         // 给获胜方玩家奖励（使用 RoundEconomyService）
         forEachOnlinePlayer((player, stats) -> {
             if (stats.getTeam().equals(winningTeam)) {
                 roundEconomyService.distributeWinReward(player);
             }
         });

         // 更新统计信息面板
         scoreboardManager.updateScoreboard();
         
         // 广播回合结束信息（包含原因）
         String message = String.format("第 %d 回合结束！%s 获胜！原因：%s",
             currentRound,
             winningTeam.equals("CT") ? "反恐精英 (CT)" : "恐怖分子 (T)",
             reason);
         broadcastToAllPlayersInMatch(Component.literal(message).withStyle(ChatFormatting.YELLOW));

         // 检查是否打满回合，决定结束比赛还是进入下一回合
         if (currentRound >= totalRounds) {
             // 比赛打满了，根据比分决定胜负或平局
             if (ctScore == tScore) {
                 handleTie();
             } else {
                 String overallWinner = (ctScore > tScore) ? "CT" : "T";
                 finishMatch(overallWinner);
             }
             return; // 比赛已结束，不设置下一回合倒计时
         }

         // 设置回合结束展示时间，随后自动开始下一回合
         this.tickCounter = ServerConfig.roundEndSeconds * 20;
     }
}