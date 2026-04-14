package by.deokma.numismaticsstats.neoforge.command;

import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.OpenShopListPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ShopListCommand {

    private ShopListCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shoplist")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                        return 0;
                    }
                    // Tell the client to open the ShopListScreen
                    NetworkHandler.sendToPlayer(player, new OpenShopListPacket());
                    return 1;
                })
        );
    }
}
