package by.deokma.numismaticsstats.neoforge.network;

import by.deokma.numismaticsstats.market.MarketData;
import by.deokma.numismaticsstats.market.MarketEntry;
import by.deokma.numismaticsstats.neoforge.client.StockMarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Sent server → client with the full market entry list. */
public record MarketPacket(List<MarketEntry> entries) implements CustomPacketPayload {

    public static final Type<MarketPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("numismaticsstats", "market_data"));

    public static final StreamCodec<FriendlyByteBuf, MarketPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeInt(pkt.entries.size());
                for (MarketEntry e : pkt.entries) e.write(buf);
            },
            buf -> {
                int size = buf.readInt();
                List<MarketEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(MarketEntry.read(buf));
                return new MarketPacket(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MarketPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            MarketData.set(pkt.entries());
            MarketData.setLoading(false);
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof StockMarketScreen screen) {
                screen.refreshEntries();
            }
        });
    }
}
