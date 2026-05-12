package by.deokma.create_stockmarket.shop;

import by.deokma.create_stockmarket.util.ItemStackPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Snapshot of a shop block entity — safe to send over the network.
 * Covers Numismatics Vendor, Create TableCloth, and optional Tradeworks shelves.
 */
public record ShopEntry(
        BlockPos  pos,
        String    dimensionId,
        ItemStack sellingItem,
        int       totalPriceInSpurs,   // Numismatics coins (0 if item-priced shop)
        ItemStack priceItem,           // Barter item price (EMPTY if Vendor)
        UUID      ownerUuid,
        String    ownerName,
        String    mode,                // "SELL" / "BUY"
        String    shopType,            // "VENDOR" / "TABLECLOTH" / "TRADEWORKS"
        int       offerIndex           // TableCloth ordered_stacks index; -1 for Vendor / unused
) {
    public boolean isTableCloth() { return "TABLECLOTH".equals(shopType); }

    public boolean isTradeworks() { return "TRADEWORKS".equals(shopType); }

    /** Table cloth or Tradeworks — priced with {@link #priceItem()}, not coins. */
    public boolean usesItemPrice() {
        return "TABLECLOTH".equals(shopType) || "TRADEWORKS".equals(shopType);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(dimensionId);
        writeItem(buf, sellingItem);
        buf.writeInt(totalPriceInSpurs);
        writeItem(buf, priceItem);
        buf.writeUUID(ownerUuid);
        buf.writeUtf(ownerName);
        buf.writeUtf(mode);
        buf.writeUtf(shopType);
        buf.writeInt(offerIndex);
    }

    public static ShopEntry read(FriendlyByteBuf buf) {
        BlockPos  pos         = buf.readBlockPos();
        String    dimensionId = buf.readUtf();
        ItemStack sellingItem = readItem(buf);
        int       price       = buf.readInt();
        ItemStack priceItem   = readItem(buf);
        UUID      ownerUuid   = buf.readUUID();
        String    ownerName   = buf.readUtf();
        String    mode        = buf.readUtf();
        String    shopType    = buf.readUtf();
        int       offerIndex  = buf.readInt();
        return new ShopEntry(pos, dimensionId, sellingItem, price, priceItem,
                ownerUuid, ownerName, mode, shopType, offerIndex);
    }

    private static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        var reg = buf instanceof net.minecraft.network.RegistryFriendlyByteBuf rfbb
                ? rfbb.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        if (stack.isEmpty()) {
            buf.writeNbt(null);
        } else {
            ItemStackPersistence.writeToNetworkBuf(buf, stack, reg);
        }
    }

    private static ItemStack readItem(FriendlyByteBuf buf) {
        var tag = buf.readNbt();
        var reg = buf instanceof net.minecraft.network.RegistryFriendlyByteBuf rfbb
                ? rfbb.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        return ItemStackPersistence.parseNetworkNbt(tag, reg);
    }
}
