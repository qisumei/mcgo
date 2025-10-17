package com.qisumei.csgo.c4.sound;

import com.qisumei.csgo.QisCSGO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, QisCSGO.MODID);

    // 使用 fromNamespaceAndPath 创建 ResourceLocation
    public static final DeferredHolder<SoundEvent, SoundEvent> TW_SOUND = 
        SOUND_EVENTS.register("tw", 
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "tw")
            ));

    public static final DeferredHolder<SoundEvent, SoundEvent> CTW_SOUND =
        SOUND_EVENTS.register("ctw",
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "ctw")
            ));

    public static final DeferredHolder<SoundEvent, SoundEvent> C4_PLACE = 
        SOUND_EVENTS.register("c4_place", 
            () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "c4_place")
            ));

    // 原版音效引用也使用新方法
    public static SoundEvent ALARM_SOUND() {
        return BuiltInRegistries.SOUND_EVENT.get(
            ResourceLocation.fromNamespaceAndPath("minecraft", "block.note_block.hat")
        );
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}