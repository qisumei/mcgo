package com.qisumei.csgo.c4.block;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class C4Block extends Block {
    private static final VoxelShape C4_SHAPE = Block.box(0, 0, 0, 15, 5, 12);

    public C4Block(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return C4_SHAPE;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClientSide()) {
            Match match = MatchManager.getMatchFromC4Pos(pos);
            if (match != null) {
                match.onC4Defused();
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        // --- 修正: 从 player.level() 获取世界状态 ---
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return 0.0f;
        }

        Match match = MatchManager.getPlayerMatch(sp);
        if (match == null) {
            return 0.0f;
        }

        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        if (stats == null || !"CT".equals(stats.getTeam())) {
            return 0.0f;
        }

        float baseSpeed = super.getDestroyProgress(state, player, world, pos);

        if (player.getMainHandItem().is(Items.SHEARS)) {
            return baseSpeed * 2.0f;
        }

        return baseSpeed;
    }
}