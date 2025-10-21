package com.qisumei.csgo.economy;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.EconomyManager;
import com.qisumei.csgo.service.ServiceFallbacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商店GUI - 使用箱子界面展示可购买的物品
 */
public class ShopGUI {

    // 武器 -> 弹药 映射（基于 pointblank 配置常见口径）
    private static final java.util.Map<String, String> AMMO_BY_WEAPON = new java.util.HashMap<>();
    static {
        AMMO_BY_WEAPON.put("pointblank:glock17", "pointblank:ammo9mm");
        AMMO_BY_WEAPON.put("pointblank:m9", "pointblank:ammo9mm");
        AMMO_BY_WEAPON.put("pointblank:deserteagle", "pointblank:ammo50ae");
        AMMO_BY_WEAPON.put("pointblank:mp7", "pointblank:ammo46");
        AMMO_BY_WEAPON.put("pointblank:ump45", "pointblank:ammo45acp");
        AMMO_BY_WEAPON.put("pointblank:p90", "pointblank:ammo57");
        AMMO_BY_WEAPON.put("pointblank:mp5", "pointblank:ammo9mm");
        AMMO_BY_WEAPON.put("pointblank:vector", "pointblank:ammo45acp");
        AMMO_BY_WEAPON.put("pointblank:ak47", "pointblank:ammo762");
        AMMO_BY_WEAPON.put("pointblank:m4a1", "pointblank:ammo556");
        AMMO_BY_WEAPON.put("pointblank:aug", "pointblank:ammo556");
        AMMO_BY_WEAPON.put("pointblank:a4_sg553", "pointblank:ammo556");
        AMMO_BY_WEAPON.put("pointblank:l96a1", "pointblank:ammo338lapua");
    }

    /**
     * 打开商店GUI给玩家
     * @param player 玩家
     * @param team 队伍（"CT" 或 "T"，用于显示对应队伍的物品）
     */
    public static void openShop(ServerPlayer player, String team) {
        MenuProvider menuProvider = new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new ShopMenu(containerId, playerInventory, (ServerPlayer) p, team),
            Component.literal("§6§l武器商店 - " + ("CT".equals(team) ? "反恐精英" : "恐怖分子"))
        );

        player.openMenu(menuProvider);
    }

    // 自定义Chest菜单以拦截点击进行购买
    private static class ShopMenu extends ChestMenu {
        private final ServerPlayer player;

        protected ShopMenu(int containerId, Inventory playerInventory, ServerPlayer player, String team) {
            super(MenuType.GENERIC_9x6, containerId, playerInventory, new ShopContainer(team, player), 6);
            this.player = player;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
            if (slotId >= 0 && slotId < 54) {
                ItemStack stack = this.getSlot(slotId).getItem();
                if (stack.isEmpty()) {
                    return;
                }

                if (slotId == 53 && stack.getItem() == Items.EMERALD) {
                    // refresh balance display
                    ItemStack emerald = buildMoneyDisplay(this.player);
                    this.getSlot(53).set(emerald);
                    this.broadcastChanges();
                    return;
                }

                var match = ServiceFallbacks.getPlayerMatch(this.player);
                if (match == null || match.getRoundState() != com.qisumei.csgo.game.Match.RoundState.BUY_PHASE) {
                    this.player.sendSystemMessage(Component.literal("§c只能在购买阶段购买！"));
                    return;
                }

                String itemId = parseItemIdFromLore(stack);
                Integer price = parsePriceFromLore(stack);

                if (itemId == null) {
                    itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                }
                if (price == null) {
                    this.player.sendSystemMessage(Component.literal("§c该物品无法购买（缺少价格信息）。"));
                    return;
                }

                if (!EconomyManager.takeMoney(this.player, price)) {
                    return;
                }

                // 为玩家生成实际物品
                ItemStack toGive = createItemFor(itemId);
                if (toGive.isEmpty()) {
                    this.player.sendSystemMessage(Component.literal("§c购买失败：找不到物品 " + itemId));
                    // 退款
                    EconomyManager.giveMoney(this.player, price);
                    return;
                }

                boolean added = this.player.getInventory().add(toGive.copy());
                if (!added) {
                    // 背包满了则丢在脚下
                    this.player.drop(toGive.copy(), false);
                }

                // 特例：购买铁胸甲视为“护甲+头盔”，同时赠送头盔
                if ("minecraft:iron_chestplate".equals(itemId)) {
                    ItemStack helm = new ItemStack(Items.IRON_HELMET);
                    if (!this.player.getInventory().add(helm)) {
                        this.player.drop(helm, false);
                    }
                }

                // 自动附赠一组对应口径的弹药（如果有映射）
                giveDefaultAmmoForWeapon(this.player, itemId);

                // 购买成功反馈与刷新余额显示
                this.player.sendSystemMessage(Component.literal("§a购买成功：").append(toGive.getHoverName()).append(Component.literal(" §7(-$" + price + ")")));
                // refresh balance slot
                ItemStack emerald = buildMoneyDisplay(this.player);
                this.getSlot(53).set(emerald);
                this.broadcastChanges();
                return;
            }

            super.clicked(slotId, button, clickType, clicker);
        }

        private static ItemStack buildMoneyDisplay(ServerPlayer player) {
            int balance = VirtualMoneyManager.getInstance().getMoney(player);
            ItemStack item = new ItemStack(Items.EMERALD);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l你的余额: §e$" + balance));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7左键点击刷新显示"));
            item.set(DataComponents.LORE, new ItemLore(lore));
            return item;
        }

        private static String parseItemIdFromLore(ItemStack stack) {
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) return null;
            for (Component line : lore.lines()) {
                String s = line.getString();
                int idx = s.indexOf("ID:");
                if (idx >= 0) {
                    String id = s.substring(idx + 3).trim();
                    // 去除颜色符号等
                    id = id.replace("§7", "").replace("§8", "").replace("§", "").trim();
                    return id;
                }
            }
            return null;
        }

        private static Integer parsePriceFromLore(ItemStack stack) {
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) return null;
            for (Component line : lore.lines()) {
                String s = line.getString();
                if (s.contains("价格") || s.contains("$")) {
                    // 尝试提取 $ 后的数字
                    int dollar = s.indexOf('$');
                    if (dollar >= 0) {
                        String num = s.substring(dollar + 1).replaceAll("[^0-9]", "");
                        if (!num.isEmpty()) {
                            try { return Integer.parseInt(num); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            return null;
        }

        private static ItemStack createItemFor(String itemId) {
            try {
                ResourceLocation id = ResourceLocation.tryParse(itemId);
                if (id == null) return ItemStack.EMPTY;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == null || item == Items.AIR) return ItemStack.EMPTY;
                return new ItemStack(item);
            } catch (Exception e) {
                QisCSGO.LOGGER.error("解析物品ID失败: {}", itemId, e);
                return ItemStack.EMPTY;
            }
        }

        private static void giveDefaultAmmoForWeapon(ServerPlayer player, String weaponId) {
            String ammoId = AMMO_BY_WEAPON.get(weaponId);
            if (ammoId == null) return;
            try {
                ResourceLocation rid = ResourceLocation.tryParse(ammoId);
                if (rid == null) return;
                Item ammoItem = BuiltInRegistries.ITEM.get(rid);
                if (ammoItem == null || ammoItem == Items.AIR) return;
                ItemStack ammoStack = new ItemStack(ammoItem);
                // 给一组 64 发（遵循 MC 栈上限；具体数量可在此调优）
                ammoStack.setCount(64);
                if (!player.getInventory().add(ammoStack)) {
                    player.drop(ammoStack, false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 创建商店物品展示
     */
    private static class ShopContainer implements net.minecraft.world.Container {
        private final ItemStack[] items = new ItemStack[54]; // 6行9列

        public ShopContainer(String team, ServerPlayer player) {
            // 初始化所有槽位为空
            for (int i = 0; i < items.length; i++) {
                items[i] = ItemStack.EMPTY;
            }

            // 填充商店物品
            setupShopItems(team);
            // 底部右侧显示余额
            updateMoneyDisplay(player);
        }

        private void setupShopItems(String team) {
            int slot = 0;

            // 第一行：手枪（使用 pointblank 实际存在的ID）
            addShopItem(slot++, "pointblank:glock17", "Glock-17", WeaponPrices.getPrice("pointblank:glock17"));
            addShopItem(slot++, "pointblank:m9", "M9", WeaponPrices.getPrice("pointblank:m9"));
            addShopItem(slot++, "pointblank:deserteagle", "沙漠之鹰", WeaponPrices.getPrice("pointblank:deserteagle"));
            slot = 9; // 跳到下一行

            // 第二行：冲锋枪（选择实际存在的：mp7/ump45/p90/mp5/vector）
            addShopItem(slot++, "pointblank:mp7", "MP7", WeaponPrices.getPrice("pointblank:mp7"));
            addShopItem(slot++, "pointblank:ump45", "UMP-45", WeaponPrices.getPrice("pointblank:ump45"));
            addShopItem(slot++, "pointblank:p90", "P90", WeaponPrices.getPrice("pointblank:p90"));
            addShopItem(slot++, "pointblank:mp5", "MP5", WeaponPrices.getPrice("pointblank:mp5"));
            addShopItem(slot++, "pointblank:vector", "Vector", WeaponPrices.getPrice("pointblank:vector"));
            slot = 18; // 跳到下一行

            // 第三行：步枪（使用 m4a1 / ak47 / aug / sg553(实际ID为 a4_sg553)）
            addShopItem(slot++, "pointblank:ak47", "AK-47", WeaponPrices.getPrice("pointblank:ak47"));
            addShopItem(slot++, "pointblank:m4a1", "M4A1", WeaponPrices.getPrice("pointblank:m4a1"));
            addShopItem(slot++, "pointblank:aug", "AUG", WeaponPrices.getPrice("pointblank:aug"));
            addShopItem(slot++, "pointblank:a4_sg553", "SG 553", WeaponPrices.getPrice("pointblank:a4_sg553"));
            slot = 27; // 跳到下一行

            // 第四行：狙击枪（pointblank 使用 l96a1 作为 AWP 类）
            addShopItem(slot++, "pointblank:l96a1", "L96A1", WeaponPrices.getPrice("pointblank:l96a1"));
            slot = 36; // 跳到下一行

            // 第五行：投掷物（pointblank 只有通用 grenade）
            addShopItem(slot++, "pointblank:grenade", "手雷", WeaponPrices.getPrice("pointblank:grenade"));
            slot = 45; // 跳到下一行

            // 第六行：护甲和工具（仅保留存在的物品；移除不存在的 defuse_kit）
            addShopItem(slot++, "minecraft:leather_chestplate", "护甲", WeaponPrices.getPrice("minecraft:leather_chestplate"));
            addShopItem(slot++, "minecraft:iron_chestplate", "护甲+头盔", WeaponPrices.getPrice("minecraft:iron_chestplate"));

            // 底部右侧余额展示在 updateMoneyDisplay 中处理
        }

        private void addShopItem(int slot, String itemId, String displayName, int price) {
            try {
                ItemStack item = createShopItem(itemId, displayName, price);
                items[slot] = item;
            } catch (Exception e) {
                QisCSGO.LOGGER.error("创建商店物品失败: " + itemId, e);
            }
        }

        private ItemStack createShopItem(String itemId, String displayName, int price) {
            // 优先尝试用真实物品作为展示
            ItemStack real = createRealItem(itemId);
            if (!real.isEmpty()) {
                // 附加价格和ID说明
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7价格: §a$" + price));
                lore.add(Component.literal("§8左键点击购买"));
                lore.add(Component.literal("§7ID: " + itemId).withStyle(ChatFormatting.DARK_GRAY));
                real.set(DataComponents.LORE, new ItemLore(lore));
                // 如果提供了自定义展示名称则覆盖
                if (displayName != null && !displayName.isEmpty()) {
                    real.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + displayName));
                }
                return real;
            }
            // 回退到纸张占位
            return createDisplayItem(itemId, displayName, price);
        }

        private ItemStack createRealItem(String itemId) {
            try {
                ResourceLocation id = ResourceLocation.tryParse(itemId);
                if (id == null) return ItemStack.EMPTY;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == null || item == Items.AIR) return ItemStack.EMPTY;
                return new ItemStack(item);
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }

        private ItemStack createDisplayItem(String itemId, String displayName, int price) {
            // 使用纸作为展示物品，带有自定义名称和描述
            ItemStack item = new ItemStack(Items.PAPER);

            // 设置显示名称
            item.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + displayName));

            // 设置描述
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7价格: §a$" + price));
            lore.add(Component.literal(""));
            lore.add(Component.literal("§e左键点击购买"));
            lore.add(Component.literal("§7ID: " + itemId).withStyle(ChatFormatting.DARK_GRAY));

            item.set(DataComponents.LORE, new ItemLore(lore));

            return item;
        }

        private ItemStack createMoneyDisplay(ServerPlayer player) {
            int balance = VirtualMoneyManager.getInstance().getMoney(player);
            ItemStack item = new ItemStack(Items.EMERALD);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l你的余额: §e$" + balance));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7左键点击刷新显示"));
            item.set(DataComponents.LORE, new ItemLore(lore));

            return item;
        }

        public void updateMoneyDisplay(ServerPlayer player) {
            items[53] = createMoneyDisplay(player);
        }

        @Override
        public int getContainerSize() {
            return 54;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack item : items) {
                if (!item.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < items.length ? items[slot] : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY; // 不允许移除
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            // 仅内部更新余额使用
            if (slot >= 0 && slot < items.length) {
                items[slot] = stack;
            }
        }

        @Override
        public void setChanged() {
            // 不需要标记更改
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            // 不允许清空
        }
    }
}

