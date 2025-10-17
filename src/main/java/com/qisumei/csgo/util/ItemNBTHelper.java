package com.qisumei.csgo.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag; // <-- 确保导入了 Tag
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemNBTHelper {

    /**
     * 将一个 ItemStack (包括其NBT) 转换为可用于配置和命令的字符串.
     * @param stack 要转换的物品
     * @param registries 注册表访问器，用于序列化
     * @return 格式为 "minecraft:item_id{nbt...}" 的字符串
     */
    public static String itemStackToString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // --- 修正 #1: 安全地处理返回的 Tag 类型 ---
        Tag rawTag = stack.save(registries);
        if (!(rawTag instanceof CompoundTag tag)) {
            // 如果返回的不是CompoundTag (正常情况不会发生)，则只返回物品ID
            return id != null ? id.toString() : "";
        }
        
        // 移除一些不需要保存的基础数据
        tag.remove("id");
        tag.remove("Count");
        
        if (tag.isEmpty()) {
            return id.toString();
        }
        
        return id + tag.toString();
    }

    /**
     * 检查一个 ItemStack 的 ID 是否匹配一个可能带NBT的配置字符串 (忽略NBT).
     * @param stack 要检查的物品
     * @param configString 配置文件中的字符串, e.g., "minecraft:diamond_sword{...}"
     * @return 如果基础物品ID匹配则返回 true
     */
    public static boolean idMatches(ItemStack stack, String configString) {
        if (stack.isEmpty() || configString.isEmpty()) {
            return false;
        }

        try {
            String idFromString;
            int nbtStartIndex = configString.indexOf('{');
            if (nbtStartIndex != -1) {
                idFromString = configString.substring(0, nbtStartIndex);
            } else {
                idFromString = configString;
            }
            
            // --- 修正 #2: 使用 ResourceLocation.tryParse() 并检查null ---
            ResourceLocation configId = ResourceLocation.tryParse(idFromString);
            if (configId == null) {
                return false; // 如果配置中的ID格式错误，则直接返回false
            }

            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            return stackId.equals(configId);

        } catch (Exception e) {
            return false;
        }
    }
}