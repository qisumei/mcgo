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

import javax.annotation.Nonnull;

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
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
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
    public void onRemove(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean moved) {
        if (!world.isClientSide()) {
            Match match = MatchManager.getMatchFromC4Pos(pos);
            if (match != null) {
                match.onC4Defused();
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    /**
     * 获取玩家破坏该方块的进度速度。
     * [核心修正] 移除了错误的 "instanceof Level" 检查，并整合了权限判断。
     * @param state 方块状态对象
     * @param player 正在破坏方块的玩家对象
     * @param world 方块访问器接口
     * @param pos 方块位置坐标
     * @return 返回破坏进度速度，0.0f 表示无法破坏
     */
    @Override
    public float getDestroyProgress(@Nonnull BlockState state, @Nonnull Player player, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
        // 首先，确保玩家是服务端玩家，否则不允许破坏
        if (!(player instanceof ServerPlayer sp)) {
            return 0.0f;
        }

        // 检查玩家是否在比赛中
        Match match = MatchManager.getPlayerMatch(sp);
        if (match == null) {
            return 0.0f;
        }

        // 检查玩家是否为 CT 阵营
        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        if (stats == null || !"CT".equals(stats.getTeam())) {
            return 0.0f; // 如果不是CT，则无法破坏
        }

        // --- 如果所有检查都通过，则计算破坏速度 ---

        // 获取基础破坏速度（取决于方块硬度和玩家状态）
        float baseSpeed = super.getDestroyProgress(state, player, world, pos);

        // 如果玩家手持剪刀（拆弹器），则速度加倍
        if (player.getMainHandItem().is(Items.SHEARS)) {
            return baseSpeed * 2.0f;
        }

        // 返回计算出的速度
        return baseSpeed;
    }
}
