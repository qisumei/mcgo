package com.qisumei.csgo.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * MatchAreaManager 负责与比赛地图/区域相关的工具方法。
 * 它通过 Supplier 注入比赛内的点集合，降低对 Match 内部字段的直接依赖。
 */
public class MatchAreaManager {
    private final MatchContext context;
    private final Supplier<List<BlockPos>> ctSpawnsSupplier;
    private final Supplier<List<BlockPos>> tSpawnsSupplier;

    public MatchAreaManager(MatchContext context, Supplier<List<BlockPos>> ctSpawnsSupplier, Supplier<List<BlockPos>> tSpawnsSupplier) {
        this.context = context;
        this.ctSpawnsSupplier = ctSpawnsSupplier;
        this.tSpawnsSupplier = tSpawnsSupplier;
    }

    /**
     * 计算并返回一个包围了比赛所有关键点的大致区域。
     */
    public AABB getMatchAreaBoundingBox() {
        List<BlockPos> allPositions = new ArrayList<>();
        List<BlockPos> ctSpawns = ctSpawnsSupplier.get();
        List<BlockPos> tSpawns = tSpawnsSupplier.get();
        if (ctSpawns != null) allPositions.addAll(ctSpawns);
        if (tSpawns != null) allPositions.addAll(tSpawns);
        if (context.getBombsiteA() != null) {
            allPositions.add(BlockPos.containing(context.getBombsiteA().getCenter()));
        }
        if (context.getBombsiteB() != null) {
            allPositions.add(BlockPos.containing(context.getBombsiteB().getCenter()));
        }

        if (allPositions.isEmpty()) return null;

        AABB boundingBox = new AABB(allPositions.getFirst());
        for (BlockPos pos : allPositions) {
            boundingBox = boundingBox.minmax(new AABB(pos));
        }
        return boundingBox;
    }

    /**
     * 清理比赛区域内所有掉落的物品实体。
     */
    public void clearDroppedItems() {
        AABB matchArea = getMatchAreaBoundingBox();
        if (matchArea == null) return;

        // 扩大搜索范围确保覆盖整个战场
        AABB searchArea = matchArea.inflate(50.0, 20.0, 50.0);

        List<ItemEntity> itemsToRemove = context.getServer().overworld().getEntitiesOfClass(ItemEntity.class, searchArea, (e) -> true);
        for (ItemEntity it : itemsToRemove) it.discard();

        // 若需要，可在日志中记录数量（Match 会根据自身需要调用日志）
    }
}

