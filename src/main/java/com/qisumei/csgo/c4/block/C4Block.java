package com.qisumei.csgo.c4.block;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * C4方块类，继承自Block类。
 * 这个方块现在是坚不可摧的(-1.0f)，其拆除逻辑完全由外部的Match和GameEventsHandler处理。
 * 重构后：通过C4Manager来处理C4相关逻辑。
 */
public class C4Block extends Block {
    private static final VoxelShape C4_SHAPE = Block.box(0, 0, 0, 15, 5, 12);

    

    /**
     * 构造函数，创建一个新的C4方块实例。
     * @param props 方块属性对象，定义方块的基本属性如材质、硬度等。
     */
    public C4Block(BlockBehaviour.Properties props) {
        super(props);
    }

    /**
     * 获取方块的碰撞形状。
     * @return 返回C4方块的体素形状。
     */
    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return C4_SHAPE;
    }

    /**
     * 当方块被移除时的回调方法。
     * 这是拆弹成功的最终触发点。当C4Manager确认拆弹进度达到100%后，会移除这个方块，从而调用此方法。
     * 重构后：通过C4Manager来处理拆弹逻辑。
     * @param state 被移除前的方块状态。
     * @param world 方块所在的世界对象。
     * @param pos 方块的位置坐标。
     * @param newState 移除后的新方块状态。
     * @param moved 是否是由于移动导致的移除。
     */
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moved) {
        // 确保这个方法只在方块真的被替换成其他东西（比如空气）时才触发
        if (!state.is(newState.getBlock())) {
            // 在服务端执行
            if (!world.isClientSide()) {
                // 根据C4的位置找到对应的比赛
                Match match = MatchManager.getMatchFromC4Pos(pos);
                if (match != null) {
                    // 修改：通过C4Manager处理C4拆除逻辑
                    // 注意：这里我们无法直接获取拆弹玩家，因为C4Block的onRemove不包含玩家上下文
                    // C4Manager会在handlePlayerDefuseTick中记录拆弹玩家
                    match.getC4Manager().onC4Defused(null); // 传递null，让C4Manager自行处理
                }
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    /**
     * [核心修改] 重写此方法并返回0.0f，以防止玩家通过常规方式破坏方块。
     * 拆除逻辑现在完全由 C4Manager 和 GameEventsHandler 控制。
     */
    @Override
    public float getDestroyProgress(@Nonnull BlockState state, @Nonnull Player player, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
        return 0.0f;
    }
}