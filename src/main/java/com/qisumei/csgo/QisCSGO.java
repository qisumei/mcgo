package com.qisumei.csgo;

import com.mojang.logging.LogUtils;
import com.qisumei.csgo.c4.block.C4Block;
import com.qisumei.csgo.c4.item.C4Item;
import com.qisumei.csgo.c4.sound.ModSounds;
import com.qisumei.csgo.commands.CSCommand;
import com.qisumei.csgo.config.ServerConfig;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * 模组主类，负责初始化和注册所有模组内容。
 * 这是模组加载的入口点，在这里我们注册物品、方块、声音、配置和事件监听器。
 *
 * @author Qisumei
 */
@Mod(QisCSGO.MODID)
public class QisCSGO {
    /**
     * 模组的唯一ID，用于标识和引用模组内容。
     */
    public static final String MODID = "qiscsgo";
    /**
     * 用于在控制台输出日志信息的日志记录器。
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    // 延迟注册器，用于在合适的时机自动注册物品和方块
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(MODID);

    /**
     * C4物品的延迟注册持有者。
     * 通过这个静态字段，我们可以在模组的任何地方安全地引用C4物品。
     */
    public static final DeferredHolder<Item, C4Item> C4_ITEM = ITEMS.register("c4", C4Item::new);

    /**
     * C4方块的延迟注册持有者。
     * 定义了C4方块的基本属性，例如硬度、爆炸抗性、声音和非不透明性。
     */
    @SuppressWarnings("unchecked")
    public static final DeferredHolder<Block, C4Block> C4_BLOCK = (DeferredHolder<Block, C4Block>)(DeferredHolder<?, ?>)BLOCKS.register("c4",
            () -> new C4Block(BlockBehaviour.Properties.of()
                    .destroyTime(6.45f) // 设置基础破坏时间
                    .explosionResistance(1200.0f) // 极高的爆炸抗性，防止被其他爆炸破坏
                    .sound(SoundType.METAL) // 设置方块交互的声音为金属声
                    .noOcclusion()) // 设置为非不透明方块，避免渲染问题
    );

    /**
     * 模组的构造函数。
     * NeoForge会在加载模组时调用此方法。
     *
     * @param modEventBus 模组事件总线，用于注册特定于模组加载阶段的事件监听器。
     * @param container 模组容器，提供与当前模组相关的信息和操作。
     */
    public QisCSGO(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("Qisu's CSGO Mod is loading!");

        // 注册服务器配置文件
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        // 将物品、方块和声音的延迟注册器附加到模组事件总线
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ModSounds.register(modEventBus);

        // 添加模组生命周期事件的监听器
        modEventBus.addListener(this::onConfigLoad); // 监听配置文件加载事件
        modEventBus.addListener(this::onConfigReload); // 监听配置文件重载事件
        modEventBus.addListener(this::onBuildCreativeModeTabContents); // 监听创造模式物品栏构建事件

        // 将通用事件的监听器注册到NeoForge的事件总线
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands); // 监听指令注册事件
    }

    /**
     * 当注册指令时调用的事件处理方法。
     *
     * @param event 指令注册事件对象。
     */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        CSCommand.register(event.getDispatcher());
    }

    /**
     * 当配置文件首次加载时调用的事件处理方法。
     *
     * @param event 配置文件加载事件对象。
     */
    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            ServerConfig.bake();
        }
    }

    /**
     * 当配置文件被重载时（例如，通过游戏内指令）调用的事件处理方法。
     *
     * @param event 配置文件重载事件对象。
     */
    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            ServerConfig.bake();
        }
    }

    /**
     * 当构建创造模式物品栏内容时调用的事件处理方法。
     * 用于将模组的物品添加到指定的创造模式物品栏标签页。
     *
     * @param event 创造模式物品栏内容构建事件对象。
     */
    private void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        // 将C4物品添加到“战斗”标签页
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(C4_ITEM.get());
        }
    }
}