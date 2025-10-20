package com.qisumei.csgo.c4.sound;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组音效注册类。
 * <p>
 * 此类负责将 {@code resources/assets/qiscsgo/sounds.json} 中定义的所有自定义音效
 * 注册到游戏中，以便它们可以被代码调用。
 * </p>
 *
 * @author Qisumei
 */
public class ModSounds {
    /**
     * 音效事件的延迟注册器。
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, QisCSGO.MODID);

    // --- 自定义音效注册 ---
    public static final DeferredHolder<SoundEvent, SoundEvent> T_WIN = registerSoundEvent("tw");
    public static final DeferredHolder<SoundEvent, SoundEvent> CT_WIN = registerSoundEvent("ctw");
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_PLACE = registerSoundEvent("c4_place");

    /**
     * C4倒计时提示音。
     * 这是一个对原版音效的引用，用于C4的“滴答”声。
     */
    public static final SoundEvent ALARM_SOUND = BuiltInRegistries.SOUND_EVENT.get(
            ResourceLocation.withDefaultNamespace("block.note_block.hat")
    );

    /**
     * 辅助方法，用于快速注册一个音效事件。
     *
     * @param name 音效的名称（与 sounds.json 中的键名一致）。
     * @return 一个指向已注册音效的 DeferredHolder。
     */
    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    /**
     * 将音效注册器附加到模组事件总线。
     *
     * @param eventBus 模组事件总线实例。
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
