package com.qisumei.csgo.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 这是一个Mixin类，用于修改原版Villager（村民）的行为。
 * Mixin是一种强大的工具，可以在不直接修改原版代码的情况下，向其中注入我们自己的逻辑。
 * @Mixin(Villager.class) 注解告诉系统，这个“补丁”是打在 Villager 类上的。
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {

    // 使用@Shadow注解，可以让我们在Mixin类中访问到Villager类中的私有或保护方法。
    // 我们需要这些方法来复刻并修改原版的交易逻辑。
    @Shadow public abstract MerchantOffers getOffers();
    @Shadow public abstract void setTradingPlayer(@Nullable Player p_35399_);
    @Shadow protected abstract void openTradingScreen(Player p_35396_, Component p_35397_, int p_35398_);
    @Shadow public abstract VillagerData getVillagerData();

    /**
     * 这个方法使用@Inject注解，将我们的代码注入到原版Villager的`mobInteract`方法的开头。
     * `mobInteract`是处理玩家与生物交互的核心方法。
     * 我们的目标是：绕过原版代码中“检查是否已有玩家正在交易”的逻辑。
     * * @param pPlayer 与村民交互的玩家。
     * @param pHand 玩家使用的手。
     * @param cir 回调信息对象，它允许我们取消原方法的执行。
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void allowMultiplayerTrading(Player pPlayer, InteractionHand pHand, CallbackInfoReturnable<InteractionResult> cir) {
        // 首先，将'this'转换回Villager类型，以便调用它的方法。
        Villager thisVillager = (Villager)(Object)this;

        // 我们的修改只在服务器端生效，客户端的交互逻辑保持原样。
        if (thisVillager.level().isClientSide()) {
            return; // 直接返回，让原方法继续执行客户端的逻辑。
        }

        // 如果村民是幼年或者没有任何交易选项，我们也不需要干预，让原方法处理即可。
        if (thisVillager.isBaby() || this.getOffers().isEmpty()) {
            return;
        }

        // --- 核心自定义逻辑 ---

        // 设定当前交易的玩家。
        // 因为其他模组或原版逻辑可能仍会查询它。这个值会被下一个交互的玩家覆盖。
        this.setTradingPlayer(pPlayer);
        
        // 调用原版的内部方法，为当前玩家打开交易GUI。
        this.openTradingScreen(pPlayer, thisVillager.getDisplayName(), this.getVillagerData().getLevel());
        cir.setReturnValue(InteractionResult.SUCCESS);
        
        // 这可以防止原版代码中那个“只允许单人交易”的检查逻辑被运行到。
        cir.cancel();
    }
}
