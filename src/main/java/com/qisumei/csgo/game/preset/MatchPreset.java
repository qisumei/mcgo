package com.qisumei.csgo.game.preset;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 一个不可变的记录（Record），用于存储一场CSGO比赛的完整预设配置。
 * <p>
 * 使用 Record 可以简化数据类的编写，并确保其在创建后不可被修改，提高了代码的健壮性。
 * </p>
 *
 * @param ctSpawns         CT队伍的出生点列表。
 * @param tSpawns          T队伍的出生点列表。
 * @param ctShopPos        CT队伍的商店位置。
 * @param tShopPos         T队伍的商店位置。
 * @param bombsiteA        A炸弹区的边界框。
 * @param bombsiteB        B炸弹区的边界框。
 * @param totalRounds      比赛总回合数。
 * @param roundTimeSeconds 每回合的时间（秒）。
 * @author Qisumei
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
     * 将当前 MatchPreset 对象序列化为 NBT 标签，以便保存到文件中。
     *
     * @return 包含此预设所有数据的 {@link CompoundTag}。
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        // 序列化BlockPos列表
        tag.put("ctSpawns", serializeBlockPosList(this.ctSpawns));
        tag.put("tSpawns", serializeBlockPosList(this.tSpawns));

        // 序列化单个BlockPos（如果存在）
        Optional.ofNullable(this.ctShopPos).ifPresent(pos -> tag.put("ctShopPos", NbtUtils.writeBlockPos(pos)));
        Optional.ofNullable(this.tShopPos).ifPresent(pos -> tag.put("tShopPos", NbtUtils.writeBlockPos(pos)));

        // 序列化AABB（如果存在）
        Optional.ofNullable(this.bombsiteA).ifPresent(aabb -> tag.put("bombsiteA", serializeAABB(aabb)));
        Optional.ofNullable(this.bombsiteB).ifPresent(aabb -> tag.put("bombsiteB", serializeAABB(aabb)));

        // 序列化基本数据类型
        tag.putInt("totalRounds", this.totalRounds);
        tag.putInt("roundTimeSeconds", this.roundTimeSeconds);

        return tag;
    }

    /**
     * 从 NBT 标签反序列化为 MatchPreset 对象。
     *
     * @param tag 包含 MatchPreset 数据的 {@link CompoundTag}。
     * @return 一个新的 {@link MatchPreset} 实例。
     */
    public static MatchPreset fromNbt(CompoundTag tag) {
        // 反序列化BlockPos列表
        List<BlockPos> ctSpawns = deserializeBlockPosList(tag.getList("ctSpawns", CompoundTag.TAG_COMPOUND));
        List<BlockPos> tSpawns = deserializeBlockPosList(tag.getList("tSpawns", CompoundTag.TAG_COMPOUND));

        // 反序列化单个BlockPos（如果存在）
        // **[修复]** 使用新的 `readBlockPos(tag, key)` 方法，它返回一个 Optional。
        BlockPos ctShop = NbtUtils.readBlockPos(tag, "ctShopPos").orElse(null);
        BlockPos tShop = NbtUtils.readBlockPos(tag, "tShopPos").orElse(null);

        // 反序列化AABB（如果存在）
        AABB aabbA = tag.contains("bombsiteA") ? deserializeAABB(tag.getCompound("bombsiteA")) : null;
        AABB aabbB = tag.contains("bombsiteB") ? deserializeAABB(tag.getCompound("bombsiteB")) : null;

        // 反序列化基本数据类型
        int rounds = tag.getInt("totalRounds");
        int time = tag.getInt("roundTimeSeconds");

        return new MatchPreset(ctSpawns, tSpawns, ctShop, tShop, aabbA, aabbB, rounds, time);
    }

    // --- 私有辅助方法 ---

    /**
     * 将 BlockPos 列表序列化为 ListTag。
     */
    private static ListTag serializeBlockPosList(List<BlockPos> list) {
        ListTag listTag = new ListTag();
        list.forEach(pos -> listTag.add(NbtUtils.writeBlockPos(pos)));
        return listTag;
    }

    /**
     * 将 ListTag 反序列化为 BlockPos 列表。
     */
    private static List<BlockPos> deserializeBlockPosList(ListTag listTag) {
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            // **[修复]** 使用 NbtUtils.readBlockPos(CompoundTag) 的内联实现来绕过解析问题。
            list.add(deserializeBlockPos(listTag.getCompound(i)));
        }
        return list;
    }
    
    /**
     * 将 CompoundTag 反序列化为 BlockPos。
     * 这是对 `NbtUtils.readBlockPos(CompoundTag)` 的一个手动实现，以解决潜在的映射或版本兼容性问题。
     */
    private static BlockPos deserializeBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }

    /**
     * 将 AABB 序列化为 CompoundTag。
     */
    private static CompoundTag serializeAABB(AABB aabb) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("minX", aabb.minX);
        tag.putDouble("minY", aabb.minY);
        tag.putDouble("minZ", aabb.minZ);
        tag.putDouble("maxX", aabb.maxX);
        tag.putDouble("maxY", aabb.maxY);
        tag.putDouble("maxZ", aabb.maxZ);
        return tag;
    }

    /**
     * 将 CompoundTag 反序列化为 AABB。
     */
    private static AABB deserializeAABB(CompoundTag tag) {
        return new AABB(
                tag.getDouble("minX"), tag.getDouble("minY"), tag.getDouble("minZ"),
                tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ")
        );
    }
}

