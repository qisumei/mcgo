// 文件: src/main/java/com/qisumei/csgo/c4/C4Manager.java
package com.qisumei.csgo.c4;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.handler.C4CountdownHandler;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // C4掉落物广播计时器
    private int c4BroadcastCooldown = 0;

    // 拆弹相关
    private static final int DEFUSE_TIME_TICKS = 6 * 20; // 空手拆弹6秒
    private static final int DEFUSE_TIME_WITH_KIT_TICKS = 3 * 20; // 使用拆弹器3秒
    private final Map<UUID, Integer> defusingPlayers = new HashMap<>();

    public C4Manager(Match match) {
        this.match = match;
        this.countdownHandler = new C4CountdownHandler(this);
    }

    /**
     * 每tick调用，用于处理需要持续更新的逻辑。
     */
    public void tick() {
        // 更新C4安放后的倒计时
        countdownHandler.tick();
        // 更新C4掉落时的距离显示
        handleDroppedC4Tick();
    }

    /**
     * 【新增】处理C4掉落在地上时的坐标广播和距离显示。
     */
    private void handleDroppedC4Tick() {
        // 只有在C4未安放时才检查掉落物
        if (isPlanted) {
            return;
        }

        ItemEntity droppedC4 = findDroppedC4();
        if (droppedC4 != null) {
            // --- 坐标广播逻辑 ---
            if (c4BroadcastCooldown <= 0) {
                c4BroadcastCooldown = 20; // 每秒广播一次
                BlockPos c4DropPos = droppedC4.blockPosition();
                Component message = Component.literal("C4掉落在: " + c4DropPos.getX() + ", " + c4DropPos.getY() + ", " + c4DropPos.getZ()).withStyle(ChatFormatting.YELLOW);
                match.broadcastToTeam(message, "T");
            }
            c4BroadcastCooldown--;

            // --- 距离显示逻辑 ---
            for (UUID playerUUID : match.getAlivePlayers()) {
                ServerPlayer player = match.getServer().getPlayerList().getPlayer(playerUUID);
                if (player == null) continue;

                PlayerStats stats = match.getPlayerStats().get(playerUUID);
                if (stats != null && "T".equals(stats.getTeam())) {
                    double distance = player.distanceTo(droppedC4);
                    String distanceString = String.format("%.1f", distance);
                    Component distanceMessage = Component.literal("距离C4: " + distanceString + "米").withStyle(ChatFormatting.YELLOW);
                    player.sendSystemMessage(distanceMessage, true); // 发送到Action Bar
                }
            }
        } else {
            c4BroadcastCooldown = 0; // 重置计时器
        }
    }

    /**
     * 【新增】在比赛区域内寻找掉落的C4实体。
     * @return 如果找到，则返回ItemEntity对象；否则返回null。
     */
    private ItemEntity findDroppedC4() {
        AABB searchBox = match.getMatchAreaBoundingBox();
        if (searchBox == null) return null;

        List<ItemEntity> items = match.getServer().overworld().getEntitiesOfClass(ItemEntity.class, searchBox.inflate(50.0),
            item -> item.getItem().is(QisCSGO.C4_ITEM.get()));

        return items.isEmpty() ? null : items.get(0);
    }

    public void reset() {
        if (isPlanted && c4Pos != null) {
            match.getServer().overworld().removeBlock(c4Pos, false);
        }
        defusingPlayers.clear();
        countdownHandler.stop();
        this.isPlanted = false;
        this.c4Pos = null;
    }

    public void onC4Planted(BlockPos pos) {
        this.isPlanted = true;
        this.c4Pos = pos;
        countdownHandler.start(pos);

        String siteName = "包点"; // 默认
        if(match.getBombsiteA() != null && match.getBombsiteA().contains(pos.getCenter())){
            siteName = "A点";
        } else if(match.getBombsiteB() != null && match.getBombsiteB().contains(pos.getCenter())){
            siteName = "B点";
        }

        Component message = Component.literal("§e[信息] §f炸弹已安放在 §a" + siteName + "！");
        match.broadcastToAllPlayersInMatch(message);

        match.onC4PlantedUpdateMatchTimer();
    }

    public void onC4Defused(ServerPlayer defuser) {
        if (match.getRoundState() == Match.RoundState.IN_PROGRESS) {
            countdownHandler.stop();
            defusingPlayers.clear();
            match.broadcastToAllPlayersInMatch(Component.literal("§b" + (defuser != null ? defuser.getName().getString() : "CT") + " §f已经拆除了炸弹！"));
            match.endRound("CT", "炸弹已被拆除");
        }
    }

    public void onC4Exploded() {
        if (c4Pos != null) {
            match.applyCustomExplosionDamage(c4Pos);
        }
        match.endRound("T", "炸弹已爆炸");
    }

    public void handlePlayerDefuseTick(ServerPlayer player) {
        if (isPlayerEligibleToDefuse(player)) {
            int currentProgress = defusingPlayers.getOrDefault(player.getUUID(), 0);
            currentProgress++;
            defusingPlayers.put(player.getUUID(), currentProgress);

            boolean hasKit = player.getMainHandItem().is(Items.SHEARS) || player.getOffhandItem().is(Items.SHEARS);
            int totalDefuseTime = hasKit ? DEFUSE_TIME_WITH_KIT_TICKS : DEFUSE_TIME_TICKS;

            if (currentProgress >= totalDefuseTime) {
                match.getServer().overworld().removeBlock(c4Pos, false);
            } else {
                displayDefuseProgress(player, currentProgress, totalDefuseTime);
            }
        } else {
            if (defusingPlayers.containsKey(player.getUUID())) {
                defusingPlayers.remove(player.getUUID());
                player.sendSystemMessage(Component.literal(""), true); 
            }
        }
    }

    // --- Helper 和 Getter 方法 ---

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

    public boolean isC4Planted() {
        return this.isPlanted;
    }

    public BlockPos getC4Pos() {
        return this.c4Pos;
    }
    
    public Match getMatch() {
        return this.match;
    }
}