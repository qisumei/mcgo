package com.qisumei.csgo.game.preset;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchPreset 类用于存储一场CSGO比赛的预设配置信息。
 * 包括反恐精英（CT）和恐怖分子（T）的出生点、商店位置、炸弹安放区范围、总回合数和每回合时间等。
 */
public class MatchPreset {

    /** 反恐精英（CT）的出生点列表 */
    public final List<BlockPos> ctSpawns;

    /** 恐怖分子（T）的出生点列表 */
    public final List<BlockPos> tSpawns;

    /** 反恐精英（CT）的商店位置 */
    public final BlockPos ctShopPos;

    /** 恐怖分子（T）的商店位置 */
    public final BlockPos tShopPos;

    /** A炸弹区的边界框 */
    public final AABB bombsiteA;

    /** B炸弹区的边界框 */
    public final AABB bombsiteB;

    /** 总回合数 */
    public final int totalRounds;

    /** 每个回合的时间（秒） */
    public final int roundTimeSeconds;

    /**
     * 构造一个 MatchPreset 实例。
     *
     * @param ctSpawns         反恐精英（CT）的出生点列表
     * @param tSpawns          恐怖分子（T）的出生点列表
     * @param ctShopPos        反恐精英（CT）的商店位置
     * @param tShopPos         恐怖分子（T）的商店位置
     * @param bombsiteA        A炸弹区的边界框
     * @param bombsiteB        B炸弹区的边界框
     * @param totalRounds      总回合数
     * @param roundTimeSeconds 每个回合的时间（秒）
     */
    public MatchPreset(List<BlockPos> ctSpawns, List<BlockPos> tSpawns, BlockPos ctShopPos, BlockPos tShopPos, AABB bombsiteA, AABB bombsiteB, int totalRounds, int roundTimeSeconds) {
        this.ctSpawns = ctSpawns;
        this.tSpawns = tSpawns;
        this.ctShopPos = ctShopPos;
        this.tShopPos = tShopPos;
        this.bombsiteA = bombsiteA;
        this.bombsiteB = bombsiteB;
        this.totalRounds = totalRounds;
        this.roundTimeSeconds = roundTimeSeconds;
    }

    /**
     * 将当前 MatchPreset 对象序列化为 NBT 标签。
     *
     * @return 序列化后的 CompoundTag 对象
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        // --- 使用手动方式写入 BlockPos 列表 ---
        ListTag ctSpawnsTag = new ListTag();
        for (BlockPos pos : ctSpawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            ctSpawnsTag.add(posTag);
        }
        tag.put("ctSpawns", ctSpawnsTag);

        ListTag tSpawnsTag = new ListTag();
        for (BlockPos pos : tSpawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            tSpawnsTag.add(posTag);
        }
        tag.put("tSpawns", tSpawnsTag);

        // --- 使用手动方式写入单个 BlockPos ---
        if (ctShopPos != null) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", ctShopPos.getX());
            posTag.putInt("y", ctShopPos.getY());
            posTag.putInt("z", ctShopPos.getZ());
            tag.put("ctShopPos", posTag);
        }
        if (tShopPos != null) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", tShopPos.getX());
            posTag.putInt("y", tShopPos.getY());
            posTag.putInt("z", tShopPos.getZ());
            tag.put("tShopPos", posTag);
        }

        // AABB 和其他数据保持不变
        if (bombsiteA != null) {
            tag.putDouble("bombsiteA_minX", bombsiteA.minX);
            tag.putDouble("bombsiteA_minY", bombsiteA.minY);
            tag.putDouble("bombsiteA_minZ", bombsiteA.minZ);
            tag.putDouble("bombsiteA_maxX", bombsiteA.maxX);
            tag.putDouble("bombsiteA_maxY", bombsiteA.maxY);
            tag.putDouble("bombsiteA_maxZ", bombsiteA.maxZ);
        }
        if (bombsiteB != null) {
            tag.putDouble("bombsiteB_minX", bombsiteB.minX);
            tag.putDouble("bombsiteB_minY", bombsiteB.minY);
            tag.putDouble("bombsiteB_minZ", bombsiteB.minZ);
            tag.putDouble("bombsiteB_maxX", bombsiteB.maxX);
            tag.putDouble("bombsiteB_maxY", bombsiteB.maxY);
            tag.putDouble("bombsiteB_maxZ", bombsiteB.maxZ);
        }

        tag.putInt("totalRounds", totalRounds);
        tag.putInt("roundTimeSeconds", roundTimeSeconds);
        return tag;
    }

    /**
     * 从 NBT 标签反序列化为 MatchPreset 对象。
     *
     * @param tag 包含 MatchPreset 数据的 CompoundTag
     * @return 反序列化后的 MatchPreset 实例
     */
    public static MatchPreset fromNbt(CompoundTag tag) {
        List<BlockPos> ctSpawns = new ArrayList<>();
        // --- 使用手动方式读取 BlockPos 列表 ---
        tag.getList("ctSpawns", CompoundTag.TAG_COMPOUND).forEach(t -> {
            CompoundTag posTag = (CompoundTag) t;
            ctSpawns.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
        });

        List<BlockPos> tSpawns = new ArrayList<>();
        tag.getList("tSpawns", CompoundTag.TAG_COMPOUND).forEach(t -> {
            CompoundTag posTag = (CompoundTag) t;
            tSpawns.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
        });

        // --- 使用手动方式读取单个 BlockPos ---
        BlockPos ctShop = null;
        if (tag.contains("ctShopPos")) {
            CompoundTag posTag = tag.getCompound("ctShopPos");
            ctShop = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
        }
        BlockPos tShop = null;
        if (tag.contains("tShopPos")) {
            CompoundTag posTag = tag.getCompound("tShopPos");
            tShop = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
        }

        AABB aabbA = null;
        if (tag.contains("bombsiteA_minX")) {
            aabbA = new AABB(tag.getDouble("bombsiteA_minX"), tag.getDouble("bombsiteA_minY"), tag.getDouble("bombsiteA_minZ"),
                             tag.getDouble("bombsiteA_maxX"), tag.getDouble("bombsiteA_maxY"), tag.getDouble("bombsiteA_maxZ"));
        }
        AABB aabbB = null;
        if (tag.contains("bombsiteB_minX")) {
             aabbB = new AABB(tag.getDouble("bombsiteB_minX"), tag.getDouble("bombsiteB_minY"), tag.getDouble("bombsiteB_minZ"),
                              tag.getDouble("bombsiteB_maxX"), tag.getDouble("bombsiteB_maxY"), tag.getDouble("bombsiteB_maxZ"));
        }

        int rounds = tag.getInt("totalRounds");
        int time = tag.getInt("roundTimeSeconds");

        return new MatchPreset(ctSpawns, tSpawns, ctShop, tShop, aabbA, aabbB, rounds, time);
    }
}
