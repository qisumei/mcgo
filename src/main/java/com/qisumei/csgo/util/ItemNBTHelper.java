package com.qisumei.csgo.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 一个工具类，提供将 ItemStack 转换为命令字符串以及比较基础物品ID的功能。
 * 主要用于处理配置文件和游戏内命令中物品的表示。
 *
 * @author Qisumei
 */
public final class ItemNBTHelper {

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private ItemNBTHelper() {}

    /**
     * 将一个 ItemStack 转换为其在游戏命令中的字符串表示形式。
     * <p>
     * 生成的格式为 {@code "minecraft:item_id[component_key=component_value,...]"}，
     * 这与现代Minecraft版本（1.20+）的 {@code /give} 命令格式兼容。
     *
     * @param stack 要转换的物品堆栈。
     * @param registries 用于序列化物品组件的注册表访问器。
     * @return 代表该物品的命令字符串；如果物品为空或无效，则返回空字符串。
     */
    public static String toCommandString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }

        // 1. 获取物品的注册名称 (ResourceLocation)
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return ""; // 如果物品未注册，则无法处理
        }

        // 2. 将物品堆栈的完整数据保存到一个NBT标签中
        Tag rawTag = stack.save(registries);
        if (!(rawTag instanceof CompoundTag tag)) {
            // 如果保存结果不是CompoundTag，说明没有组件信息，直接返回ID
            return id.toString();
        }

        // 3. 检查并提取 "components" 数据
        if (tag.contains("components", Tag.TAG_COMPOUND)) {
            CompoundTag components = tag.getCompound("components");

            if (components.isEmpty()) {
                // 如果没有组件，只返回物品ID
                return id.toString();
            }

            // 4. 手动构建组件字符串，以匹配 "key=value,key2=value2" 的格式
            StringBuilder componentsBuilder = new StringBuilder();
            boolean isFirst = true;
            for (String key : components.getAllKeys()) {
                if (!isFirst) {
                    componentsBuilder.append(',');
                }
                Tag componentValue = components.get(key);
                // 使用 getAsString() 将组件值转换为紧凑的SNBT字符串
                String valueString = componentValue != null ? componentValue.getAsString() : "{}";

                // 拼接 "key=value" 格式
                componentsBuilder.append(key).append('=').append(valueString);
                isFirst = false;
            }

            // 5. 按照 `item_id[components]` 格式进行最终拼接
            return id + "[" + componentsBuilder + "]";
        }

        // 如果物品没有任何组件，则只返回物品ID
        return id.toString();
    }

    /**
     * 检查一个 ItemStack 的基础物品ID是否与配置字符串中的ID匹配，忽略组件/NBT数据。
     * <p>
     * 例如，该方法会认为 "minecraft:diamond_sword" 和 "minecraft:diamond_sword[minecraft:unbreakable={}]" 是匹配的。
     *
     * @param stack 要检查的物品堆栈。
     * @param configString 配置文件或命令中的完整物品字符串。
     * @return 如果基础物品ID匹配，则返回 true；否则返回 false。
     */
    public static boolean isSameBaseItem(ItemStack stack, String configString) {
        if (stack.isEmpty() || configString == null || configString.isEmpty()) {
            return false;
        }

        try {
            // 1. 从配置字符串中提取物品ID部分
            String idFromString = configString;
            int componentStartIndex = configString.indexOf('[');
            if (componentStartIndex != -1) {
                // 处理现代组件格式 "minecraft:item[...]"
                idFromString = configString.substring(0, componentStartIndex);
            } else {
                // 处理旧版或无NBT格式 "minecraft:item{...}" 或 "minecraft:item"
                int nbtStartIndex = configString.indexOf('{');
                if (nbtStartIndex != -1) {
                    idFromString = configString.substring(0, nbtStartIndex);
                }
            }

            // 2. 解析字符串ID为 ResourceLocation
            ResourceLocation configId = ResourceLocation.tryParse(idFromString);
            if (configId == null) {
                // 如果配置中的ID格式错误，则认为不匹配
                return false;
            }

            // 3. 获取 ItemStack 的 ResourceLocation 并进行比较
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return configId.equals(stackId);

        } catch (Exception e) {
            // 捕获任何可能的字符串处理异常，确保程序稳定
            return false;
        }
    }
}
