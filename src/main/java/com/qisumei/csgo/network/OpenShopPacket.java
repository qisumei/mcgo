package com.qisumei.csgo.network;

import com.qisumei.csgo.QisCSGO;
import com.qisumei.csgo.economy.ShopGUI;
import com.qisumei.csgo.game.Match;
import com.qisumei.csgo.game.PlayerStats;
import com.qisumei.csgo.service.ServiceFallbacks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 打开商店数据包 - 客户端按P键后发送到服务器
 */
public record OpenShopPacket() implements CustomPacketPayload {

    public static final Type<OpenShopPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QisCSGO.MODID, "open_shop"));

    public static final StreamCodec<ByteBuf, OpenShopPacket> STREAM_CODEC = StreamCodec.unit(new OpenShopPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理逻辑
     */
    public static void handle(OpenShopPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 检查玩家是否在比赛中
                Match match = ServiceFallbacks.getPlayerMatch(player);
                if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你不在进行中的比赛中！"));
                    return;
                }

                // 检查是否在购买阶段
                if (match.getRoundState() != Match.RoundState.BUY_PHASE) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c只能在购买阶段打开商店！"));
                    return;
                }

                // 获取玩家队伍
                PlayerStats stats = match.getPlayerStats().get(player.getUUID());
                if (stats == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c无法获取你的队伍信息！"));
                    return;
                }

                String team = stats.getTeam();

                // 打开商店GUI
                ShopGUI.openShop(player, team);
            }
        });
    }
}

