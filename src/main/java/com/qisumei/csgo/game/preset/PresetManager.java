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
import java.util.Collections;
import java.util.List;

/**
 * 预设管理器工具类，负责处理CSGO比赛预设的加载、保存和列出操作。
 * <p>
 * 这是一个静态工具类，所有方法都是静态的。它将预设文件存储在世界存档目录下的一个自定义文件夹 ({@code csgo_presets}) 中。
 * </p>
 *
 * @author Qisumei
 */
public final class PresetManager {

    /**
     * 指向世界存档中 "csgo_presets" 文件夹的 {@link LevelResource}。
     * NeoForge 使用此对象来安全地定位世界文件夹中的路径。
     */
    private static final LevelResource CSGO_PRESETS_FOLDER = new LevelResource("csgo_presets");
    private static final String FILE_EXTENSION = ".dat";

    /**
     * 私有构造函数，防止该工具类被实例化。
     */
    private PresetManager() {}

    /**
     * 获取或创建用于存储比赛预设的文件夹。
     *
     * @param server Minecraft服务器实例。
     * @return 代表预设文件夹的 {@link File} 对象。
     */
    private static File getPresetFolder(MinecraftServer server) {
        Path path = server.getWorldPath(CSGO_PRESETS_FOLDER);
        File folder = path.toFile();
        if (!folder.exists() && !folder.mkdirs()) {
            QisCSGO.LOGGER.warn("无法创建预设文件夹: {}", folder.getAbsolutePath());
        }
        return folder;
    }

    /**
     * 将一个比赛预设保存到文件中。
     *
     * @param preset     要保存的 {@link MatchPreset} 对象。
     * @param name       预设的名称（将作为文件名）。
     * @param server     Minecraft服务器实例。
     * @return 如果保存成功，则返回 true；否则返回 false。
     */
    public static boolean savePreset(MatchPreset preset, String name, MinecraftServer server) {
        File presetFile = new File(getPresetFolder(server), name + FILE_EXTENSION);
        try {
            NbtIo.writeCompressed(preset.toNbt(), presetFile.toPath());
            return true;
        } catch (IOException e) {
            QisCSGO.LOGGER.error("无法保存比赛预设 '{}': {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 从文件中加载一个指定名称的比赛预设。
     *
     * @param name   要加载的预设名称。
     * @param server Minecraft服务器实例。
     * @return 如果加载成功，则返回一个 {@link MatchPreset} 对象；如果文件不存在或加载失败，则返回 null。
     */
    public static MatchPreset loadPreset(String name, MinecraftServer server) {
        File presetFile = new File(getPresetFolder(server), name + FILE_EXTENSION);
        if (!presetFile.exists()) {
            return null;
        }
        try {
            // 使用 NbtAccounter.unlimitedHeap() 以允许读取任意大小的NBT数据，因为这是服务器自己的文件。
            CompoundTag tag = NbtIo.readCompressed(presetFile.toPath(), NbtAccounter.unlimitedHeap());
            return MatchPreset.fromNbt(tag);
        } catch (IOException e) {
            QisCSGO.LOGGER.error("无法加载比赛预设 '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * 列出所有已保存的比赛预设的名称。
     *
     * @param server Minecraft服务器实例。
     * @return 一个包含所有预设名称的字符串列表。
     */
    public static List<String> listPresets(MinecraftServer server) {
        File[] files = getPresetFolder(server).listFiles((dir, name) -> name.endsWith(FILE_EXTENSION));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> presetNames = new ArrayList<>();
        for (File file : files) {
            presetNames.add(file.getName().replace(FILE_EXTENSION, ""));
        }
        return presetNames;
    }
}
