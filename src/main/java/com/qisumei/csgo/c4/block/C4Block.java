package com.qisumei.csgo.c4.block;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * C4炸弹方块类，代表已被安放在游戏世界中的C4。
 * <p>
 * 这个方块有两个核心特性：
 * 1. 它是坚不可摧的。玩家无法通过常规方式（如挖掘）来破坏它。
 * 2. 它的移除逻辑与拆弹成功事件绑定。当这个方块被代码移除时（在 {@link Match#defuseC4} 中），
 * 会触发 {@link #onRemove} 方法，从而调用 {@link Match#onC4Defused} 来宣布CT胜利。
 * </p>
 *
 * @author Qisumei
 */
public class C4Block extends Block {
    /**
     * C4方块的自定义碰撞箱形状，使其看起来更扁平。
     */
    private static final VoxelShape C4_SHAPE = Block.box(0, 0, 0, 15, 5, 12);

    public C4Block(BlockBehaviour.Properties props) {
        super(props);
    }

    /**
     * 获取方块的视觉和物理形状。
     *
     * @return 返回C4方块的自定义体素形状。
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return C4_SHAPE;
    }

    /**
     * 当此方块从世界中被移除时调用的回调方法。
     * <p>
     * 这是触发拆弹成功的关键。当CT玩家成功完成拆弹动作后，游戏逻辑会调用
     * {@code level.removeBlock()} 来移除这个C4方块，从而进入此方法，
     * 最终调用 {@link Match#onC4Defused()} 来结束回合。
     * </p>
     *
     * @param state     被移除前的方块状态。
     * @param world     方块所在的世界。
     * @param pos       方块的位置。
     * @param newState  将要替换当前方块的新状态（通常是空气）。
     * @param isMoving  方块是否因被活塞等移动而移除。
     */
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        // 确保这个方法只在方块真的被替换成其他东西时才触发
        if (!state.is(newState.getBlock()) && !world.isClientSide()) {
            // 根据C4的位置找到对应的比赛
            Match match = MatchManager.getMatchFromC4Pos(pos);
            if (match != null) {
                // 调用比赛的C4拆除成功逻辑
                match.onC4Defused();
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    /**
     * 获取玩家破坏此方块的进度。
     *
     * @return 总是返回 0.0f，使玩家无法通过长按左键来破坏此方块。
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return 0.0f;
    }
}
