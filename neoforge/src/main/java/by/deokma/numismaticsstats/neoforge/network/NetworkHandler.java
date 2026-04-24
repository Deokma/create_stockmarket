package by.deokma.numismaticsstats.neoforge.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {

    private NetworkHandler() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NetworkHandler::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        // Client → Server
        reg.playToServer(
                RequestShopListPacket.TYPE,
                RequestShopListPacket.CODEC,
                RequestShopListPacket::handle
        );

        // Server → Client
        reg.playToClient(
                ShopListPacket.TYPE,
                ShopListPacket.CODEC,
                ShopListPacket::handle
        );

        reg.playToClient(
                OpenShopListPacket.TYPE,
                OpenShopListPacket.CODEC,
                OpenShopListPacket::handle
        );

        // Market packets
        reg.playToServer(
                RequestMarketPacket.TYPE,
                RequestMarketPacket.CODEC,
                RequestMarketPacket::handle
        );

        reg.playToClient(
                MarketPacket.TYPE,
                MarketPacket.CODEC,
                MarketPacket::handle
        );

        reg.playToClient(
                OpenStockMarketPacket.TYPE,
                OpenStockMarketPacket.CODEC,
                OpenStockMarketPacket::handle
        );

        reg.playToClient(
                TradeStatsPacket.TYPE,
                TradeStatsPacket.CODEC,
                TradeStatsPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, ShopListPacket pkt) {
        PacketDistributor.sendToPlayer(player, pkt);
    }

    public static void sendToPlayer(ServerPlayer player, OpenShopListPacket pkt) {
        PacketDistributor.sendToPlayer(player, pkt);
    }

    public static void sendToPlayer(ServerPlayer player, MarketPacket pkt) {
        PacketDistributor.sendToPlayer(player, pkt);
    }

    public static void sendToPlayer(ServerPlayer player, OpenStockMarketPacket pkt) {
        PacketDistributor.sendToPlayer(player, pkt);
    }

    public static void sendToServer(RequestShopListPacket pkt) {
        PacketDistributor.sendToServer(pkt);
    }

    public static void sendToServer(RequestMarketPacket pkt) {
        PacketDistributor.sendToServer(pkt);
    }

    public static void sendToPlayer(ServerPlayer player, TradeStatsPacket pkt) {
        PacketDistributor.sendToPlayer(player, pkt);
    }
}
