package com.qisumei.csgo.economy;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.EconomyManager;
import com.qisumei.csgo.service.ServiceFallbacks;
import com.qisumei.csgo.weapon.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商店GUI - 使用箱子界面展示可购买的物品
 * 重构后使用 WeaponRegistry 系统管理武器
 */
public class ShopGUI {

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

                // 检查是否是投掷物，如果是则检查是否已购买过
                if (isThrowable(itemId)) {
                    var playerMatch = ServiceFallbacks.getPlayerMatch(this.player);
                    if (playerMatch != null) {
                        var stats = playerMatch.getPlayerStats().get(this.player.getUUID());
                        if (stats != null && stats.hasPurchasedThrowable(itemId)) {
                            this.player.sendSystemMessage(Component.literal("§c该投掷物每场比赛只能购买一次！"));
                            return;
                        }
                    }
                }

                if (!EconomyManager.takeMoney(this.player, price)) {
                    return;
                }

                // 使用武器注册表创建武器
                ItemStack toGive = createWeaponFromRegistry(itemId);
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

                // 自动附赠一组对应口径的弹药（使用武器定义）
                giveAmmoForWeapon(this.player, itemId);
                
                // 如果是投掷物，记录购买
                if (isThrowable(itemId)) {
                    var playerMatch = ServiceFallbacks.getPlayerMatch(this.player);
                    if (playerMatch != null) {
                        var stats = playerMatch.getPlayerStats().get(this.player.getUUID());
                        if (stats != null) {
                            stats.addPurchasedThrowable(itemId);
                        }
                    }
                }

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
        
        /**
         * 使用武器注册表创建武器
         */
        private static ItemStack createWeaponFromRegistry(String itemId) {
            // 尝试从武器注册表获取武器定义
            Optional<WeaponDefinition> weaponOpt = WeaponRegistry.getWeapon(itemId);
            if (weaponOpt.isPresent()) {
                return WeaponFactory.createWeapon(weaponOpt.get());
            }
            
            // 如果不在注册表中，回退到直接创建物品（用于护甲等非武器物品）
            return createItemFor(itemId);
        }
        
        /**
         * 为玩家提供武器对应的弹药
         */
        private static void giveAmmoForWeapon(ServerPlayer player, String weaponId) {
            Optional<WeaponDefinition> weaponOpt = WeaponRegistry.getWeapon(weaponId);
            if (weaponOpt.isEmpty()) {
                return;
            }
            
            WeaponDefinition weapon = weaponOpt.get();
            if (!weapon.getAmmoType().hasAmmo()) {
                return;
            }
            
            ItemStack ammo = WeaponFactory.createAmmo(
                weapon.getAmmoType(), 
                weapon.getDefaultAmmoAmount()
            );
            
            if (!ammo.isEmpty()) {
                if (!player.getInventory().add(ammo)) {
                    player.drop(ammo, false);
                }
            }
        }
        
        /**
         * 检查物品是否为投掷物
         */
        private static boolean isThrowable(String itemId) {
            Optional<WeaponDefinition> weaponOpt = WeaponRegistry.getWeapon(itemId);
            if (weaponOpt.isPresent()) {
                return weaponOpt.get().getType() == WeaponType.GRENADE;
            }
            // 回退到旧的检查方式
            return itemId != null && (
                itemId.contains("grenade") || 
                itemId.contains("flash") || 
                itemId.contains("smoke") ||
                itemId.equals("qiscsgo:smoke_grenade")
            );
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

            // 使用武器注册表自动填充商店
            // 第一行：手枪
            List<WeaponDefinition> pistols = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.PISTOL, team);
            for (WeaponDefinition weapon : pistols) {
                if (slot >= 9) break;
                addShopItemFromWeapon(slot++, weapon);
            }
            slot = 9; // 跳到下一行

            // 第二行：冲锋枪
            List<WeaponDefinition> smgs = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.SMG, team);
            for (WeaponDefinition weapon : smgs) {
                if (slot >= 18) break;
                addShopItemFromWeapon(slot++, weapon);
            }
            slot = 18; // 跳到下一行

            // 第三行：步枪
            List<WeaponDefinition> rifles = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.RIFLE, team);
            for (WeaponDefinition weapon : rifles) {
                if (slot >= 27) break;
                addShopItemFromWeapon(slot++, weapon);
            }
            slot = 27; // 跳到下一行

            // 第四行：狙击枪
            List<WeaponDefinition> snipers = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.SNIPER, team);
            for (WeaponDefinition weapon : snipers) {
                if (slot >= 36) break;
                addShopItemFromWeapon(slot++, weapon);
            }
            slot = 36; // 跳到下一行

            // 第五行：投掷物
            List<WeaponDefinition> grenades = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.GRENADE, team);
            for (WeaponDefinition weapon : grenades) {
                if (slot >= 45) break;
                addShopItemFromWeapon(slot++, weapon);
            }
            slot = 45; // 跳到下一行

            // 第六行：护甲和重型武器
            List<WeaponDefinition> heavyItems = WeaponRegistry.getWeaponsByTypeAndTeam(WeaponType.HEAVY, team);
            for (WeaponDefinition weapon : heavyItems) {
                if (slot >= 53) break;
                addShopItemFromWeapon(slot++, weapon);
            }

            // 底部右侧余额展示在 updateMoneyDisplay 中处理
        }

        /**
         * 从武器定义添加商店物品
         */
        private void addShopItemFromWeapon(int slot, WeaponDefinition weapon) {
            try {
                ShopItem shopItem = com.qisumei.csgo.weapon.ShopItem.fromWeaponDefinition(weapon);
                items[slot] = shopItem.getDisplayStack();
            } catch (Exception e) {
                QisCSGO.LOGGER.error("从武器定义创建商店物品失败: " + weapon.getWeaponId(), e);
            }
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

