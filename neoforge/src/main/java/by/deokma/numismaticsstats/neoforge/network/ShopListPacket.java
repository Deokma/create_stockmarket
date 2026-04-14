package by.deokma.numismaticsstats.neoforge.network;

import by.deokma.numismaticsstats.shop.ShopEntry;
import by.deokma.numismaticsstats.shop.ShopListData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Sent server → client with the full shop list. */
public record ShopListPacket(List<ShopEntry> entries) implements CustomPacketPayload {

    public static final Type<ShopListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("numismaticsstats", "shop_list"));

    public static final StreamCodec<FriendlyByteBuf, ShopListPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeInt(pkt.entries.size());
                for (ShopEntry e : pkt.entries) e.write(buf);
            },
            buf -> {
                int size = buf.readInt();
                List<ShopEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(ShopEntry.read(buf));
                return new ShopListPacket(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ShopListPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ShopListData.set(pkt.entries());
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof by.deokma.numismaticsstats.neoforge.client.StockMarketScreen sms) {
                sms.refreshShopEntries();
            } else if (mc.screen instanceof by.deokma.numismaticsstats.neoforge.client.ShopListScreen screen) {
                screen.refreshEntries();
            }
        });
    }
}
