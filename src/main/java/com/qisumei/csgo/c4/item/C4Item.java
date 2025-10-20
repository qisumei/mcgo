package com.qisumei.csgo.c4.item;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * C4Item 类表示游戏中用于安放炸弹的物品。
 * 该类继承自 Minecraft 的 Item 类，并实现了与 C4 安放相关的逻辑。
 */
public class C4Item extends Item {
    private static final int USE_DURATION = 70; // 3.5秒

    /**
     * 构造函数，初始化 C4 物品属性。
     * 设置最大堆叠数量为 1。
     */
    public C4Item() {
        super(new Item.Properties().stacksTo(1));
    }

    /**
     * 获取使用动画类型。
     *
     * @param stack 当前物品堆栈
     * @return 使用动画类型（BLOCK）
     */
    @Override
    @javax.annotation.Nonnull
    public UseAnim getUseAnimation(@javax.annotation.Nonnull ItemStack stack) {
        return UseAnim.BLOCK;
    }

/**
     * 处理玩家右键使用 C4 的行为。
     * 包括检查玩家是否处于比赛状态、是否为恐怖分子阵营、是否在炸弹安放区等条件。
     *
     * @param world  当前世界对象
     * @param player 使用物品的玩家
     * @param hand   玩家使用的交互手（主手或副手）
     * @return 操作结果，包含使用的物品堆栈
     */
    @Override
    @javax.annotation.Nonnull
    public InteractionResultHolder<ItemStack> use(@javax.annotation.Nonnull Level world, @javax.annotation.Nonnull Player player,@javax.annotation.Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 检查使用者是否为服务端玩家
        if (player instanceof ServerPlayer sp) {
            Match match = MatchManager.getPlayerMatch(sp);

            // --- 核心修正: 增加对回合状态的检查 ---
            // 必须是比赛进行中，并且当前回合也必须是正在进行中（非购买、非结束阶段）
            if (match == null || match.getState() != Match.MatchState.IN_PROGRESS || match.getRoundState() != Match.RoundState.IN_PROGRESS) {
                // 如果回合已经结束，则发送提示信息并阻止安放。
                sp.sendSystemMessage(Component.literal("§c现在不是安放C4的时间！"));
                return InteractionResultHolder.fail(stack);
            }

            PlayerStats stats = match.getPlayerStats().get(sp.getUUID());
            if (stats == null || !"T".equals(stats.getTeam())) {
                sp.sendSystemMessage(Component.literal("§c只有恐怖分子(T)才能安放C4！"));
                return InteractionResultHolder.fail(stack);
            }

            if (!match.isPlayerInBombsite(sp)) {
                sp.sendSystemMessage(Component.literal("§c你必须在炸弹安放区才能安放C4！"));
                return InteractionResultHolder.fail(stack);
            }

            if(match.isC4Planted()){
                sp.sendSystemMessage(Component.literal("§c炸弹已经被安放了！"));
                return InteractionResultHolder.fail(stack);
            }
        }

        // 如果所有检查都通过，则开始使用物品（播放安放动画）。
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 在使用过程中每 tick 调用一次的方法。
     * 主要用于检测玩家是否完成安放动作，并执行实际的 C4 安放逻辑。
     *
     * @param world             当前世界对象
     * @param user              使用物品的实体（玩家）
     * @param stack             当前物品堆栈
     * @param remainingUseTicks 剩余使用时间（tick）
     */
    @Override
    public void onUseTick(@javax.annotation.Nonnull Level world,@javax.annotation.Nonnull LivingEntity user,@javax.annotation.Nonnull ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof ServerPlayer player) || world.isClientSide()) return;

        if (remainingUseTicks == 1) {
            Match match = MatchManager.getPlayerMatch(player);
            if (match == null) return;

            BlockHitResult hitResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());

                if(!match.isPosInBombsite(placePos)){
                    player.sendSystemMessage(Component.literal("§c安放位置不在包点内，取消安放！"));
                    return;
                }

                world.setBlock(placePos, QisCSGO.C4_BLOCK.get().defaultBlockState(), 11);

                match.onC4Planted(placePos);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.stopUsingItem();
            }
        }
    }

    /**
     * 获取使用持续时间。
     *
     * @param stack 当前物品堆栈
     * @param user  使用物品的实体
     * @return 使用持续时间（单位：tick）
     */
    @Override
    public int getUseDuration(@javax.annotation.Nonnull ItemStack stack,@javax.annotation.Nonnull LivingEntity user) {
        return USE_DURATION;
    }
}
