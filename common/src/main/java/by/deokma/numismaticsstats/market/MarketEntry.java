package by.deokma.numismaticsstats.market;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of aggregated market data for a single item type.
 * Safe to send over the network.
 *
 * barterItem — the payment item for TableCloth shops (EMPTY if none / Vendor-only).
 */
public record MarketEntry(
        ResourceLocation itemId,
        ItemStack        displayStack,
        int              minPrice,
        int              avgPrice,
        int              sellCount,
        int              buyCount,
        PriceTrend       trend,
        List<Integer>    priceHistory,
        ItemStack        barterItem    // payment item for TableCloth; EMPTY if not barter
) {
    /** True when this entry has no Vendor price — only TableCloth barter shops. */
    public boolean isBarterOnly() { return minPrice <= 0 && avgPrice <= 0 && !barterItem.isEmpty(); }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        writeItem(buf, displayStack);
        buf.writeInt(minPrice);
        buf.writeInt(avgPrice);
        buf.writeInt(sellCount);
        buf.writeInt(buyCount);
        buf.writeByte(trend.ordinal());
        buf.writeInt(priceHistory.size());
        for (int price : priceHistory) buf.writeInt(price);
        writeItem(buf, barterItem);
    }

    public static MarketEntry read(FriendlyByteBuf buf) {
        ResourceLocation itemId       = buf.readResourceLocation();
        ItemStack        displayStack = readItem(buf);
        int              minPrice     = buf.readInt();
        int              avgPrice     = buf.readInt();
        int              sellCount    = buf.readInt();
        int              buyCount     = buf.readInt();
        PriceTrend       trend        = PriceTrend.values()[buf.readByte()];
        int              histSize     = buf.readInt();
        List<Integer>    priceHistory = new ArrayList<>(histSize);
        for (int i = 0; i < histSize; i++) priceHistory.add(buf.readInt());
        ItemStack        barterItem   = readItem(buf);
        return new MarketEntry(itemId, displayStack, minPrice, avgPrice,
                sellCount, buyCount, trend, priceHistory, barterItem);
    }

    private static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        var reg = buf instanceof net.minecraft.network.RegistryFriendlyByteBuf rfbb
                ? rfbb.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        buf.writeNbt(stack.isEmpty() ? null : stack.save(reg));
    }

    private static ItemStack readItem(FriendlyByteBuf buf) {
        var tag = buf.readNbt();
        if (tag == null) return ItemStack.EMPTY;
        var reg = buf instanceof net.minecraft.network.RegistryFriendlyByteBuf rfbb
                ? rfbb.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        return ItemStack.parseOptional(reg, tag);
    }
}
