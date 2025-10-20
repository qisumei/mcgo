package com.qisumei.csgo.game.preset;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 预设管理器，用于保存、加载和列出CSGO比赛预设。
 */
public class PresetManager {

    // --- 新增：定义一个用于存放预设的自定义资源路径 ---
    public static final LevelResource CSGO_PRESETS_FOLDER = new LevelResource("csgo_presets");

    /**
     * 获取服务器中预设文件夹的路径。如果该文件夹不存在，则创建它。
     *
     * @param server Minecraft服务器实例
     * @return 表示预设文件夹的File对象
     */
    private static File getPresetFolder(MinecraftServer server) {
        Path path = server.getWorldPath(CSGO_PRESETS_FOLDER);
        File folder = path.toFile();
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                QisCSGO.LOGGER.warn("无法创建预设文件夹: {}", folder.getAbsolutePath());
            }
        }
        return folder;
    }

    /**
     * 将指定的比赛预设保存到磁盘上。
     *
     * @param preset 比赛预设对象
     * @param name   预设名称（不含扩展名）
     * @param server Minecraft服务器实例
     * @return 是否成功保存
     */
    public static boolean savePreset(MatchPreset preset, String name, MinecraftServer server) {
        File presetFile = new File(getPresetFolder(server), name + ".dat");
        try {
            NbtIo.writeCompressed(preset.toNbt(), presetFile.toPath());
            return true;
        } catch (IOException e) {
            QisCSGO.LOGGER.error("无法保存比赛预设 '{}': {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 从磁盘加载指定名称的比赛预设。
     *
     * @param name   预设名称（不含扩展名）
     * @param server Minecraft服务器实例
     * @return 加载成功的MatchPreset对象；若失败或文件不存在则返回null
     */
    public static MatchPreset loadPreset(String name, MinecraftServer server) {
        File presetFile = new File(getPresetFolder(server), name + ".dat");
        if (!presetFile.exists()) {
            return null;
        }
        try {
            // --- 修正 #2: 调用 readCompressed 时添加 NbtAccounter.unlimitedHeap() ---
            CompoundTag tag = NbtIo.readCompressed(presetFile.toPath(), NbtAccounter.unlimitedHeap());
            return MatchPreset.fromNbt(tag);
        } catch (IOException e) {
            QisCSGO.LOGGER.error("无法加载比赛预设 '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * 列出当前所有已保存的比赛预设名称。
     *
     * @param server Minecraft服务器实例
     * @return 包含所有预设名称（不含扩展名）的列表
     */
    public static List<String> listPresets(MinecraftServer server) {
        File[] files = getPresetFolder(server).listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return new ArrayList<>();
        List<String> presetNames = new ArrayList<>();
        for (File file : files) {
            presetNames.add(file.getName().replace(".dat", ""));
        }
        return presetNames;
    }
}
