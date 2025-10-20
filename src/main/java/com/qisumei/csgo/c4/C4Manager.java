// 新建文件: src/main/java/com/qisumei/csgo/c4/C4Manager.java

package com.qisumei.csgo.c4;

import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * C4管理器，封装所有与C4相关的逻辑，包括状态管理、安放、拆除和爆炸。
 * Match类通过此类来管理C4，实现功能解耦。
 */
public class C4Manager {

    private final Match match;
    private final C4CountdownHandler countdownHandler;

    // C4 状态
    private boolean isPlanted = false;
    private BlockPos c4Pos;

    // 拆弹相关
    private static final int DEFUSE_TIME_TICKS = 6 * 20; // 空手拆弹6秒
    private static final int DEFUSE_TIME_WITH_KIT_TICKS = 3 * 20; // 使用拆弹器3秒
    private final Map<UUID, Integer> defusingPlayers = new HashMap<>();

    /**
     * 构造函数，初始化C4管理器
     * @param match 对当前比赛的引用，用于回调比赛相关功能
     */
    public C4Manager(Match match) {
        this.match = match;
        // C4CountdownHandler现在由C4Manager拥有和管理
        this.countdownHandler = new C4CountdownHandler(this);
    }

    /**
     * 每tick调用，用于处理需要持续更新的逻辑
     * 主要职责：驱动倒计时处理器的tick逻辑
     */
    public void tick() {
        countdownHandler.tick();
    }

    /**
     * 重置C4状态，用于新回合开始
     * 清理所有C4相关状态，移除已安放的C4方块，停止倒计时
     */
    public void reset() {
        if (isPlanted && c4Pos != null) {
            match.getServer().overworld().removeBlock(c4Pos, false);
        }
        defusingPlayers.clear();
        countdownHandler.stop();
        this.isPlanted = false;
        this.c4Pos = null;
    }

    /**
     * 当C4被安放时调用，初始化C4安放状态
     * @param pos C4被安放的位置
     */
    public void onC4Planted(BlockPos pos) {
        this.isPlanted = true;
        this.c4Pos = pos;
        countdownHandler.start(pos);

        String siteName = "未知地点";
        if (match.isPosInBombsite(pos)) {
            // 这里可以添加更精确的包点判断逻辑
             siteName = "包点";
        }

        Component message = Component.literal("§e[信息] §f炸弹已安放在 §a" + siteName + "！");
        match.broadcastToAllPlayersInMatch(message);

        // 告知Match类回合时间需要改变
        match.onC4PlantedUpdateMatchTimer();
    }

    /**
     * 当C4被成功拆除时调用，处理拆弹成功逻辑
     * @param defuser 拆除C4的玩家，可为null表示未知玩家
     */
    public void onC4Defused(ServerPlayer defuser) {
        if (match.getRoundState() == Match.RoundState.IN_PROGRESS) {
            countdownHandler.stop();
            defusingPlayers.clear();
            match.broadcastToAllPlayersInMatch(Component.literal("§b" + (defuser != null ? defuser.getName().getString() : "CT") + " §f已经拆除了炸弹！"));
            match.endRound("CT", "炸弹已被拆除");
        }
    }

    /**
     * 当C4爆炸时调用，处理爆炸逻辑和回合结束
     */
    public void onC4Exploded() {
        if (c4Pos != null) {
            match.applyCustomExplosionDamage(c4Pos);
        }
        match.endRound("T", "炸弹已爆炸");
    }

    /**
     * 处理玩家每一tick的拆弹逻辑，包括进度跟踪和完成检测
     * @param player 正在被检测的玩家
     */
    public void handlePlayerDefuseTick(ServerPlayer player) {
        if (isPlayerEligibleToDefuse(player)) {
            int currentProgress = defusingPlayers.getOrDefault(player.getUUID(), 0);
            currentProgress++;
            defusingPlayers.put(player.getUUID(), currentProgress);

            boolean hasKit = player.getMainHandItem().is(Items.SHEARS) || player.getOffhandItem().is(Items.SHEARS);
            int totalDefuseTime = hasKit ? DEFUSE_TIME_WITH_KIT_TICKS : DEFUSE_TIME_TICKS;

            if (currentProgress >= totalDefuseTime) {
                // 拆除成功，移除C4方块，这将触发C4Block.onRemove -> onC4Defused
                match.getServer().overworld().removeBlock(c4Pos, false);
            } else {
                displayDefuseProgress(player, currentProgress, totalDefuseTime);
            }
        } else {
            if (defusingPlayers.containsKey(player.getUUID())) {
                defusingPlayers.remove(player.getUUID());
                player.sendSystemMessage(Component.literal(""), true); // 清空action bar
            }
        }
    }

    // --- Helper 和 Getter 方法 ---

    /**
     * 检查玩家是否符合拆弹条件
     * @param player 要检查的玩家
     * @return 如果玩家符合拆弹条件返回true，否则返回false
     */
    private boolean isPlayerEligibleToDefuse(ServerPlayer player) {
        if (!this.isPlanted) return false;
        
        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        if (stats == null || !"CT".equals(stats.getTeam())) return false;

        if (!player.isCrouching()) return false;

        BlockHitResult hitResult = player.level().clip(new ClipContext(
            player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(5)),
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));

        return hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(this.c4Pos);
    }

    /**
     * 在玩家action bar上显示拆弹进度条
     * @param player 要显示进度的玩家
     * @param current 当前进度值
     * @param total 总进度值
     */
    private void displayDefuseProgress(ServerPlayer player, int current, int total) {
        int percentage = (int) (((float) current / total) * 100);
        int barsFilled = (int) (((float) current / total) * 10);
        
        StringBuilder progressBar = new StringBuilder("§a[");
        for (int i = 0; i < 10; i++) {
            progressBar.append(i < barsFilled ? "|" : "§7-");
        }
        progressBar.append("§a] §f").append(percentage).append("%");

        Component message = Component.literal("拆除中... ").append(Component.literal(progressBar.toString()));
        player.sendSystemMessage(message, true);
    }

    /**
     * 获取C4是否已安放的状态
     * @return 如果C4已安放返回true，否则返回false
     */
    public boolean isC4Planted() {
        return this.isPlanted;
    }

    /**
     * 获取C4方块的位置
     * @return C4方块的位置，如果未安放返回null
     */
    public BlockPos getC4Pos() {
        return this.c4Pos;
    }
    
    /**
     * 获取关联的Match实例
     * @return 当前关联的Match实例
     */
    public Match getMatch() {
        return this.match;
    }
}