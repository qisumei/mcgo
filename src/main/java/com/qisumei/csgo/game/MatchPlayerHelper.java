package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.util.ItemNBTHelper;
import com.qisumei.csgo.server.ServerCommandExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchPlayerHelper：提取并封装与玩家物品/背包相关的通用逻辑，降低 Match 的体积。
 *
 * @deprecated 请改用可注入的 {@link PlayerService}（默认实现 {@link MatchPlayerService}），
 *             该工具类保留以兼容旧调用，但新代码应注入 PlayerService 以便于单元测试和替换实现。
 */
@Deprecated
public final class MatchPlayerHelper {

    private MatchPlayerHelper() {}

    /**
     * 选择性地清空玩家背包，保留配置中受保护的物品。
     */
    public static void performSelectiveClear(ServerPlayer player) {
        if (player == null) return;
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
     * 在手枪局为玩家发放初始装备（通过命令执行器发放）。
     */
    public static void giveInitialGear(ServerPlayer player, String team, ServerCommandExecutor commandExecutor) {
        if (player == null || team == null || commandExecutor == null) return;
        List<String> gearList = "CT".equals(team) ? ServerConfig.ctPistolRoundGear : ServerConfig.tPistolRoundGear;
        for (String itemId : gearList) {
            String command = "give " + player.getName().getString() + " " + itemId;
            commandExecutor.executeForPlayer(player, command);
        }
    }

    /**
     * 记录单个玩家当前可保存的装备（用于胜利保留装备逻辑）。
     * 返回可保存的 ItemStack 列表（调用方负责复制/保存到 PlayerStats）。
     */
    public static List<ItemStack> capturePlayerGear(ServerPlayer player) {
        List<ItemStack> currentGear = new ArrayList<>();
        if (player == null) return currentGear;

        // 安全获取 C4 物品，失败则为 null
        net.minecraft.world.item.Item c4Item = null;
        try {
            c4Item = QisCSGO.C4_ITEM.get();
        } catch (Throwable t) {
            QisCSGO.LOGGER.error("在 MatchPlayerHelper 获取 C4_ITEM 时发生异常：", t);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            boolean isProtected = ServerConfig.inventoryProtectedItems.stream()
                    .anyMatch(id -> ItemNBTHelper.idMatches(stack, id));
            boolean isC4 = (c4Item != null) && stack.is(c4Item);

            if (!isProtected && !isC4) {
                currentGear.add(stack.copy());
            }
        }
        return currentGear;
    }
}
