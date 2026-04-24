package by.deokma.numismaticsstats.neoforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent server → client to tell the client to open the ShopListScreen. */
public record OpenShopListPacket() implements CustomPacketPayload {

    public static final Type<OpenShopListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("numismaticsstats", "open_shop_list"));

    public static final StreamCodec<FriendlyByteBuf, OpenShopListPacket> CODEC =
            StreamCodec.unit(new OpenShopListPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenShopListPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(ClientPacketHandler::handleOpenShopList);
    }
}
