package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.server.ServerCommandExecutor;
import com.qisumei.csgo.util.ItemNBTHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlayerService 的默认实现。
 * 封装与玩家物品/背包相关的通用逻辑。
 */
public final class MatchPlayerService implements PlayerService {
    private final ServerCommandExecutor commandExecutor;

    /**
     * 构造一个 MatchPlayerService 实例。
     * 
     * @param commandExecutor 服务器命令执行器（不能为 null）
     * @throws NullPointerException 如果 commandExecutor 为 null
     */
    public MatchPlayerService(ServerCommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "CommandExecutor cannot be null");
    }

    @Override
    public void performSelectiveClear(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
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

    @Override
    public void giveInitialGear(ServerPlayer player, String team) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(team, "Team cannot be null");
        
        List<String> gearList = "CT".equals(team) ? ServerConfig.ctPistolRoundGear : ServerConfig.tPistolRoundGear;
        for (String itemId : gearList) {
            String command = "give " + player.getName().getString() + " " + itemId;
            commandExecutor.executeForPlayer(player, command);
        }
    }

    @Override
    public List<ItemStack> capturePlayerGear(ServerPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        List<ItemStack> currentGear = new ArrayList<>();

        // 安全获取 C4 物品，失败则为 null
        net.minecraft.world.item.Item c4Item = null;
        try {
            c4Item = QisCSGO.C4_ITEM.get();
        } catch (Throwable t) {
            QisCSGO.LOGGER.error("在 MatchPlayerService 获取 C4_ITEM 时发生异常：", t);
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
