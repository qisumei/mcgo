package com.qisumei.csgo.weapon;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * 武器工厂类 - 负责根据武器定义创建实际的 Minecraft 物品
 * 这个类封装了 PointBlank 武器的创建逻辑
 */
public class WeaponFactory {

    /**
     * 根据武器定义创建武器物品
     * 
     * @param definition 武器定义
     * @return 创建的武器物品，如果失败返回空物品
     */
    public static ItemStack createWeapon(WeaponDefinition definition) {
        try {
            // 创建基础武器物品
            ItemStack weapon = createBaseItem(definition.getWeaponId());
            if (weapon.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // 附加默认附件
            weapon = attachDefaultAttachments(weapon, definition);

            return weapon;
        } catch (Exception e) {
            QisCSGO.LOGGER.error("创建武器失败: {}", definition.getWeaponId(), e);
            return ItemStack.EMPTY;
        }
    }

    /**
     * 创建基础物品
     */
    private static ItemStack createBaseItem(String itemId) {
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

    /**
     * 为武器附加默认附件
     */
    private static ItemStack attachDefaultAttachments(ItemStack weapon, WeaponDefinition definition) {
        if (definition.getDefaultAttachments().isEmpty()) {
            return weapon;
        }

        try {
            // 获取或创建自定义数据
            CustomData customData = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            
            // 获取或创建附件标签
            CompoundTag attachmentsTag = tag.getCompound("pointblank:attachments");
            
            // 添加所有默认附件
            for (WeaponAttachment attachment : definition.getDefaultAttachments()) {
                String attachmentType = attachment.getType().name().toLowerCase();
                attachmentsTag.putString(attachmentType, attachment.getAttachmentId());
            }
            
            // 将附件标签放回主标签
            tag.put("pointblank:attachments", attachmentsTag);
            
            // 更新武器的自定义数据
            weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            
            return weapon;
        } catch (Exception e) {
            QisCSGO.LOGGER.error("为武器附加附件失败: {}", definition.getWeaponId(), e);
            return weapon;
        }
    }

    /**
     * 创建弹药物品
     * 
     * @param ammoType 弹药类型
     * @param amount 数量
     * @return 创建的弹药物品
     */
    public static ItemStack createAmmo(AmmoType ammoType, int amount) {
        if (!ammoType.hasAmmo()) {
            return ItemStack.EMPTY;
        }

        try {
            ItemStack ammo = createBaseItem(ammoType.getItemId());
            if (!ammo.isEmpty()) {
                ammo.setCount(Math.min(amount, 64));
            }
            return ammo;
        } catch (Exception e) {
            QisCSGO.LOGGER.error("创建弹药失败: {}", ammoType.getItemId(), e);
            return ItemStack.EMPTY;
        }
    }
}
