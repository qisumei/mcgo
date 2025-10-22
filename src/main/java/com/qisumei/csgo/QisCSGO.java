package com.qisumei.csgo;

import com.mojang.logging.LogUtils;
import com.qisumei.csgo.c4.block.C4Block;
import com.qisumei.csgo.c4.item.C4Item;
import com.qisumei.csgo.c4.sound.ModSounds;
import com.qisumei.csgo.commands.CSCommand;
import com.qisumei.csgo.client.KeyBindings;
import com.qisumei.csgo.network.OpenShopPacket;
import com.qisumei.csgo.config.ServerConfig;
import com.qisumei.csgo.service.ServiceRegistry;
import com.qisumei.csgo.service.MatchServiceImpl;
import com.qisumei.csgo.service.EconomyServiceImpl;
import com.qisumei.csgo.service.MatchService;
import com.qisumei.csgo.service.EconomyService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
    public static final DeferredHolder<Item, SmokeGrenadeItem> SMOKE_GRENADE_ITEM = ITEMS.register("smoke_grenade",
            () -> new SmokeGrenadeItem(new Item.Properties()));
    public static final DeferredBlock<C4Block> C4_BLOCK = (DeferredBlock<C4Block>) QisCSGO.BLOCKS.register("c4",
        () -> new C4Block(BlockBehaviour.Properties.of()
            .destroyTime(6.45f)
            .explosionResistance(1200.0f)
            .sound(SoundType.METAL)
            .noOcclusion())
    );

    

    public QisCSGO(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("MCTP的CSGO已被成功加载");

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ModSounds.register(modEventBus);

        // 注册默认 MatchService（当前实现委托给老的 MatchManager，以保证兼容）
        MatchService previousMatch = ServiceRegistry.register(MatchService.class, new MatchServiceImpl());
        if (previousMatch != null) {
            LOGGER.warn("A previous MatchService was replaced during initialization: {}", previousMatch.getClass().getName());
        }
        // 注册默认 EconomyService（当前实现委托给旧的 EconomyManager）
        EconomyService previousEconomy = ServiceRegistry.register(EconomyService.class, new EconomyServiceImpl());
        if (previousEconomy != null) {
            LOGGER.warn("A previous EconomyService was replaced during initialization: {}", previousEconomy.getClass().getName());
        }


        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onBuildCreativeModeTabContents);
        modEventBus.addListener(this::onRegisterPayloads);
        modEventBus.addListener(this::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
            OpenShopPacket.TYPE,
            OpenShopPacket.STREAM_CODEC,
            OpenShopPacket::handle
        );
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_SHOP);
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
            event.accept(SMOKE_GRENADE_ITEM.get());
        }
    }
}