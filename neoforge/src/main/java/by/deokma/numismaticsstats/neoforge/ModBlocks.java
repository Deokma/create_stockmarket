package by.deokma.numismaticsstats.neoforge;

import by.deokma.numismaticsstats.NumismaticsStats;
import by.deokma.numismaticsstats.block.MarketTerminalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, NumismaticsStats.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, NumismaticsStats.MOD_ID);

    public static final Supplier<MarketTerminalBlock> MARKET_TERMINAL = BLOCKS.register(
            "market_terminal",
            () -> new MarketTerminalBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.5f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );

    public static final Supplier<BlockItem> MARKET_TERMINAL_ITEM = ITEMS.register(
            "market_terminal",
            () -> new BlockItem(MARKET_TERMINAL.get(), new Item.Properties())
    );

    private ModBlocks() {}
}
