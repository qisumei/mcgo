package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.game.preset.MatchPreset;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CSGO比赛核心类，管理一场比赛的完整生命周期、状态机和所有游戏逻辑。
 * <p>
 * 这个类是整个模组最核心的部分，它负责处理回合的开始与结束、玩家状态管理、
 * C4逻辑、经济系统、计分板、Boss栏等所有与单场比赛相关的事务。
 * </p>
 *
 * @author Qisumei
 */
public class Match {

    // --- 枚举定义 ---

    /**
     * 比赛的整体状态。
     */
    public enum MatchState { PREPARING, IN_PROGRESS, FINISHED }

    /**
     * 单个回合内的具体阶段。
     */
    public enum RoundState { BUY_PHASE, IN_PROGRESS, ROUND_END, PAUSED }

    // --- 常量定义 ---

    private static final int DEFUSE_TIME_TICKS = 6 * 20; // 空手拆弹需要6秒 (120 ticks)
    private static final int DEFUSE_TIME_WITH_KIT_TICKS = 3 * 20; // 使用拆弹器需要3秒 (60 ticks)
    private static final int C4_TIMER_TICKS = 40 * 20; // C4爆炸倒计时40秒
    private static final int C4_BROADCAST_COOLDOWN_TICKS = 20; // C4掉落位置广播间隔 (1秒)
    private static final double BUY_ZONE_MAX_DISTANCE = 10.0; // 购买区域最大允许距离
    private static final double C4_EXPLOSION_RADIUS = 16.0; // C4爆炸伤害半径
    private static final float C4_EXPLOSION_MAX_DAMAGE = 100.0f; // C4爆炸最大伤害
    private static final int SCOREBOARD_REBUILD_INTERVAL_TICKS = 200; // 计分板重建间隔 (10秒)

    // --- 比赛基础信息 ---
    private final String name;
    private final int maxPlayers;
    private final MinecraftServer server;
    private MatchState state;
    private int totalRounds;
    private int roundTimeSeconds;

    // --- 队伍与玩家数据 ---
    private final String ctTeamName;
    private final String tTeamName;
    private final Map<UUID, PlayerStats> playerStats;
    private final Set<UUID> alivePlayers;
    private final Map<String, BlockPos> lastTeammateDeathPos; // 记录每队最后死亡玩家的位置，用于观察

    // --- 地图配置信息 ---
    private final List<BlockPos> ctSpawns;
    private final List<BlockPos> tSpawns;
    private BlockPos ctShopPos;
    private BlockPos tShopPos;
    private AABB bombsiteA;
    private AABB bombsiteB;

    // --- 回合状态机 ---
    private RoundState roundState;
    private int currentRound;
    private int ctScore;
    private int tScore;
    private int tickCounter; // 主计时器，用于回合时间、购买时间等
    private String lastRoundWinner;
    private final Set<UUID> roundSurvivors;

    // --- C4 相关状态 ---
    private final C4CountdownHandler c4CountdownHandler;
    private boolean c4Planted;
    private BlockPos c4Pos;
    private int c4BroadcastCooldown;
    private final Map<UUID, Integer> defusingPlayers; // 追踪玩家的拆弹进度

    // --- UI与显示元素 ---
    private final ServerBossEvent bossBar;
    private Scoreboard scoreboard;
    private Objective objective;
    private int scoreboardRebuildCounter;

    /**
     * Match类的构造函数，用于初始化一场新的比赛。
     *
     * @param name       比赛的唯一名称。
     * @param maxPlayers 比赛的最大玩家数。
     * @param server     Minecraft服务器实例。
     */
    public Match(String name, int maxPlayers, MinecraftServer server) {
        // 初始化比赛基础信息
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.server = server;
        this.state = MatchState.PREPARING;
        this.totalRounds = 12;
        this.roundTimeSeconds = 120; // 默认2分钟

        // 初始化队伍与玩家数据
        this.ctTeamName = name + "_CT";
        this.tTeamName = name + "_T";
        this.playerStats = new HashMap<>();
        this.alivePlayers = new HashSet<>();
        this.lastTeammateDeathPos = new HashMap<>();

        // 初始化地图配置
        this.ctSpawns = new ArrayList<>();
        this.tSpawns = new ArrayList<>();

        // 初始化回合状态
        this.roundState = RoundState.PAUSED;
        this.currentRound = 0;
        this.ctScore = 0;
        this.tScore = 0;
        this.tickCounter = 0;
        this.lastRoundWinner = "";
        this.roundSurvivors = new HashSet<>();

        // 初始化C4相关
        this.c4CountdownHandler = new C4CountdownHandler(this);
        this.c4Planted = false;
        this.c4Pos = null;
        this.c4BroadcastCooldown = 0;
        this.defusingPlayers = new HashMap<>();

        // 初始化UI元素
        this.bossBar = new ServerBossEvent(
                Component.literal("等待比赛开始..."),
                BossEvent.BossBarColor.WHITE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.scoreboardRebuildCounter = 0;
    }

    // =================================================================================
    // SECTION: 比赛主逻辑流程 (Main Match Flow)
    // =================================================================================

    /**
     * 正式开始比赛。
     * 此方法将比赛状态从未开始转为进行中，并启动第一个回合。
     */
    public void start() {
        this.state = MatchState.IN_PROGRESS;

        // 为所有参赛玩家设置高击退抗性
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            setPlayerKnockbackResistance(player, 1000.0);
        }

        // 初始化计分板
        setupScoreboard();
        broadcastToAllPlayersInMatch(Component.literal("比赛开始！"));
        // 开始新回合
        startNewRound();
    }

    /**
     * 比赛的核心更新方法，每tick被MatchManager调用。
     * 这是驱动所有回合内逻辑状态变化的地方。
     */
    public void tick() {
        if (state != MatchState.IN_PROGRESS) return;

        // 1. 更新C4倒计时器（如果已安放）
        c4CountdownHandler.tick();

        // 2. 检查并广播掉落的C4位置
        tickC4DropLocation();

        // 3. 在购买阶段，检查玩家是否离开购买区域
        if (roundState == RoundState.BUY_PHASE) {
            checkPlayerBuyZone();
        }

        // 4. 处理主计时器
        if (tickCounter > 0) {
            tickCounter--;
            if (tickCounter == 0) {
                // 根据当前回合状态，在计时器归零时触发相应事件
                switch (roundState) {
                    case BUY_PHASE -> beginRoundInProgress(); // 购买阶段结束，开始战斗
                    case IN_PROGRESS -> endRound("CT", "时间耗尽"); // 回合时间到，CT胜利
                    case ROUND_END -> {
                        if (this.state == MatchState.IN_PROGRESS) {
                            startNewRound(); // 回合结束展示时间到，开始新回合
                        }
                    }
                    // [修复] 统一使用箭头表达式
                    case PAUSED -> {
                        // 在PAUSED状态下，不执行任何操作
                    }
                    default -> {
                        // 在其他未定义状态下，不执行任何操作
                    }
                }
            }
        }

        // 5. 定期更新UI
        if (server.getTickCount() % 20 == 0) {
            updateScoreboard(); // 每秒更新计分板
        }
        if (server.getTickCount() % 5 == 0) {
            updateSpectatorCameras(); // 每1/4秒更新观察者视角
        }
        updateBossBar(); // 每tick更新Boss栏进度条
    }

    /**
     * 开始一个新回合的完整流程。
     * 这是比赛中最重要的逻辑序列之一。
     */
    private void startNewRound() {
        // 步骤 1: 清理战场，移除上回合的掉落物
        clearDroppedItems();

        // 步骤 2: 推进回合数，重置C4状态
        this.currentRound++;
        resetC4State();

        // 步骤 3: 检查是否到达半场，如果需要则交换队伍
        if (this.currentRound == (this.totalRounds / 2) + 1) {
            swapTeams();
        }

        // 步骤 4: 广播当前比分
        broadcastScoreUpdate();

        // 步骤 5: 设置回合状态为购买阶段，并重置计时器
        this.roundState = RoundState.BUY_PHASE;
        this.tickCounter = ServerConfig.buyPhaseSeconds * 20;

        // 步骤 6: 传送玩家，清空背包，并发放本回合应有的装备
        teleportAndPreparePlayers();

        // 步骤 7: 在装备发放完毕后，处理经济，发放金钱
        distributeRoundIncome();

        // 步骤 8: 生成商店村民
        spawnShops();

        // 步骤 9: 广播回合开始消息
        broadcastToAllPlayersInMatch(Component.literal("第 " + this.currentRound + " 回合开始！购买阶段！"));
        QisCSGO.LOGGER.info("比赛 '{}': 第 {} 回合开始，进入购买阶段。", name, currentRound);

        // 步骤 10: 最后为T阵营随机一名玩家发放C4
        giveC4ToRandomT();
    }

    /**
     * 结束购买阶段，进入回合的战斗阶段。
     */
    private void beginRoundInProgress() {
        this.roundState = RoundState.IN_PROGRESS;
        this.tickCounter = this.roundTimeSeconds * 20;

        // 再次传送玩家回出生点，防止有玩家在购买阶段偷跑
        teleportPlayersToSpawns();

        // 记录所有玩家在战斗阶段开始时的装备，用于胜利后保留
        recordAllPlayerGear();

        // 移除商店村民
        removeShops();

        broadcastToAllPlayersInMatch(Component.literal("战斗开始！"));
        QisCSGO.LOGGER.info("比赛 '{}': 进入战斗阶段。", name);
    }

    /**
     * 结束当前回合的逻辑。
     *
     * @param winningTeam 获胜的队伍 ("CT" 或 "T")。
     * @param reason      获胜的原因（例如 "全歼敌人"、"炸弹爆炸"）。
     */
    private void endRound(String winningTeam, String reason) {
        if (this.roundState == RoundState.ROUND_END) return; // 防止重复调用

        this.roundState = RoundState.ROUND_END;
        this.lastRoundWinner = winningTeam;

        // 记录本回合的幸存者
        this.roundSurvivors.clear();
        this.roundSurvivors.addAll(this.alivePlayers);

        // 更新比分和连败记录
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

        // 检查比赛是否结束
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
            // 如果比赛未结束，则设置回合结束的等待时间
            this.tickCounter = ServerConfig.roundEndSeconds * 20;
        }
    }

    /**
     * 结束整场比赛。
     *
     * @param winningTeam 最终获胜的队伍。
     */
    private void finishMatch(String winningTeam) {
        this.state = MatchState.FINISHED;
        Component winnerText = Component.literal(winningTeam).withStyle(winningTeam.equals("CT") ? ChatFormatting.BLUE : ChatFormatting.GOLD);
        broadcastToAllPlayersInMatch(Component.literal("比赛结束！胜利者是 ").append(winnerText).append("!"));
        QisCSGO.LOGGER.info("比赛 '{}' 结束, {}方胜利.", name, winningTeam);

        // 清理和收尾工作
        broadcastEndGameStats();
        resetAndTeleportPlayers();
        cleanupMatchData();
    }

    /**
     * 由管理员强制结束比赛。
     */
    public void forceEnd() {
        this.state = MatchState.FINISHED;
        broadcastToAllPlayersInMatch(Component.literal("比赛已被管理员强制结束。"));

        // 清理和收尾工作
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

        // 清理和收尾工作
        broadcastEndGameStats();
        resetAndTeleportPlayers();
        cleanupMatchData();
    }

    /**
     * 清理所有与本场比赛相关的服务器数据。
     */
    private void cleanupMatchData() {
        removeShops();
        removeScoreboard();
        this.bossBar.removeAllPlayers();

        // 移除为本场比赛创建的队伍
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team remove " + ctTeamName);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team remove " + tTeamName);

        // 从比赛管理器中移除本场比赛
        MatchManager.removeMatch(this.name);

        QisCSGO.LOGGER.info("比赛 '{}' 的数据已清理。", this.name);
    }

    // =================================================================================
    // SECTION: 玩家状态与行为处理 (Player State & Actions)
    // =================================================================================

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

            // 根据上回合结果决定是否保留装备
            boolean wasWinner = stats.getTeam().equals(this.lastRoundWinner);
            boolean wasSurvivor = this.roundSurvivors.contains(playerUUID);

            performSelectiveClear(player);

            if (isPistolRound) {
                // 手枪局给予初始装备
                giveInitialGear(player, stats.getTeam());
            } else {
                // 如果是胜利方的幸存者，则恢复上回合的装备
                if (wasSurvivor && wasWinner) {
                    stats.getRoundGear().forEach(gear -> player.getInventory().add(gear.copy()));
                }
            }
            stats.clearRoundGear();

            // 重置玩家状态
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects();

            // 给予购买阶段无敌效果
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ServerConfig.buyPhaseSeconds * 20, 4, false, false, true));

            // 传送到随机出生点
            List<BlockPos> spawns = "CT".equals(stats.getTeam()) ? ctSpawns : tSpawns;
            if (spawns.isEmpty()) {
                QisCSGO.LOGGER.error("比赛 '{}' 无法传送 {} 队玩家，因为没有设置出生点！", this.name, stats.getTeam());
                player.sendSystemMessage(Component.literal("错误: " + stats.getTeam() + " 队没有设置出生点！请联系管理员。").withStyle(ChatFormatting.RED));
                continue;
            }
            BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
            player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
    }

    /**
     * 标记一名玩家在本回合中死亡，并检查回合是否因此结束。
     *
     * @param deadPlayer 死亡的玩家。
     */
    public void markPlayerAsDead(ServerPlayer deadPlayer) {
        if (!this.alivePlayers.contains(deadPlayer.getUUID())) return;

        // 记录死亡位置，用于队友观察
        PlayerStats deadPlayerStats = getPlayerStats().get(deadPlayer.getUUID());
        if (deadPlayerStats != null) {
            this.lastTeammateDeathPos.put(deadPlayerStats.getTeam(), deadPlayer.blockPosition());
        }

        // 从存活玩家列表中移除
        this.alivePlayers.remove(deadPlayer.getUUID());
        if (deadPlayerStats != null) deadPlayerStats.incrementDeaths();

        QisCSGO.LOGGER.info("玩家 {} 在比赛 '{}' 中阵亡。", deadPlayer.getName().getString(), name);

        // 将死亡玩家设为观察者模式并立即为其寻找观战目标
        deadPlayer.setGameMode(GameType.SPECTATOR);
        findAndSetSpectatorTarget(deadPlayer);

        // 检查回合是否结束
        checkRoundEndCondition();
    }

    /**
     * 处理玩家在比赛中重生的情况（强制设为观察者）。
     *
     * @param respawningPlayer 重生的玩家。
     */
    public void handlePlayerRespawn(ServerPlayer respawningPlayer) {
        respawningPlayer.setGameMode(GameType.SPECTATOR);
        findAndSetSpectatorTarget(respawningPlayer);
    }

    /**
     * 检查回合是否因一方全部阵亡而结束。
     */
    private void checkRoundEndCondition() {
        if (roundState != RoundState.IN_PROGRESS || playerStats.isEmpty() || currentRound == 0) return;

        // 统计双方存活人数
        long aliveCtCount = alivePlayers.stream().filter(uuid -> "CT".equals(playerStats.get(uuid).getTeam())).count();
        long aliveTCount = alivePlayers.stream().filter(uuid -> "T".equals(playerStats.get(uuid).getTeam())).count();

        if (c4Planted) {
            // C4安放后，如果所有CT都阵亡了，T方立即获胜
            if (aliveCtCount == 0) {
                endRound("T", "所有CT玩家阵亡");
            }
            // 如果T方全部阵亡，回合会继续，直到C4爆炸或被拆除。
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


    // =================================================================================
    // SECTION: C4 逻辑 (C4 Logic)
    // =================================================================================

    /**
     * 当C4被安放时由 {@link com.qisumei.csgo.c4.item.C4Item} 调用。
     *
     * @param pos C4被安放的位置坐标。
     */
    public void onC4Planted(BlockPos pos) {
        this.c4Planted = true;
        this.c4Pos = pos;
        this.tickCounter = C4_TIMER_TICKS; // 重置主计时器为C4倒计时
        c4CountdownHandler.start(pos);

        // 判断安放点
        String siteName = "未知地点";
        if (bombsiteA != null && bombsiteA.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
            siteName = "A点";
        } else if (bombsiteB != null && bombsiteB.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
            siteName = "B点";
        }

        Component message = Component.literal("§e[信息] §f炸弹已安放在 §a" + siteName + "！");
        broadcastToAllPlayersInMatch(message);
    }

    /**
     * 当C4被成功拆除时由 {@link com.qisumei.csgo.c4.block.C4Block} 调用。
     */
    public void onC4Defused() {
        if (this.roundState == RoundState.IN_PROGRESS) {
            c4CountdownHandler.stop();
            defusingPlayers.clear();
            endRound("CT", "炸弹已被拆除");
        }
    }

    /**
     * 当C4爆炸时由 {@link C4CountdownHandler} 调用。
     */
    public void onC4Exploded() {
        if (c4Pos != null) {
            // 造成自定义的范围伤害
            applyCustomExplosionDamage();

            // [修复] SoundEvents API 变更
            // playSound 需要一个实际的 SoundEvent 对象，而不是 Holder。
            // 我们需要通过调用 .value() 来获取它。
            server.overworld().playSound(null, c4Pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (server.overworld().random.nextFloat() - server.overworld().random.nextFloat()) * 0.2F) * 0.7F);
            server.overworld().sendParticles(ParticleTypes.EXPLOSION_EMITTER, c4Pos.getX() + 0.5, c4Pos.getY() + 0.5, c4Pos.getZ() + 0.5, 2, 1.0, 1.0, 1.0, 0.0);
        }
        endRound("T", "炸弹已爆炸");
    }

    /**
     * 在每tick中处理CT玩家的拆弹逻辑。
     *
     * @param player 正在被检测的玩家。
     */
    public void handlePlayerDefuseTick(ServerPlayer player) {
        if (isPlayerEligibleToDefuse(player)) {
            // 推进拆弹进度
            int currentProgress = defusingPlayers.getOrDefault(player.getUUID(), 0) + 1;
            defusingPlayers.put(player.getUUID(), currentProgress);

            // 检查是否有拆弹器
            boolean hasKit = player.getMainHandItem().is(Items.SHEARS) || player.getOffhandItem().is(Items.SHEARS);
            int totalDefuseTime = hasKit ? DEFUSE_TIME_WITH_KIT_TICKS : DEFUSE_TIME_TICKS;

            // 检查拆弹是否完成
            if (currentProgress >= totalDefuseTime) {
                defuseC4(player);
            } else {
                displayDefuseProgress(player, currentProgress, totalDefuseTime);
            }
        } else {
            // 如果玩家不再满足拆弹条件，则重置其进度
            if (defusingPlayers.remove(player.getUUID()) != null) {
                player.sendSystemMessage(Component.literal(""), true); // 发送空消息来清除Action Bar
            }
        }
    }


    // =================================================================================
    // SECTION: 辅助方法与Getters/Setters (Helpers & Getters/Setters)
    // =================================================================================

    // --- 比赛状态查询 ---
    public String getName() { return name; }
    public MatchState getState() { return state; }
    public RoundState getRoundState() { return this.roundState; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getPlayerCount() { return playerStats.size(); }
    public String getCtTeamName() { return ctTeamName; }
    public String getTTeamName() { return tTeamName; }
    public Map<UUID, PlayerStats> getPlayerStats() { return playerStats; }
    public long getCtCount() { return playerStats.values().stream().filter(s -> "CT".equals(s.getTeam())).count(); }
    public long getTCount() { return playerStats.values().stream().filter(s -> "T".equals(s.getTeam())).count(); }
    public MinecraftServer getServer() { return this.server; }
    public boolean isC4Planted() { return this.c4Planted; }
    public BlockPos getC4Pos() { return this.c4Pos; }
    public Set<UUID> getAlivePlayers() { return this.alivePlayers; }

    // --- 比赛设置修改 ---
    public void setBombsiteA(AABB area) { this.bombsiteA = area; }
    public void setBombsiteB(AABB area) { this.bombsiteB = area; }
    public void addCtSpawn(BlockPos pos) { this.ctSpawns.add(pos); }
    public void addTSpawn(BlockPos pos) { this.tSpawns.add(pos); }
    public void setTotalRounds(int rounds) { this.totalRounds = rounds; }
    public void setRoundTimeSeconds(int seconds) { this.roundTimeSeconds = seconds; }
    public void setCtShopPos(BlockPos pos) { this.ctShopPos = pos; }
    public void setTShopPos(BlockPos pos) { this.tShopPos = pos; }

    // --- 内部辅助方法 ---

    /**
     * 将一名玩家添加到比赛中。
     *
     * @param player 要添加的玩家。
     * @param team   玩家要加入的队伍 ("CT" 或 "T")。
     */
    public void addPlayer(ServerPlayer player, String team) {
        playerStats.put(player.getUUID(), new PlayerStats(team));
        reapplyScoreboardToPlayer(player);
        this.bossBar.addPlayer(player);
        setPlayerKnockbackResistance(player, 1000.0);
    }

    /**
     * 从比赛中移除一名玩家。
     *
     * @param player 要移除的玩家。
     */
    public void removePlayer(ServerPlayer player) {
        playerStats.remove(player.getUUID());
        this.bossBar.removePlayer(player);
        setPlayerKnockbackResistance(player, 0.0);
    }

    /**
     * 将当前比赛的设置转换为一个MatchPreset对象，用于保存。
     * @return 包含当前比赛设置的MatchPreset实例。
     */
    public MatchPreset toPreset() {
        return new MatchPreset(this.ctSpawns, this.tSpawns, this.ctShopPos, this.tShopPos, this.bombsiteA, this.bombsiteB, this.totalRounds, this.roundTimeSeconds);
    }

    /**
     * 从一个MatchPreset对象加载比赛设置。
     * @param preset 包含比赛设置的预设对象。
     */
    public void applyPreset(MatchPreset preset) {
        this.ctSpawns.clear();
        this.ctSpawns.addAll(preset.ctSpawns());
        this.tSpawns.clear();
        this.tSpawns.addAll(preset.tSpawns());
        this.ctShopPos = preset.ctShopPos();
        this.tShopPos = preset.tShopPos();
        this.bombsiteA = preset.bombsiteA();
        this.bombsiteB = preset.bombsiteB();
        this.totalRounds = preset.totalRounds();
        this.roundTimeSeconds = preset.roundTimeSeconds();
    }
    
    /**
     * 向比赛中的所有玩家广播一条消息。
     *
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
     * 向指定队伍的所有玩家广播一条消息。
     *
     * @param message 要广播的消息。
     * @param team    目标队伍 ("CT" 或 "T")。
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
     * 从T阵营中随机挑选一名存活玩家给予C4。
     */
    private void giveC4ToRandomT() {
        List<ServerPlayer> aliveTPlayers = alivePlayers.stream()
                .map(uuid -> server.getPlayerList().getPlayer(uuid))
                .filter(p -> p != null && "T".equals(playerStats.get(p.getUUID()).getTeam()))
                .collect(Collectors.toList());

        if (!aliveTPlayers.isEmpty()) {
            ServerPlayer playerWithC4 = aliveTPlayers.get(new Random().nextInt(aliveTPlayers.size()));
            playerWithC4.getInventory().add(new ItemStack(QisCSGO.C4_ITEM.get()));
            playerWithC4.sendSystemMessage(Component.literal("§e你携带了C4炸弹！").withStyle(ChatFormatting.BOLD));
        }
    }

    /**
     * 在购买阶段开始时，为双方队伍生成商店村民。
     */
    private void spawnShops() {
        removeShops(); // 先移除旧的商店
        int duration = ServerConfig.buyPhaseSeconds * 20;
        Random random = new Random();

        // 生成CT商店
        if (ctShopPos != null) {
            int ctShopsToSpawn = Math.max(1, (int) Math.ceil(getCtCount() / 2.0));
            for (int i = 0; i < ctShopsToSpawn; i++) {
                double offsetX = random.nextDouble() * 2 - 1; // 在[-1, 1]范围内随机偏移
                double offsetZ = random.nextDouble() * 2 - 1;
                String command = String.format(Locale.US, "summon villager %.2f %.2f %.2f %s",
                        ctShopPos.getX() + 0.5 + offsetX, (double) ctShopPos.getY(), ctShopPos.getZ() + 0.5 + offsetZ,
                        ShopManager.getCtVillagerNbt(duration));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        }

        // 生成T商店
        if (tShopPos != null) {
            int tShopsToSpawn = Math.max(1, (int) Math.ceil(getTCount() / 2.0));
            for (int i = 0; i < tShopsToSpawn; i++) {
                double offsetX = random.nextDouble() * 2 - 1;
                double offsetZ = random.nextDouble() * 2 - 1;
                String command = String.format(Locale.US, "summon villager %.2f %.2f %.2f %s",
                        tShopPos.getX() + 0.5 + offsetX, (double) tShopPos.getY(), tShopPos.getZ() + 0.5 + offsetZ,
                        ShopManager.getTVillagerNbt(duration));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        }
    }

    /**
     * 移除所有商店村民。
     */
    private void removeShops() {
        // 使用一个较大的范围来确保所有分散的商店都被移除
        if (ctShopPos != null)
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..5,x=" + ctShopPos.getX() + ",y=" + ctShopPos.getY() + ",z=" + ctShopPos.getZ() + "]");
        if (tShopPos != null)
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=minecraft:villager,distance=..5,x=" + tShopPos.getX() + ",y=" + tShopPos.getY() + ",z=" + tShopPos.getZ() + "]");
    }

    /**
     * 在战斗阶段开始时，记录所有存活玩家的装备。
     */
    private void recordAllPlayerGear() {
        for (UUID playerUUID : alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            PlayerStats stats = playerStats.get(playerUUID);
            if (stats == null) continue;

            List<ItemStack> currentGear = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && !ItemNBTHelper.isSameBaseItem(stack, "minecraft:diamond") && !stack.is(QisCSGO.C4_ITEM.get())) {
                    currentGear.add(stack.copy());
                }
            }
            stats.setRoundGear(currentGear);
        }
    }

    /**
     * 在每回合开始时为玩家发放金钱。
     */
    private void distributeRoundIncome() {
        boolean isPistolRound = (currentRound == 1 || currentRound == (totalRounds / 2) + 1);

        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PlayerStats stats = entry.getValue();
            if (player == null) continue;

            if (isPistolRound) {
                // 手枪局，清空钻石并发放固定起始资金
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "clear " + player.getName().getString() + " minecraft:diamond");
                EconomyManager.giveMoney(player, ServerConfig.pistolRoundStartingMoney);
            } else {
                // 根据上回合胜负发放奖励
                boolean wasWinner = stats.getTeam().equals(this.lastRoundWinner);
                int income;
                if (wasWinner) {
                    income = ServerConfig.winReward;
                } else {
                    int lossBonus = Math.min(stats.getConsecutiveLosses() * ServerConfig.lossStreakBonus, ServerConfig.maxLossStreakBonus);
                    income = ServerConfig.lossReward + lossBonus;
                }
                EconomyManager.giveMoney(player, income);
            }
        }
    }

    /**
     * 选择性地清空玩家背包，保留受保护的物品（如货币、护甲）。
     */
    private void performSelectiveClear(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                    .anyMatch(id -> ItemNBTHelper.isSameBaseItem(stack, id));

            if (!isProtected) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 在手枪局为玩家发放初始装备。
     */
    private void giveInitialGear(ServerPlayer player, String team) {
        List<String> gearList = "CT".equals(team) ? ServerConfig.ctPistolRoundGear : ServerConfig.tPistolRoundGear;
        for (String itemCommandString : gearList) {
            String command = "give " + player.getName().getString() + " " + itemCommandString;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        }
    }

    /**
     * 在半场时交换双方队伍，并重置玩家状态。
     */
    private void swapTeams() {
        broadcastToAllPlayersInMatch(Component.literal("半场换边！队伍已交换。").withStyle(ChatFormatting.YELLOW));
        // 交换比分
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
                // 更新玩家在游戏内的队伍
                String newTeamName = "CT".equals(newTeam) ? getCtTeamName() : getTTeamName();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team leave " + player.getName().getString());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "team join " + newTeamName + " " + player.getName().getString());
                
                // 清理背包并发放提示
                performSelectiveClear(player);
                player.sendSystemMessage(Component.literal("你现在是 " + ("CT".equals(newTeam) ? "反恐精英 (CT)" : "恐怖分子 (T)") + " 队的一员！").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /**
     * 传送所有玩家到他们队伍的随机出生点。
     */
    private void teleportPlayersToSpawns() {
        Random random = new Random();
        for (UUID playerUUID : alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;
            PlayerStats stats = playerStats.get(playerUUID);
            if (stats == null) continue;

            List<BlockPos> spawns = "CT".equals(stats.getTeam()) ? ctSpawns : tSpawns;
            if (!spawns.isEmpty()) {
                BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
                player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        }
    }

    /**
     * 在购买阶段检查玩家是否超出了购买区域。
     */
    private void checkPlayerBuyZone() {
        Random random = new Random();
        for (UUID playerUUID : alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            PlayerStats stats = playerStats.get(playerUUID);
            String team = stats.getTeam();
            BlockPos shopPos = "CT".equals(team) ? ctShopPos : tShopPos;
            List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;

            if (shopPos != null && !spawns.isEmpty()) {
                double distance = Math.sqrt(player.distanceToSqr(shopPos.getX() + 0.5, player.getY(), shopPos.getZ() + 0.5));
                if (distance > BUY_ZONE_MAX_DISTANCE) {
                    BlockPos spawnPos = spawns.get(random.nextInt(spawns.size()));
                    player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal("购买阶段请不要离开购买区域！").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    /**
     * 每tick检查C4是否掉落，并向T阵营广播其位置和距离。
     */
    private void tickC4DropLocation() {
        ItemEntity droppedC4 = findDroppedC4();
        if (droppedC4 != null) {
            // 定时向全队广播坐标
            if (c4BroadcastCooldown <= 0) {
                c4BroadcastCooldown = C4_BROADCAST_COOLDOWN_TICKS;
                BlockPos c4DropPos = droppedC4.blockPosition();
                Component message = Component.literal("C4掉落在: " + c4DropPos.getX() + ", " + c4DropPos.getY() + ", " + c4DropPos.getZ()).withStyle(ChatFormatting.YELLOW);
                broadcastToTeam(message, "T");
            }
            c4BroadcastCooldown--;

            // 持续向每个T显示距离
            for (UUID playerUUID : alivePlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null && "T".equals(playerStats.get(playerUUID).getTeam())) {
                    double distance = player.distanceTo(droppedC4);
                    String distanceString = String.format("%.1f", distance);
                    Component distanceMessage = Component.literal("距离C4: " + distanceString + "米").withStyle(ChatFormatting.YELLOW);
                    player.sendSystemMessage(distanceMessage, true);
                }
            }
        } else {
            c4BroadcastCooldown = 0;
        }
    }

    /**
     * 清理比赛区域内的所有掉落物品。
     */
    private void clearDroppedItems() {
        // 构建一个包含所有关键点的AABB
        List<BlockPos> allKeyPositions = new ArrayList<>();
        allKeyPositions.addAll(ctSpawns);
        allKeyPositions.addAll(tSpawns);
        if (bombsiteA != null) {
            allKeyPositions.add(BlockPos.containing(bombsiteA.getCenter()));
        }
        if (bombsiteB != null) {
            allKeyPositions.add(BlockPos.containing(bombsiteB.getCenter()));
        }

        if (allKeyPositions.isEmpty()) return;

        // [修复] Lambda 表达式中的变量必须是 final 或 effectively final
        // 创建一个临时的、可变的 AABB 用于构建
        AABB tempSearchBox = new AABB(allKeyPositions.get(0));
        for (BlockPos pos : allKeyPositions) {
            tempSearchBox = tempSearchBox.minmax(new AABB(pos));
        }
        if (bombsiteA != null) {
            tempSearchBox = tempSearchBox.minmax(bombsiteA);
        }
        if (bombsiteB != null) {
            tempSearchBox = tempSearchBox.minmax(bombsiteB);
        }
        
        // 将最终构建好的 AABB 赋值给一个 final 变量，以便在 lambda 中使用
        final AABB searchBox = tempSearchBox.inflate(100.0);

        // 获取并移除区域内的所有ItemEntity
        List<ItemEntity> itemsToRemove = server.overworld().getEntitiesOfClass(ItemEntity.class, searchBox, (e) -> true);
        itemsToRemove.forEach(ItemEntity::discard);
        QisCSGO.LOGGER.info("比赛 '{}': 清理了 {} 个掉落物品。", this.name, itemsToRemove.size());
    }

    /**
     * 为指定的观察者寻找并设置一个新的观战目标。
     * 优先级：存活队友 -> C4位置 -> 队伍出生点。
     */
    private void findAndSetSpectatorTarget(ServerPlayer spectator) {
        PlayerStats spectatorStats = getPlayerStats().get(spectator.getUUID());
        if (spectatorStats == null) return;
        String team = spectatorStats.getTeam();

        // 1. 寻找随机的存活队友
        List<ServerPlayer> aliveTeammates = alivePlayers.stream()
                .map(uuid -> server.getPlayerList().getPlayer(uuid))
                .filter(p -> p != null && team.equals(getPlayerStats().get(p.getUUID()).getTeam()))
                .collect(Collectors.toList());

        if (!aliveTeammates.isEmpty()) {
            spectator.setCamera(aliveTeammates.get(new Random().nextInt(aliveTeammates.size())));
            return;
        }

        // 2. 如果没有队友，则飞向C4位置上空
        if (isC4Planted() && c4Pos != null) {
            spectator.teleportTo(server.overworld(), c4Pos.getX() + 0.5, c4Pos.getY() + 10, c4Pos.getZ() + 0.5, spectator.getYRot(), 90);
            return;
        }

        // 3. 如果C4也未安放，则飞向随机出生点
        List<BlockPos> spawns = "CT".equals(team) ? ctSpawns : tSpawns;
        if (!spawns.isEmpty()) {
            BlockPos spawnPos = spawns.get(new Random().nextInt(spawns.size()));
            spectator.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, spectator.getYRot(), spectator.getXRot());
        }
    }

    /**
     * 每tick更新所有观察者的视角，确保他们附身在有效目标上。
     */
    private void updateSpectatorCameras() {
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && player.isSpectator()) {
                // 如果观察者没有附身在任何实体上（例如，他们手动脱离了），为他们重新寻找目标
                if (player.getCamera() == player) {
                    findAndSetSpectatorTarget(player);
                }
            }
        }
    }

    /**
     * 检查一个玩家是否满足所有开始拆弹的条件。
     */
    private boolean isPlayerEligibleToDefuse(ServerPlayer player) {
        if (!this.c4Planted) return false;
        PlayerStats stats = playerStats.get(player.getUUID());
        if (stats == null || !"CT".equals(stats.getTeam())) return false;
        if (!player.isCrouching()) return false;

        // 光线追踪检查玩家是否正对着C4
        BlockHitResult hitResult = player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(5)), // 检查5格距离
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        return hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(this.c4Pos);
    }

    /**
     * 在玩家的Action Bar上显示拆弹进度。
     */
    private void displayDefuseProgress(ServerPlayer player, int currentProgress, int totalProgress) {
        int barsFilled = (int) (((float) currentProgress / totalProgress) * 10);
        StringBuilder progressBar = new StringBuilder("§a[");
        progressBar.append("|".repeat(barsFilled));
        progressBar.append("§7-".repeat(10 - barsFilled));
        progressBar.append("§a]");
        Component message = Component.literal("拆除中... ").append(Component.literal(progressBar.toString()));
        player.sendSystemMessage(message, true);
    }

    /**
     * 执行C4的最终拆除逻辑。
     */
    private void defuseC4(ServerPlayer player) {
        if (c4Pos != null) {
            // 移除C4方块，这将触发 onRemove -> onC4Defused -> endRound
            server.overworld().removeBlock(c4Pos, false);
            broadcastToAllPlayersInMatch(Component.literal("§b" + player.getName().getString() + " §f已经拆除了炸弹！"));
        }
    }

    /**
     * 应用自定义的C4爆炸伤害。
     */
    private void applyCustomExplosionDamage() {
        if (c4Pos == null) return;
        DamageSource damageSource = server.overworld().damageSources().genericKill();

        for (UUID playerUUID : alivePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) continue;

            double distance = Math.sqrt(player.distanceToSqr(c4Pos.getX() + 0.5, c4Pos.getY() + 0.5, c4Pos.getZ() + 0.5));
            if (distance < C4_EXPLOSION_RADIUS) {
                float damageFalloff = (float) (1.0 - distance / C4_EXPLOSION_RADIUS);
                float damageToApply = C4_EXPLOSION_MAX_DAMAGE * damageFalloff;
                if (damageToApply > 0) {
                    player.hurt(damageSource, damageToApply);
                }
            }
        }
    }

    /**
     * 在新回合开始时重置C4相关的所有状态。
     */
    private void resetC4State() {
        if (c4Planted && c4Pos != null) {
            server.overworld().removeBlock(c4Pos, false);
        }
        defusingPlayers.clear();
        c4CountdownHandler.stop();
        this.c4Planted = false;
        this.c4Pos = null;
    }

    /**
     * 初始化比赛计分板。
     */
    private void setupScoreboard() {
        this.scoreboard = server.getScoreboard();
        String safeMatchName = name.replaceAll("[^a-zA-Z0-9_.-]", "");
        String objectiveName = "kda_" + safeMatchName.substring(0, Math.min(safeMatchName.length(), 12));

        // 如果已存在同名计分项，先移除
        Optional.ofNullable(this.scoreboard.getObjective(objectiveName)).ifPresent(this.scoreboard::removeObjective);

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

        // 定期完整重建计分板，以移除已离开的玩家
        if (++scoreboardRebuildCounter >= SCOREBOARD_REBUILD_INTERVAL_TICKS) {
            rebuildScoreboard();
            scoreboardRebuildCounter = 0;
            return;
        }

        // 仅更新在榜玩家的分数
        playerStats.forEach((uuid, stats) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                // 新 API: 直接获取或创建分数并设置
                // 第三个参数 true 表示如果分数不存在则创建
                this.scoreboard.getOrCreatePlayerScore(player, this.objective, true).set(stats.getKills());
            }
        });
    }

    /**
 * 完整重建计分板，清除所有旧条目并添加当前所有玩家。
 * 这是为了处理玩家离线后计分板条目残留的问题。
 */
private void rebuildScoreboard() {
    if (this.objective == null || this.scoreboard == null) return;

    // [修复] Scoreboard API 变更 - 1.21.1 版本
    // 获取所有当前计分项的分数条目
    Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(this.objective);

    // [修复] 遍历副本并移除每个分数条目
    // 在 1.21.1 中，PlayerScoreEntry.owner() 返回 String，但我们需要 ScoreHolder
    // 使用 ScoreHolder.forNameOnly() 来创建 ScoreHolder
    new ArrayList<>(scores).forEach(score -> {
        // 使用 ScoreHolder.forNameOnly 将字符串转换为 ScoreHolder
        ScoreHolder scoreHolder = ScoreHolder.forNameOnly(score.owner());
        scoreboard.resetSinglePlayerScore(scoreHolder, this.objective);
    });

    // 重新添加当前所有玩家的分数
    playerStats.forEach((uuid, stats) -> {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            this.scoreboard.getOrCreatePlayerScore(player, this.objective, true).set(stats.getKills());
        }
    });

    QisCSGO.LOGGER.debug("重建计分板: {}", this.objective.getName());
}

    /**
     * 在比赛结束时移除计分板。
     */
    private void removeScoreboard() {
        if (this.scoreboard != null && this.objective != null) {
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            Optional.ofNullable(this.scoreboard.getObjective(this.objective.getName()))
                    .ifPresent(this.scoreboard::removeObjective);
            this.objective = null;
        }
    }

    /**
     * 为指定玩家重新应用计分板，主要用于玩家重连。
     */
    public void reapplyScoreboardToPlayer(ServerPlayer player) {
        if (this.scoreboard != null && this.objective != null) {
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
        }
    }

    /**
     * 更新Boss栏的显示内容和进度。
     */
    private void updateBossBar() {
        float progress = 1.0f;
        String text = "比赛暂停";
        BossEvent.BossBarColor color = BossEvent.BossBarColor.PURPLE;

        switch (this.roundState) {
            case BUY_PHASE -> {
                progress = (float) this.tickCounter / (ServerConfig.buyPhaseSeconds * 20);
                text = "购买阶段剩余: " + (this.tickCounter / 20 + 1) + "s";
                color = BossEvent.BossBarColor.GREEN;
            }
            case IN_PROGRESS -> {
                if (this.c4Planted) {
                    progress = (float) this.tickCounter / C4_TIMER_TICKS;
                    text = "§c§lC4即将爆炸: " + (this.tickCounter / 20 + 1) + "s";
                    color = BossEvent.BossBarColor.RED;
                } else {
                    progress = (float) this.tickCounter / (this.roundTimeSeconds * 20);
                    text = "回合剩余时间: " + (this.tickCounter / 20 + 1) + "s";
                    color = BossEvent.BossBarColor.WHITE;
                }
            }
            case ROUND_END -> {
                text = "回合结束";
                color = BossEvent.BossBarColor.YELLOW;
            }
            // [修复] 统一使用箭头表达式
            case PAUSED -> {
                // 保持 PAUSED 状态的默认值
            }
            default -> {
                // 其他状态的默认值
            }
        }
        this.bossBar.setProgress(progress);
        this.bossBar.setName(Component.literal(text));
        this.bossBar.setColor(color);
    }
    
    /**
     * 获取比赛的Boss栏实例。
     */
    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }

    /**
     * 为一个指定的玩家设置击退抗性属性。
     */
    private void setPlayerKnockbackResistance(ServerPlayer player, double amount) {
        if (player != null) {
            String command = "attribute " + player.getName().getString() + " minecraft:generic.knockback_resistance base set " + amount;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        }
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
     * 根据击杀数和死亡数获取队伍的排名玩家列表。
     */
    private List<Map.Entry<UUID, PlayerStats>> getRankedPlayers(String team, int limit) {
        return playerStats.entrySet().stream()
                .filter(entry -> team.equals(entry.getValue().getTeam()))
                .sorted(Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().getKills()).reversed()
                        .thenComparingInt(e -> e.getValue().getDeaths()))
                .limit(limit)
                .toList();
    }
    
    /**
     * 比赛结束后，重置所有玩家状态并将其传送至世界出生点。
     */
    private void resetAndTeleportPlayers() {
        BlockPos spawnPos = server.overworld().getSharedSpawnPos();
        for (UUID playerUUID : playerStats.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.setGameMode(server.getDefaultGameType());
                setPlayerKnockbackResistance(player, 0.0);
                player.removeAllEffects();
                player.getInventory().clearContent();
                player.teleportTo(server.overworld(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            }
        }
    }
    
    /**
     * 在每回合开始时，向所有玩家广播当前的比分。
     */
    private void broadcastScoreUpdate() {
        String titleJson = String.format("[{\"text\":\"CT \",\"color\":\"blue\"},{\"text\":\"%d - %d\",\"color\":\"white\"},{\"text\":\" T\",\"color\":\"gold\"}]", ctScore, tScore);
        String chatJson = String.format("[{\"text\":\"%s：\",\"color\":\"yellow\"},{\"text\":\"CT \",\"color\":\"blue\"},{\"text\":\"%d:%d\",\"color\":\"white\",\"bold\":true},{\"text\":\" T\",\"color\":\"gold\"}]", this.name, ctScore, tScore);

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "title @a title " + titleJson);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tellraw @a " + chatJson);
    }

    /**
     * 检查一个玩家是否在任何一个包点内。
     */
    public boolean isPlayerInBombsite(ServerPlayer player) {
        return (bombsiteA != null && bombsiteA.contains(player.position())) || (bombsiteB != null && bombsiteB.contains(player.position()));
    }

    /**
     * 检查一个坐标是否在任何一个包点内。
     */
    public boolean isPosInBombsite(BlockPos pos) {
        return (bombsiteA != null && bombsiteA.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) || (bombsiteB != null && bombsiteB.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }
    
    /**
     * 在比赛区域内寻找掉落的C4实体。
     */
    private ItemEntity findDroppedC4() {
        if (ctSpawns.isEmpty() && tSpawns.isEmpty() && bombsiteA == null && bombsiteB == null) return null;

        // [修复] Lambda 表达式中的变量必须是 final 或 effectively final
        // 创建一个临时的、可变的 AABB 用于构建
        AABB tempSearchBox = new AABB(ctSpawns.isEmpty() ? (tSpawns.isEmpty() ? BlockPos.ZERO : tSpawns.get(0)) : ctSpawns.get(0));
        for (BlockPos pos : ctSpawns) {
            tempSearchBox = tempSearchBox.minmax(new AABB(pos));
        }
        for (BlockPos pos : tSpawns) {
            tempSearchBox = tempSearchBox.minmax(new AABB(pos));
        }
        if (bombsiteA != null) {
            tempSearchBox = tempSearchBox.minmax(bombsiteA);
        }
        if (bombsiteB != null) {
            tempSearchBox = tempSearchBox.minmax(bombsiteB);
        }

        // 将最终构建好的 AABB 赋值给一个 final 变量，以便在 lambda 中使用
        final AABB searchBox = tempSearchBox.inflate(100.0);

        List<ItemEntity> items = server.overworld().getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.getItem().is(QisCSGO.C4_ITEM.get()));
        return items.isEmpty() ? null : items.get(0);
    }
}