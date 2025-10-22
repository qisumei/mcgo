package com.qisumei.csgo.game;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投掷物消耗追踪器 - 跟踪玩家在回合中使用的投掷物
 * 
 * 核心功能：
 * 1. 记录玩家投掷的投掷物
 * 2. 回合结束时保留未使用的投掷物
 * 3. 回合开始时清空已使用的投掷物
 */
public final class GrenadeConsumptionTracker {
    
    // 玩家投掷记录 <玩家UUID, 已使用的投掷物列表>
    private final Map<UUID, Set<UsedGrenade>> usedGrenades = new ConcurrentHashMap<>();
    
    /**
     * 已使用的投掷物记录
     */
    private static class UsedGrenade {
        private final String itemId;
        private final long timestamp;
        
        public UsedGrenade(String itemId) {
            this.itemId = itemId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsedGrenade that = (UsedGrenade) o;
            return Objects.equals(itemId, that.itemId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(itemId);
        }
    }
    
    /**
     * 记录玩家投掷了投掷物
     * @param player 投掷的玩家
     * @param grenade 投掷的投掷物
     */
    public void recordGrenadeThrow(ServerPlayer player, ItemStack grenade) {
        if (player == null || grenade == null || grenade.isEmpty()) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        String itemId = grenade.getDescriptionId();
        
        usedGrenades.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet())
                    .add(new UsedGrenade(itemId));
        
        QisCSGO.LOGGER.debug("玩家 {} 投掷了投掷物: {}", player.getName().getString(), itemId);
    }
    
    /**
     * 检查玩家是否已经使用了特定的投掷物
     * @param player 玩家
     * @param grenade 投掷物
     * @return true if already used, false otherwise
     */
    public boolean hasUsedGrenade(ServerPlayer player, ItemStack grenade) {
        if (player == null || grenade == null || grenade.isEmpty()) {
            return false;
        }
        
        Set<UsedGrenade> playerGrenades = usedGrenades.get(player.getUUID());
        if (playerGrenades == null || playerGrenades.isEmpty()) {
            return false;
        }
        
        String itemId = grenade.getDescriptionId();
        return playerGrenades.stream().anyMatch(ug -> ug.getItemId().equals(itemId));
    }
    
    /**
     * 获取玩家本回合使用的投掷物数量
     * @param player 玩家
     * @return 已使用的投掷物数量
     */
    public int getUsedGrenadeCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        Set<UsedGrenade> playerGrenades = usedGrenades.get(player.getUUID());
        return playerGrenades == null ? 0 : playerGrenades.size();
    }
    
    /**
     * 清空指定玩家的投掷物使用记录
     * @param player 玩家
     */
    public void clearPlayerGrenades(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        usedGrenades.remove(player.getUUID());
        QisCSGO.LOGGER.debug("已清空玩家 {} 的投掷物使用记录", player.getName().getString());
    }
    
    /**
     * 重置所有玩家的投掷物使用记录（回合结束时调用）
     */
    public void resetAllGrenades() {
        usedGrenades.clear();
        QisCSGO.LOGGER.debug("已重置所有玩家的投掷物使用记录");
    }
    
    /**
     * 清理玩家背包中已使用的投掷物（在回合开始时调用）
     * 保留未使用的投掷物
     * @param player 玩家
     */
    public void clearUsedGrenadesFromInventory(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        Set<UsedGrenade> playerUsedGrenades = usedGrenades.get(player.getUUID());
        if (playerUsedGrenades == null || playerUsedGrenades.isEmpty()) {
            return;
        }
        
        int clearedCount = 0;
        
        // 遍历玩家背包
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            
            // 检查是否为投掷物
            if (!PurchaseLimitManager.isGrenade(stack)) {
                continue;
            }
            
            // 检查是否已使用
            String itemId = stack.getDescriptionId();
            boolean wasUsed = playerUsedGrenades.stream()
                    .anyMatch(ug -> ug.getItemId().equals(itemId));
            
            if (wasUsed) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                clearedCount++;
            }
        }
        
        if (clearedCount > 0) {
            player.getInventory().setChanged();
            QisCSGO.LOGGER.debug("已清空玩家 {} 的 {} 个已使用投掷物", 
                player.getName().getString(), clearedCount);
        }
    }
    
    /**
     * 获取玩家本回合使用的投掷物详细信息
     * @param player 玩家
     * @return 投掷物使用详情列表
     */
    public List<String> getUsedGrenadeDetails(ServerPlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }
        
        Set<UsedGrenade> playerGrenades = usedGrenades.get(player.getUUID());
        if (playerGrenades == null || playerGrenades.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> details = new ArrayList<>();
        for (UsedGrenade grenade : playerGrenades) {
            details.add(String.format("%s (投掷于 %dms 前)", 
                grenade.getItemId(), 
                System.currentTimeMillis() - grenade.getTimestamp()));
        }
        
        return details;
    }
    
    /**
     * 检查玩家背包中是否有未使用的投掷物
     * @param player 玩家
     * @return true if has unused grenades, false otherwise
     */
    public boolean hasUnusedGrenades(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        Set<UsedGrenade> playerUsedGrenades = usedGrenades.get(player.getUUID());
        
        // 遍历玩家背包查找未使用的投掷物
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            
            // 检查是否为投掷物
            if (!PurchaseLimitManager.isGrenade(stack)) {
                continue;
            }
            
            // 如果没有使用记录，或者这个投掷物未被使用，则返回true
            if (playerUsedGrenades == null || playerUsedGrenades.isEmpty()) {
                return true;
            }
            
            String itemId = stack.getDescriptionId();
            boolean wasUsed = playerUsedGrenades.stream()
                    .anyMatch(ug -> ug.getItemId().equals(itemId));
            
            if (!wasUsed) {
                return true;
            }
        }
        
        return false;
    }
}
