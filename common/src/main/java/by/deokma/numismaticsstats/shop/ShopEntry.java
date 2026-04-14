package by.deokma.numismaticsstats.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Snapshot of a shop block entity — safe to send over the network.
 * Covers both Numismatics Vendor and Create TableCloth shops.
 */
public record ShopEntry(
        BlockPos  pos,
        String    dimensionId,
        ItemStack sellingItem,
        int       totalPriceInSpurs,   // Numismatics coins (0 if TableCloth)
        ItemStack priceItem,           // Create item price (EMPTY if Vendor)
        UUID      ownerUuid,
        String    ownerName,
        String    mode,                // "SELL" / "BUY"
        String    shopType             // "VENDOR" / "TABLECLOTH"
) {
    public boolean isTableCloth() { return "TABLECLOTH".equals(shopType); }

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
        return new ShopEntry(pos, dimensionId, sellingItem, price, priceItem,
                ownerUuid, ownerName, mode, shopType);
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
