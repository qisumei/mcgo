package com.qisumei.csgo.c4.task;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.c4.C4Manager;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchContext;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * C4 Tick 任务处理器
 * 封装了所有需要在每个服务器 tick 或玩家 tick 中检查的逻辑。
 */
public class C4TickTask {

    private final C4Manager c4Manager;
    private final MatchContext context;
    private int c4BroadcastCooldown = 0;

    public C4TickTask(C4Manager c4Manager) {
        this.c4Manager = c4Manager;
        this.context = c4Manager.getContext();
    }

    /**
     * 在每个服务器 tick 中调用。
     */
    public void tick() {
        handleDroppedC4Tick();
    }

    // 安全获取 C4 物品实例，若未注册或异常则返回 null
    private Item getC4ItemOrNull() {
        try {
            return QisCSGO.C4_ITEM.get();
        } catch (Throwable t) {
            QisCSGO.LOGGER.error("在 C4TickTask 获取 C4_ITEM 时发生异常：", t);
            return null;
        }
    }

    /**
     * 在每个玩家 tick 中调用。
     * @param player 当前 tick 的玩家
     */
    public void handlePlayerTick(ServerPlayer player) {
        PlayerStats stats = context.getPlayerStats().get(player.getUUID());
        if (stats == null) return;

        if ("CT".equals(stats.getTeam())) {
            checkForIllegalC4Holder(player);
        } else if ("T".equals(stats.getTeam())) {
            handleC4PlantingHint(player);
        }
    }

    private void handleDroppedC4Tick() {
        if (c4Manager.isC4Planted()) return;

        ItemEntity droppedC4 = findDroppedC4();
        if (droppedC4 != null) {
            if (c4BroadcastCooldown <= 0) {
                c4BroadcastCooldown = 20;
                BlockPos c4DropPos = droppedC4.blockPosition();
                Component message = Component.literal("C4掉落在: " + c4DropPos.getX() + ", " + c4DropPos.getY() + ", " + c4DropPos.getZ()).withStyle(ChatFormatting.YELLOW);
                context.broadcastToTeam(message, "T");
            }
            c4BroadcastCooldown--;

            // 【修复】创建副本避免并发修改异常（玩家可能在迭代过程中死亡）
            for (UUID playerUUID : new ArrayList<>(context.getAlivePlayers())) {
                ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerUUID);
                if (player == null) continue;

                PlayerStats stats = context.getPlayerStats().get(playerUUID);
                if (stats != null && "T".equals(stats.getTeam())) {
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

    private ItemEntity findDroppedC4() {
        AABB searchBox = context.getMatchAreaBoundingBox();
        if (searchBox == null) return null;

        Item c4Item = getC4ItemOrNull();
        if (c4Item == null) return null;

        List<ItemEntity> items = context.getServer().overworld().getEntitiesOfClass(ItemEntity.class, searchBox.inflate(50.0),
            item -> item.getItem().is(c4Item));

        return items.isEmpty() ? null : items.get(0);
    }

    private void checkForIllegalC4Holder(ServerPlayer player) {
        Item c4Item = getC4ItemOrNull();
        if (c4Item == null) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(c4Item)) {
                ItemStack c4ToDrop = stack.copy();
                player.getInventory().setItem(i, ItemStack.EMPTY);
                player.drop(c4ToDrop, false, false);
                player.sendSystemMessage(Component.literal("§c作为CT，你不能持有C4！已强制丢弃。").withStyle(ChatFormatting.RED));
                QisCSGO.LOGGER.warn("已强制CT玩家 {} 丢弃C4。", player.getName().getString());
                break;
            }
        }
    }

    private void handleC4PlantingHint(ServerPlayer player) {
        Item c4Item = getC4ItemOrNull();
        if (c4Item == null) return;
        boolean holdingC4 = player.getMainHandItem().is(c4Item) || player.getOffhandItem().is(c4Item);
        if (holdingC4 && context.getRoundState() == Match.RoundState.IN_PROGRESS && !player.isUsingItem()) {
            if (context.isPlayerInBombsite(player)) {
                Component message = Component.literal("你正处于炸弹安放区，可以安放C4！").withStyle(ChatFormatting.GREEN);
                player.sendSystemMessage(message, true);
            }
        }
    }
}