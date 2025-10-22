// 新建文件: src/main/java/com/qisumei/csgo/grenade/SmokeGrenadeItem.java
package com.qisumei.csgo.grenade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 烟雾弹物品类。
 * 继承自抽象投掷物物品基类。
 */
public class SmokeGrenadeItem extends AbstractGrenadeItem {

    /**
     * 构造函数，定义物品的基本属性。
     * @param properties 物品属性
     */
    public SmokeGrenadeItem(Properties properties) {
        // 调用父类构造函数
        super(properties);
    }

    /**
     * 创建一个烟雾弹实体。
     * 这个方法在玩家扔出物品时被父类 AbstractGrenadeItem 调用。
     * @param world 当前世界
     * @param player 投掷者
     * @return 一个新的 SmokeGrenadeEntity 实例
     */
    @Override
    protected AbstractGrenadeEntity createGrenadeEntity(Level world, Player player) {
        // 创建并返回一个烟雾弹实体
        return new SmokeGrenadeEntity(world, player);
    }
}