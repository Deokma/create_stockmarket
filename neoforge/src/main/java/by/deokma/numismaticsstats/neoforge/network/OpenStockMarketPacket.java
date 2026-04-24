package by.deokma.numismaticsstats.neoforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent server → client to open the StockMarketScreen. */
public record OpenStockMarketPacket() implements CustomPacketPayload {

    public static final Type<OpenStockMarketPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("numismaticsstats", "open_stock_market"));

    public static final StreamCodec<FriendlyByteBuf, OpenStockMarketPacket> CODEC =
            StreamCodec.unit(new OpenStockMarketPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenStockMarketPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(ClientPacketHandler::handleOpenStockMarket);
    }
}
