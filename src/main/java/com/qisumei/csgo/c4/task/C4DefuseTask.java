package com.qisumei.csgo.c4.task;

import com.qisumei.csgo.c4.C4Manager;
import com.qisumei.csgo.game.PlayerStats;
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
 * C4拆弹任务处理器
 * 封装了所有与C4拆除相关的逻辑。
 */
public class C4DefuseTask {

    private final C4Manager c4Manager;

    private static final int DEFUSE_TIME_TICKS = 6 * 20; // 空手拆弹6秒
    private static final int DEFUSE_TIME_WITH_KIT_TICKS = 3 * 20; // 使用拆弹器3秒
    private final Map<UUID, Integer> defusingPlayers = new HashMap<>();

    public C4DefuseTask(C4Manager c4Manager) {
        this.c4Manager = c4Manager;
    }

    /**
     * 在每个玩家tick中调用，处理拆弹逻辑。
     * @param player 当前tick的玩家
     */
    public void handlePlayerDefuseTick(ServerPlayer player) {
        // 仅当C4已安放且玩家是CT时才执行
        if (!c4Manager.isC4Planted() || !"CT".equals(getPlayerStats(player).getTeam())) {
            return;
        }

        if (isPlayerEligibleToDefuse(player)) {
            int currentProgress = defusingPlayers.getOrDefault(player.getUUID(), 0);
            currentProgress++;
            defusingPlayers.put(player.getUUID(), currentProgress);

            boolean hasKit = player.getMainHandItem().is(Items.SHEARS) || player.getOffhandItem().is(Items.SHEARS);
            int totalDefuseTime = hasKit ? DEFUSE_TIME_WITH_KIT_TICKS : DEFUSE_TIME_TICKS;

            if (currentProgress >= totalDefuseTime) {
                // 拆除成功，移除C4方块。这将触发 C4Block.onRemove() -> c4Manager.onC4Defused()
                c4Manager.getMatch().getServer().overworld().removeBlock(c4Manager.getC4Pos(), false);
            } else {
                displayDefuseProgress(player, currentProgress, totalDefuseTime);
            }
        } else {
            // 如果不满足条件，重置该玩家的进度
            if (defusingPlayers.containsKey(player.getUUID())) {
                defusingPlayers.remove(player.getUUID());
                player.sendSystemMessage(Component.literal(""), true); // 清空action bar
            }
        }
    }

    /**
     * 重置拆弹状态，用于新回合。
     */
    public void reset() {
        defusingPlayers.clear();
    }

    // --- 私有辅助方法 ---

    private boolean isPlayerEligibleToDefuse(ServerPlayer player) {
        if (!player.isCrouching()) {
            return false;
        }

        BlockHitResult hitResult = player.level().clip(new ClipContext(
            player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(5)),
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));

        return hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(c4Manager.getC4Pos());
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

    private PlayerStats getPlayerStats(ServerPlayer player) {
        return c4Manager.getMatch().getPlayerStats().get(player.getUUID());
    }
}