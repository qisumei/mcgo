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

/**
 * C4方块类，继承自Block类
 * 用于表示游戏中的C4爆炸物方块，具有特定的形状和交互逻辑
 */
public class C4Block extends Block {
    private static final VoxelShape C4_SHAPE = Block.box(0, 0, 0, 15, 5, 12);

    /**
     * 构造函数，创建一个新的C4方块实例
     * @param props 方块属性对象，定义方块的基本属性如材质、硬度等
     */
    public C4Block(BlockBehaviour.Properties props) {
        super(props);
    }

    /**
     * 获取方块的碰撞形状
     * @param state 方块状态对象
     * @param world 方块访问器接口，提供对世界数据的访问
     * @param pos 方块位置坐标
     * @param context 碰撞上下文，包含碰撞检测的相关信息
     * @return 返回C4方块的体素形状
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return C4_SHAPE;
    }

    /**
     * 当方块被移除时的回调方法
     * 处理C4方块被拆除时的游戏逻辑，通知匹配管理器C4已被拆除
     * @param state 被移除前的方块状态
     * @param world 方块所在的世界对象
     * @param pos 方块的位置坐标
     * @param newState 移除后的新方块状态
     * @param moved 是否是由于移动导致的移除
     */
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

    /**
     * 获取玩家破坏该方块的进度速度
     * 只有CT队伍的玩家才能拆除C4，使用剪刀可以加快拆除速度
     * @param state 方块状态对象
     * @param player 正在破坏方块的玩家对象
     * @param world 方块访问器接口
     * @param pos 方块位置坐标
     * @return 返回破坏进度速度，0.0表示无法破坏
     */
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
