package com.qisumei.csgo.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemNBTHelper {

    /**
     * 将一个 ItemStack (包括其NBT) 转换为可用于配置和命令的字符串。
     * @param stack 要转换的物品
     * @param registries 注册表访问器，用于序列化
     * @return 格式为 "minecraft:item_id[component=value,...]" 的字符串
     */
    public static String itemStackToString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        // 1. 获取物品的ID
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return "";
        }

        // 2. 将物品栈保存为完整的NBT标签
        Tag rawTag = stack.save(registries);
        if (!(rawTag instanceof CompoundTag tag)) {
            return id.toString();
        }

        // 3. 检查并获取 "components" 标签
        if (tag.contains("components", Tag.TAG_COMPOUND)) {
            CompoundTag components = tag.getCompound("components");

            if (components.isEmpty()) {
                return id.toString();
            }

            // 4. [核心修正] 手动构建组件字符串以匹配 `key=value` 格式
            StringBuilder componentsBuilder = new StringBuilder();
            boolean first = true;
            for (String key : components.getAllKeys()) {
                if (!first) {
                    componentsBuilder.append(',');
                }
                // 获取组件的值，并使用 getAsString() 转换为紧凑字符串
                Tag componentValue = components.get(key);
                String valueString = componentValue != null ? componentValue.getAsString() : "{}";

                // 拼接 "key=value" 格式，注意 key 是没有引号的
                componentsBuilder.append(key).append('=').append(valueString);
                first = false;
            }
            
            // 5. 按照 `item_id[components]` 格式进行最终拼接
            return id + "[" + componentsBuilder.toString() + "]";
        }

        // 如果物品没有任何组件，则只返回物品ID
        return id.toString();
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
