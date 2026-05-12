package by.deokma.stockmarket.neoforge.network;

import by.deokma.stockmarket.market.MarketEntry;
import by.deokma.stockmarket.neoforge.market.MarketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Sent client → server to request market data. */
public record RequestMarketPacket() implements CustomPacketPayload {

    public static final Type<RequestMarketPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("stockmarket", "request_market"));

    public static final StreamCodec<FriendlyByteBuf, RequestMarketPacket> CODEC =
            StreamCodec.unit(new RequestMarketPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestMarketPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            List<MarketEntry> entries = MarketRegistry.buildEntries(player.server);
            NetworkHandler.sendToPlayer(player, new MarketPacket(entries));
            // Also send trade stats leaderboard
            var stats = by.deokma.stockmarket.neoforge.market.TradeStatsSavedData
                    .getOrCreate(player.server);
            NetworkHandler.sendToPlayer(player, new TradeStatsPacket(stats.getTopSellers(10)));
        });
    }
}
