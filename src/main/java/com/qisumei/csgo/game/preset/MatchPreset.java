package com.qisumei.csgo.game.preset;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils; // 写入时仍然可以用，因为它很稳定
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class MatchPreset {

    public final List<BlockPos> ctSpawns;
    public final List<BlockPos> tSpawns;
    public final BlockPos ctShopPos;
    public final BlockPos tShopPos;
    public final AABB bombsiteA;
    public final AABB bombsiteB;
    public final int totalRounds;
    public final int roundTimeSeconds;

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