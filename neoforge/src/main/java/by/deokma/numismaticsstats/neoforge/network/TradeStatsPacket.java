package by.deokma.numismaticsstats.neoforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sent server → client with the top sellers leaderboard.
 * Contains a list of (ownerName, salesCount) pairs sorted by sales descending.
 */
public record TradeStatsPacket(List<Map.Entry<String, Long>> topSellers) implements CustomPacketPayload {

    public static final Type<TradeStatsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("numismaticsstats", "trade_stats"));

    public static final StreamCodec<FriendlyByteBuf, TradeStatsPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeInt(pkt.topSellers.size());
                for (var e : pkt.topSellers) {
                    buf.writeUtf(e.getKey());
                    buf.writeLong(e.getValue());
                }
            },
            buf -> {
                int size = buf.readInt();
                List<Map.Entry<String, Long>> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String name = buf.readUtf();
                    long count = buf.readLong();
                    list.add(Map.entry(name, count));
                }
                return new TradeStatsPacket(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TradeStatsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandler.handleTradeStats(pkt.topSellers()));
    }
}
