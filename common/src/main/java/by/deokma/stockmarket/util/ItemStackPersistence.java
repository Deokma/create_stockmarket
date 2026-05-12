package by.deokma.stockmarket.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 1.21+ item NBT codec allows stack {@code count} only in {@code [1; 99]} on save.
 * Oversized stacks (e.g. 192 logs) throw {@link IllegalStateException} during {@link ItemStack#save}.
 * This helper stores the real count beside the serializable NBT.
 */
public final class ItemStackPersistence {

    /** Upper bound enforced by {@link ItemStack#save} codec (not necessarily {@link ItemStack#getMaxStackSize()}). */
    public static final int MAX_CODEC_COUNT = 99;

    private static final String WRAP_ITEM = "CSM_Item";
    private static final String WRAP_COUNT = "CSM_FullCount";

    private ItemStackPersistence() {}

    /** Writes non-empty stack under {@code key}; adds {@code key + "FullCount"} when count &gt; 99. */
    public static void writeIntoTag(CompoundTag parent, String key, ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return;
        int count = stack.getCount();
        ItemStack forCodec = count > MAX_CODEC_COUNT ? stack.copyWithCount(MAX_CODEC_COUNT) : stack;
        parent.put(key, forCodec.save(registries));
        if (count > MAX_CODEC_COUNT) {
            parent.putInt(key + "FullCount", count);
        }
    }

    public static ItemStack readFromTag(CompoundTag parent, String key, HolderLookup.Provider registries) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.parseOptional(registries, parent.getCompound(key));
        if (stack.isEmpty()) return stack;
        String overflow = key + "FullCount";
        if (parent.contains(overflow, Tag.TAG_INT)) {
            int full = parent.getInt(overflow);
            if (full > stack.getCount()) {
                stack = stack.copyWithCount(full);
            }
        }
        return stack;
    }

    /** Network: one NBT payload — legacy flat item tag or wrapper with full count. */
    public static void writeToNetworkBuf(FriendlyByteBuf buf, ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            buf.writeNbt(null);
            return;
        }
        int count = stack.getCount();
        if (count <= MAX_CODEC_COUNT) {
            buf.writeNbt(stack.save(registries));
            return;
        }
        CompoundTag wrap = new CompoundTag();
        wrap.putInt(WRAP_COUNT, count);
        wrap.put(WRAP_ITEM, stack.copyWithCount(MAX_CODEC_COUNT).save(registries));
        buf.writeNbt(wrap);
    }

    public static ItemStack readFromNetworkBuf(FriendlyByteBuf buf, HolderLookup.Provider registries) {
        return parseNetworkNbt(buf.readNbt(), registries);
    }

    /** Parses payload from {@link #writeToNetworkBuf} (legacy flat item tag or CSM wrapper). */
    public static ItemStack parseNetworkNbt(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null) return ItemStack.EMPTY;
        if (tag.contains(WRAP_ITEM, Tag.TAG_COMPOUND)) {
            int full = tag.getInt(WRAP_COUNT);
            ItemStack inner = ItemStack.parseOptional(registries, tag.getCompound(WRAP_ITEM));
            if (inner.isEmpty() || full <= 0) return inner;
            return full > inner.getCount() ? inner.copyWithCount(full) : inner;
        }
        return ItemStack.parseOptional(registries, tag);
    }
}
