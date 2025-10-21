package com.qisumei.csgo.client;

import com.qisumei.csgo.network.OpenShopPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端按键输入处理器
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientKeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.OPEN_SHOP.consumeClick()) {
            // 发送数据包到服务器请求打开商店
            PacketDistributor.sendToServer(new OpenShopPacket());
        }
    }
}

