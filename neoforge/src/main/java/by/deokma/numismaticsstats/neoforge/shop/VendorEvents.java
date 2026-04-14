package by.deokma.numismaticsstats.neoforge.shop;

import by.deokma.numismaticsstats.neoforge.compat.NumismaticsCompat;
import by.deokma.numismaticsstats.neoforge.compat.VendorIndexer;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class VendorEvents {

    private static final int REFRESH_INTERVAL = 6000; // 5 minutes at 20 TPS

    private static int tickCounter = 0;

    private VendorEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VendorEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(VendorEvents::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(VendorEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(VendorEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(VendorEvents::onLevelUnload);
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        VendorRegistry.onChunkLoad(level, chunk);
    }

    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Block block = event.getPlacedBlock().getBlock();
        boolean isShopBlock = block instanceof TableClothBlock
                || (NumismaticsCompat.isPresent() && VendorIndexer.isVendorBlock(block));
        if (!isShopBlock) return;
        VendorRegistry.onBlockPlace(level, event.getPos());
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Block block = event.getState().getBlock();
        boolean isShopBlock = block instanceof TableClothBlock
                || (NumismaticsCompat.isPresent() && VendorIndexer.isVendorBlock(block));
        if (!isShopBlock) return;
        VendorRegistry.onBlockBreak(level, event.getPos());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < REFRESH_INTERVAL) return;
        tickCounter = 0;
        VendorRegistry.refreshLoaded(event.getServer());
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            VendorRegistry.clear();
        }
    }
}
