package com.qisumei.csgo;

import com.mojang.logging.LogUtils;
import com.qisumei.csgo.c4.block.C4Block;
import com.qisumei.csgo.c4.item.C4Item;
import com.qisumei.csgo.c4.sound.ModSounds;
import com.qisumei.csgo.commands.CSCommand;
import com.qisumei.csgo.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mod(QisCSGO.MODID)
public class QisCSGO {
    public static final String MODID = "qiscsgo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredHolder<Item, C4Item> C4_ITEM = ITEMS.register("c4", C4Item::new);
    //定义C4方块
    public static final DeferredBlock<C4Block> C4_BLOCK = (DeferredBlock<C4Block>) QisCSGO.BLOCKS.register("c4",
        () -> new C4Block(BlockBehaviour.Properties.of()
            .destroyTime(6.45f)
            .explosionResistance(1200.0f)
            .sound(SoundType.METAL)
            .noOcclusion())
    );

    

    public QisCSGO(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("Qis的CSGO已被成功加载");

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onBuildCreativeModeTabContents);
        
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CSCommand.register(event.getDispatcher());
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            ServerConfig.bake();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            ServerConfig.bake();
        }
    }

    private void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(C4_ITEM.get());
        }
    }
}