package com.qisumei.csgo.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店物品类 - 封装在商店中显示的物品
 * 这个类使得在商店中添加新物品变得更加简单和一致
 */
public class ShopItem {
    private final String itemId;
    private final String displayName;
    private final int price;
    private final ItemStack displayStack;
    private final boolean purchasableOnce;  // 是否每回合只能购买一次

    protected ShopItem(String itemId, String displayName, int price, ItemStack displayStack, boolean purchasableOnce) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.price = price;
        this.displayStack = displayStack;
        this.purchasableOnce = purchasableOnce;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrice() {
        return price;
    }

    public ItemStack getDisplayStack() {
        return displayStack;
    }

    public boolean isPurchasableOnce() {
        return purchasableOnce;
    }

    /**
     * 创建实际要给玩家的物品
     * 子类可以重写这个方法来自定义创建逻辑
     */
    public ItemStack createItemForPlayer() {
        return WeaponFactory.createWeapon(
            WeaponRegistry.getWeapon(itemId)
                .orElseThrow(() -> new IllegalStateException("Weapon not found: " + itemId))
        );
    }

    /**
     * 创建用于在商店中显示的物品
     */
    public static ItemStack createDisplayItem(String itemId, String displayName, int price) {
        // 尝试创建真实物品
        ItemStack real = WeaponFactory.createWeapon(
            WeaponRegistry.getWeapon(itemId).orElse(null)
        );
        
        if (real.isEmpty()) {
            // 回退到纸张占位
            real = new ItemStack(Items.PAPER);
        }

        // 设置显示名称
        real.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + displayName));

        // 设置描述
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7价格: §a$" + price));
        lore.add(Component.literal("§8左键点击购买"));
        lore.add(Component.literal("§7ID: " + itemId).withStyle(ChatFormatting.DARK_GRAY));
        real.set(DataComponents.LORE, new ItemLore(lore));

        return real;
    }

    /**
     * 从武器定义创建商店物品
     */
    public static ShopItem fromWeaponDefinition(WeaponDefinition weapon) {
        ItemStack displayStack = createDisplayItem(
            weapon.getWeaponId(),
            weapon.getDisplayName(),
            weapon.getPrice()
        );

        boolean purchasableOnce = weapon.getType() == WeaponType.GRENADE;

        return new ShopItem(
            weapon.getWeaponId(),
            weapon.getDisplayName(),
            weapon.getPrice(),
            displayStack,
            purchasableOnce
        );
    }
}
