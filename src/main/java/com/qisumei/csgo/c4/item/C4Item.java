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

public class C4Item extends Item {
    private static final int USE_DURATION = 70; // 3.5秒

    public C4Item() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // --- 修正 #1: 检查 player 是否为 ServerPlayer ---
        if (player instanceof ServerPlayer sp) {
            Match match = MatchManager.getPlayerMatch(sp);
            if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
                sp.sendSystemMessage(Component.literal("§c你必须在比赛中才能安放C4！"));
                return InteractionResultHolder.fail(stack);
            }
            
            PlayerStats stats = match.getPlayerStats().get(sp.getUUID());
            if (stats == null || !"T".equals(stats.getTeam())) {
                sp.sendSystemMessage(Component.literal("§c只有恐怖分子(T)才能安放C4！"));
                return InteractionResultHolder.fail(stack);
            }

            // --- 修正 #2: 调用 isPlayerInBombsite 时传入 ServerPlayer ---
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
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
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

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION;
    }
}