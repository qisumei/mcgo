package com.qisumei.csgo.entity;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.grenade.SmokeGrenadeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组实体类型注册类
 * 负责注册所有自定义实体，如投掷物、NPC等。
 */
public class ModEntityTypes {

    /**
     * 创建一个针对EntityType的延迟注册器。
     */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, QisCSGO.MODID);

    /**
     * 注册烟雾弹实体类型。
     * - build("smoke_grenade"): 设置实体的注册ID。
     * - aac(MobCategory.MISC): 将实体分类为“杂项”，这意味着它不受生物生成上限等规则影响。
     * - sized(0.25F, 0.25F): 定义实体的碰撞箱大小。
     * - build(SmokeGrenadeEntity::new): 指定如何创建这个实体的实例。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE =
            ENTITY_TYPES.register("smoke_grenade", () -> EntityType.Builder.<SmokeGrenadeEntity>of(SmokeGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    //设置跟踪范围为 160 格。这是解决远程不显示粒子的核心。
                .setTrackingRange(160)
                
                //设置更新间隔为 5 ticks。对于飞行道具，一个较短的间隔可以使远处的运动看起来更平滑。
                .updateInterval(1)
                
                // 确保实体会接收速度更新，这对于所有飞行道具都是必需的。
                .setShouldReceiveVelocityUpdates(true)
                    .build(QisCSGO.MODID + ":smoke_grenade"));


    /**
     * 将注册器附加到模组的事件总线上，以便在正确的时机执行注册。
     * @param eventBus 模组的事件总线
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}