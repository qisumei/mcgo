package com.qisumei.csgo.events;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.entity.ModEntityTypes;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端专用的事件处理器。
 * 使用 @EventBusSubscriber 注解将其自动注册到MOD事件总线上。
 * Dist.CLIENT 确保这个类只在客户端加载，避免在服务端引发崩溃。
 */
@EventBusSubscriber(modid = QisCSGO.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    /**
     * 监听实体渲染器注册事件。
     * 这个事件在游戏启动时，只在客户端被触发一次。
     * @param event 渲染器注册事件对象
     */
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 为我们的烟雾弹实体类型注册一个渲染器。
        // ThrownItemRenderer::new 是一个方法引用，它告诉Minecraft：
        // “当需要渲染一个SmokeGrenadeEntity时，请创建一个新的ThrownItemRenderer实例来处理它。”
        // ThrownItemRenderer是原版用于渲染雪球、鸡蛋等投掷物的渲染器，它会自动使用我们上面在getDefaultItem()中指定的物品模型。
        event.registerEntityRenderer(ModEntityTypes.SMOKE_GRENADE.get(), ThrownItemRenderer::new);
        
        QisCSGO.LOGGER.info("Registered entity renderers for " + QisCSGO.MODID);
    }
}
