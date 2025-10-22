package com.qisumei.csgo.grenade;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * 抽象投掷物物品基类
 * 负责处理所有投掷物共有的“投掷”行为。
 */
public abstract class AbstractGrenadeItem extends Item {

    public AbstractGrenadeItem(Properties properties) {
        super(properties.stacksTo(1)); // 投掷物通常可以堆叠
    }

    /**
     * 当玩家右键使用物品时调用。
     */
    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, Player player, @Nonnull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // 播放投掷音效
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide) {
            // 在服务器端生成投掷物实体
            AbstractGrenadeEntity grenadeEntity = createGrenadeEntity(world, player);
            grenadeEntity.setItem(itemStack);
            grenadeEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
            world.addFreshEntity(grenadeEntity);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1); // 消耗一个物品
        }

        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }

    /**
     * 抽象方法，由子类实现，用于创建具体的投掷物实体。
     * @param world 当前世界
     * @param player 投掷者
     * @return 一个具体的 AbstractGrenadeEntity 实例
     */
    protected abstract AbstractGrenadeEntity createGrenadeEntity(Level world, Player player);
}