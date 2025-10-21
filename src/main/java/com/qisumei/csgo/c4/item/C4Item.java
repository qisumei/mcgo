// 文件: src/main/java/com/qisumei/csgo/c4/item/C4Item.java
package com.qisumei.csgo.c4.item;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;

public class C4Item extends Item {
    private static final int USE_DURATION = 70; // 3.5秒

    public C4Item() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            Match match = MatchManager.getPlayerMatch(sp);
            if (match == null || match.getState() != Match.MatchState.IN_PROGRESS || match.getRoundState() != Match.RoundState.IN_PROGRESS) {
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
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(@Nonnull Level world, @Nonnull LivingEntity user, @Nonnull ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof ServerPlayer player) || world.isClientSide()) return;

        // --- 1. 安放进度条显示逻辑 ---
        int totalUseDuration = getUseDuration(stack, user);
        int progressTicks = totalUseDuration - remainingUseTicks;
        int percentage = (int) (((float) progressTicks / totalUseDuration) * 100);
        int barsFilled = (int) (((float) progressTicks / totalUseDuration) * 10);
        StringBuilder progressBar = new StringBuilder("§a[");
        for (int i = 0; i < 10; i++) {
            progressBar.append(i < barsFilled ? "|" : "§7-");
        }
        progressBar.append("§a] §f").append(percentage).append("%");
        Component message = Component.literal("安放中... ").append(Component.literal(progressBar.toString()));
        player.sendSystemMessage(message, true);

        // --- 2. 安放完成时的智能选点逻辑 ---
        if (remainingUseTicks == 1) {
            Match match = MatchManager.getPlayerMatch(player);
            if (match == null) return;

            BlockHitResult hitResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos initialTargetPos = hitResult.getBlockPos().relative(hitResult.getDirection());
                
                //寻找最佳安放点
                BlockPos finalPlacePos = findValidPlantingSpot(world, initialTargetPos, match);

                if (finalPlacePos != null) {
                    // 找到了有效位置，安放C4
                    world.setBlock(finalPlacePos, QisCSGO.C4_BLOCK.get().defaultBlockState(), 11);
                    match.onC4Planted(finalPlacePos);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                } else {
                    // 搜索了一圈也没找到合适的位置
                    player.sendSystemMessage(Component.literal("§c无法在此处安放C4！请寻找平坦地面。"), false);
                }
            }
        }
    }
    
    /**
     * @param world 当前世界
     * @param initialPos 玩家期望的初始安放位置
     * @param match 当前比赛
     * @return 找到的可行位置，如果附近没有则返回null
     */
    private BlockPos findValidPlantingSpot(Level world, BlockPos initialPos, Match match) {
        // 1. 优先检查玩家的初始目标点
        if (isValidSpot(world, initialPos, match)) {
            return initialPos;
        }

        // 2. 如果初始点不行，则在其水平周围1格的范围内搜索 (3x3区域)
        for (BlockPos searchPos : BlockPos.betweenClosed(initialPos.offset(-1, 0, -1), initialPos.offset(1, 0, 1))) {
            // 在循环内部创建BlockPos的不可变副本进行检查
            if (isValidSpot(world, searchPos.immutable(), match)) {
                return searchPos.immutable(); // 找到第一个可行的位置就返回
            }
        }

        // 3. 如果搜索了一圈都没找到，返回null
        return null;
    }

    /**
     * 【新增】检查一个指定的位置是否是有效的C4安放点。
     * @param world 当前世界
     * @param pos 要检查的位置
     * @param match 当前比赛
     * @return 如果有效则返回true
     */
    private boolean isValidSpot(Level world, BlockPos pos, Match match) {
        // 条件1: 必须在包点范围内
        if (!match.isPosInBombsite(pos)) {
            return false;
        }

        BlockPos posBelow = pos.below();
        BlockState stateBelow = world.getBlockState(posBelow);
        BlockState stateAt = world.getBlockState(pos);

        // 条件2: 下方的方块必须是一个坚固的完整平面 (防止浮空)
        boolean isBottomSolid = stateBelow.isFaceSturdy(world, posBelow, Direction.UP);

        // 条件3: 当前位置本身必须是可被替换的方块 (如空气、草)
        boolean isReplaceable = stateAt.canBeReplaced();

        return isBottomSolid && isReplaceable;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity user) {
        return USE_DURATION;
    }
}