package com.qisumei.csgo.game.preset;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchPreset record用于存储一场CSGO比赛的预设配置信息。
 * 包括反恐精英（CT）和恐怖分子（T）的出生点、商店位置、炸弹安放区范围、总回合数和每回合时间等。
 * 
 * <p>使用Java 21的record特性提供：
 * <ul>
 *   <li>不可变数据结构，保证线程安全</li>
 *   <li>自动生成的equals、hashCode和toString方法</li>
 *   <li>简洁的语法和更好的语义表达</li>
 *   <li>编译器级别的final保证</li>
 * </ul>
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
public record MatchPreset(
    List<BlockPos> ctSpawns,
    List<BlockPos> tSpawns,
    BlockPos ctShopPos,
    BlockPos tShopPos,
    AABB bombsiteA,
    AABB bombsiteB,
    int totalRounds,
    int roundTimeSeconds
) {
    /**
     * 紧凑构造器，用于验证参数和创建防御性副本
     */
    public MatchPreset {
        // 创建不可变副本以防止外部修改
        ctSpawns = ctSpawns != null ? List.copyOf(ctSpawns) : List.of();
        tSpawns = tSpawns != null ? List.copyOf(tSpawns) : List.of();
        
        // 验证回合数和时间
        if (totalRounds <= 0) {
            throw new IllegalArgumentException("Total rounds must be positive: " + totalRounds);
        }
        if (roundTimeSeconds <= 0) {
            throw new IllegalArgumentException("Round time must be positive: " + roundTimeSeconds);
        }
    }

    /**
     * 将当前 MatchPreset 对象序列化为 NBT 标签。
     * 使用Java 21的var简化局部变量声明
     *
     * @return 序列化后的 CompoundTag 对象
     */
    public CompoundTag toNbt() {
        var tag = new CompoundTag();

        // 序列化CT出生点列表
        var ctSpawnsTag = new ListTag();
        for (var pos : ctSpawns) {
            var posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            ctSpawnsTag.add(posTag);
        }
        tag.put("ctSpawns", ctSpawnsTag);

        // 序列化T出生点列表
        var tSpawnsTag = new ListTag();
        for (var pos : tSpawns) {
            var posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            tSpawnsTag.add(posTag);
        }
        tag.put("tSpawns", tSpawnsTag);

        // 序列化商店位置
        if (ctShopPos != null) {
            var posTag = new CompoundTag();
            posTag.putInt("x", ctShopPos.getX());
            posTag.putInt("y", ctShopPos.getY());
            posTag.putInt("z", ctShopPos.getZ());
            tag.put("ctShopPos", posTag);
        }
        if (tShopPos != null) {
            var posTag = new CompoundTag();
            posTag.putInt("x", tShopPos.getX());
            posTag.putInt("y", tShopPos.getY());
            posTag.putInt("z", tShopPos.getZ());
            tag.put("tShopPos", posTag);
        }

        // 序列化包点区域
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
     * 使用Java 21的var简化局部变量声明和增强模式匹配
     *
     * @param tag 包含 MatchPreset 数据的 CompoundTag，不能为null
     * @return 反序列化后的 MatchPreset 实例
     * @throws IllegalArgumentException 如果tag为null
     */
    public static MatchPreset fromNbt(CompoundTag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("NBT tag cannot be null");
        }
        
        // 反序列化CT出生点
        var ctSpawns = new ArrayList<BlockPos>();
        tag.getList("ctSpawns", CompoundTag.TAG_COMPOUND).forEach(t -> {
            if (t instanceof CompoundTag posTag) {
                ctSpawns.add(new BlockPos(
                    posTag.getInt("x"), 
                    posTag.getInt("y"), 
                    posTag.getInt("z")
                ));
            }
        });

        // 反序列化T出生点
        var tSpawns = new ArrayList<BlockPos>();
        tag.getList("tSpawns", CompoundTag.TAG_COMPOUND).forEach(t -> {
            if (t instanceof CompoundTag posTag) {
                tSpawns.add(new BlockPos(
                    posTag.getInt("x"), 
                    posTag.getInt("y"), 
                    posTag.getInt("z")
                ));
            }
        });

        // 反序列化商店位置
        BlockPos ctShop = null;
        if (tag.contains("ctShopPos")) {
            var posTag = tag.getCompound("ctShopPos");
            ctShop = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
        }
        
        BlockPos tShop = null;
        if (tag.contains("tShopPos")) {
            var posTag = tag.getCompound("tShopPos");
            tShop = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
        }

        // 反序列化包点区域
        AABB aabbA = null;
        if (tag.contains("bombsiteA_minX")) {
            aabbA = new AABB(
                tag.getDouble("bombsiteA_minX"), tag.getDouble("bombsiteA_minY"), tag.getDouble("bombsiteA_minZ"),
                tag.getDouble("bombsiteA_maxX"), tag.getDouble("bombsiteA_maxY"), tag.getDouble("bombsiteA_maxZ")
            );
        }
        
        AABB aabbB = null;
        if (tag.contains("bombsiteB_minX")) {
            aabbB = new AABB(
                tag.getDouble("bombsiteB_minX"), tag.getDouble("bombsiteB_minY"), tag.getDouble("bombsiteB_minZ"),
                tag.getDouble("bombsiteB_maxX"), tag.getDouble("bombsiteB_maxY"), tag.getDouble("bombsiteB_maxZ")
            );
        }

        var rounds = tag.getInt("totalRounds");
        var time = tag.getInt("roundTimeSeconds");

        return new MatchPreset(ctSpawns, tSpawns, ctShop, tShop, aabbA, aabbB, rounds, time);
    }
}
