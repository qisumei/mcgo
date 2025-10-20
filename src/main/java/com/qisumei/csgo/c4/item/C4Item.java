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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * C4炸弹物品类，定义了玩家如何安放C4。
 * <p>
 * 这个类处理了玩家右键使用C4时的所有前置条件检查，以及长按完成安放的逻辑。
 * </p>
 *
 * @author Qisumei
 */
public class C4Item extends Item {
    /**
     * 安放C4所需的时间，单位为游戏刻 (ticks)。70 ticks = 3.5秒。
     */
    private static final int USE_DURATION = 70;

    /**
     * 构造函数，设置C4物品的基本属性。
     */
    public C4Item() {
        super(new Item.Properties().stacksTo(1));
    }

    /**
     * 定义物品使用时的动画。
     *
     * @return 返回 {@link UseAnim#BLOCK}，使玩家在安放时呈现格挡姿势的动画。
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /**
     * 定义物品使用（长按）的总时长。
     *
     * @return 安放C4所需的时间。
     */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity p_345831_) {
        return USE_DURATION;
    }

    /**
     * 当玩家开始右键使用C4时触发。
     * <p>
     * 此方法会进行一系列检查，以确定玩家是否满足安放C4的条件：
     * <ul>
     * <li>玩家是否在进行中的比赛中。</li>
     * <li>回合是否处于战斗阶段 (IN_PROGRESS)。</li>
     * <li>玩家是否属于T阵营。</li>
     * <li>玩家是否身处炸弹安放区。</li>
     * <li>C4是否尚未被安放。</li>
     * </ul>
     * 只有所有条件都满足时，才会开始安放过程（播放动画）。
     * </p>
     *
     * @return 如果检查通过，返回 {@code InteractionResultHolder.consume(stack)} 开始使用物品；否则返回 {@code InteractionResultHolder.fail(stack)} 中止操作。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 仅在服务端执行逻辑
        if (player instanceof ServerPlayer serverPlayer) {
            Match match = MatchManager.getPlayerMatch(serverPlayer);

            // 检查是否满足安放条件
            if (match == null || match.getState() != Match.MatchState.IN_PROGRESS || match.getRoundState() != Match.RoundState.IN_PROGRESS) {
                serverPlayer.sendSystemMessage(Component.literal("§c现在不是安放C4的时间！"), true);
                return InteractionResultHolder.fail(stack);
            }

            PlayerStats stats = match.getPlayerStats().get(serverPlayer.getUUID());
            if (stats == null || !"T".equals(stats.getTeam())) {
                serverPlayer.sendSystemMessage(Component.literal("§c只有恐怖分子(T)才能安放C4！"), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!match.isPlayerInBombsite(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("§c你必须在炸弹安放区才能安放C4！"), true);
                return InteractionResultHolder.fail(stack);
            }

            if (match.isC4Planted()) {
                serverPlayer.sendSystemMessage(Component.literal("§c炸弹已经被安放了！"), true);
                return InteractionResultHolder.fail(stack);
            }

            // 所有检查通过，开始安放
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // 客户端直接返回失败，因为所有逻辑都在服务端处理
        return InteractionResultHolder.fail(stack);
    }

    /**
     * 当玩家持续使用（长按）C4时，每tick被调用一次。
     * <p>
     * 当剩余使用时间 {@code remainingUseTicks} 减到1时，表示玩家已完成安放动作。
     * 此时，代码会进行最后一次检查，确保安放的目标方块仍在包点内，然后放置C4方块。
     * </p>
     *
     * @param remainingUseTicks 剩余使用时间（从 {@link #USE_DURATION} 开始递减）。
     */
    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // 仅在服务端且使用完成时执行
        if (world.isClientSide() || !(user instanceof ServerPlayer player) || remainingUseTicks > 1) {
            return;
        }

        Match match = MatchManager.getPlayerMatch(player);
        if (match == null) return;

        // 获取玩家准星指向的方块
        BlockHitResult hitResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // C4将被放置在玩家看着的方块的相邻位置
            BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());

            // 最终检查：确保放置点在包点内
            if (!match.isPosInBombsite(placePos)) {
                player.sendSystemMessage(Component.literal("§c安放位置不在包点内，取消安放！"), true);
                player.stopUsingItem(); // 停止安放
                return;
            }

            // 放置C4方块
            world.setBlock(placePos, QisCSGO.C4_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            match.onC4Planted(placePos);

            // 如果不是创造模式，则消耗C4物品
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.stopUsingItem();
        }
    }
}
