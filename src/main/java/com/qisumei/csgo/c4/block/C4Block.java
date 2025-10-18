package com.qisumei.csgo.c4.block;

import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.MatchManager;
import com.qisumei.csgo.game.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * C4方块类，继承自Block类
 * 用于表示游戏中的C4爆炸物方块，具有特定的形状和交互逻辑
 */
public class C4Block extends Block {
    private static final VoxelShape C4_SHAPE = Block.box(0, 0, 0, 15, 5, 12);

    // --- 新增：C4拆除逻辑相关常量和静态变量 ---
    /** 徒手拆除C4所需的时间（单位：ticks） */
    private static final int DEFUSE_TIME_HAND_TICKS = 6 * 20; // 6秒
    /** 使用剪刀拆除C4所需的时间（单位：ticks） */
    private static final int DEFUSE_TIME_SHEARS_TICKS = 3 * 20; // 3秒

    /** 用于跟踪每个玩家拆除进度的Map */
    private static final Map<UUID, Integer> defuseProgress = new HashMap<>();

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
            // 无论如何，当方块被移除时，清空所有人的拆除进度
            defuseProgress.clear();
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    /**
     * 获取玩家破坏该方块的进度速度。
     * [核心修正] 此方法现在处理C4的拆除逻辑，而不是简单的方块破坏。
     * @param state 方块状态对象
     * @param player 正在破坏方块的玩家对象
     * @param world 方块访问器接口
     * @param pos 方块位置坐标
     * @return 返回拆除进度速度，0.0f 表示无法拆除
     */
    @Override
    public float getDestroyProgress(@Nonnull BlockState state, @Nonnull Player player, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
        // 必须是服务端玩家
        if (!(player instanceof ServerPlayer sp)) {
            return 0.0f;
        }

        // 玩家必须处于下蹲状态
        if (!player.isCrouching()) {
            // 如果玩家停止下蹲，则重置其拆除进度并阻止拆除
            defuseProgress.remove(player.getUUID());
            return 0.0f;
        }

        // 检查玩家是否在比赛中且为 CT 阵营
        Match match = MatchManager.getPlayerMatch(sp);
        if (match == null) {
            return 0.0f;
        }
        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        if (stats == null || !"CT".equals(stats.getTeam())) {
            return 0.0f; // 只有 CT 才能拆除
        }

        // 根据是否手持剪刀确定总拆除时间
        boolean hasShears = player.getMainHandItem().is(Items.SHEARS);
        int totalDefuseTicks = hasShears ? DEFUSE_TIME_SHEARS_TICKS : DEFUSE_TIME_HAND_TICKS;

        // 更新并获取当前玩家的拆除进度
        int currentProgress = defuseProgress.getOrDefault(player.getUUID(), 0) + 1;
        defuseProgress.put(player.getUUID(), currentProgress);

        // 清理其他正在尝试拆除的玩家的进度
        defuseProgress.keySet().removeIf(uuid -> !uuid.equals(player.getUUID()));

        // 计算并发送进度条到玩家的快捷栏上方(Action Bar)
        int percentage = (int) (((float)currentProgress / totalDefuseTicks) * 100);
        sp.sendSystemMessage(Component.literal("拆除中... " + Math.min(percentage, 100) + "%"), true);

        // 当进度达到或超过总时间时，方块会被游戏引擎自动破坏
        if (currentProgress >= totalDefuseTicks) {
            // 拆除成功后，清空该玩家的进度记录
            defuseProgress.remove(player.getUUID());
        }

        // 返回每 tick 的进度增量。当累计增量达到 1.0f 时，方块会被破坏。
        return 1.0f / totalDefuseTicks;
    }
}