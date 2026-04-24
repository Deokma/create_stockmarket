package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.block.MarketTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Renders an animated ender eye floating above the Market Terminal block.
 * The eye hovers, rotates slowly, and tracks the nearest player —
 * similar to the enchanting table book animation.
 */
public class MarketTerminalRenderer implements BlockEntityRenderer<MarketTerminalBlockEntity> {

    private static final Logger LOGGER = LogManager.getLogger("numismaticsstats");

    private static final ResourceLocation EYE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_eye.png");

    // Eye hovers above the block top (model top is at y=14/16=0.875)
    // Base hover height + bobbing
    private static final float EYE_BASE_Y  = 1.05f;  // just above the model top
    private static final float EYE_RADIUS  = 0.25f;

    // Bobbing
    private static final float BOB_SPEED   = 1.8f;
    private static final float BOB_HEIGHT  = 0.06f;

    // Slow spin speed (degrees per tick)
    private static final float SPIN_SPEED  = 0.8f;

    private static boolean loggedOnce = false;

    public MarketTerminalRenderer(BlockEntityRendererProvider.Context ctx) {
        LOGGER.info("[MarketTerminalRenderer] Renderer created");
    }

    @Override
    public void render(MarketTerminalBlockEntity be, float partialTick,
                       PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {

        if (!loggedOnce) {
            LOGGER.info("[MarketTerminalRenderer] First render at {}", be.getBlockPos());
            loggedOnce = true;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        BlockPos blockPos = be.getBlockPos();
        double bx = blockPos.getX() + 0.5;
        double by = blockPos.getY();
        double bz = blockPos.getZ() + 0.5;

        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        float time = gameTime + partialTick;

        // Bobbing offset
        float bob = Mth.sin(time * BOB_SPEED * 0.05f) * BOB_HEIGHT;

        // Slow continuous spin around Y axis
        float spinY = (time * SPIN_SPEED) % 360f;

        // Track player — horizontal yaw only (eye looks at player)
        Vec3 eyePos = player.getEyePosition(partialTick);
        double dx = eyePos.x - bx;
        double dz = eyePos.z - bz;
        float yawToPlayer = (float) Math.atan2(dz, dx);

        // Slight pitch toward player
        double dy = eyePos.y - (by + EYE_BASE_Y + bob);
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float pitch = Mth.clamp(-(float) Math.atan2(dy, horizDist), -0.5f, 0.5f);

        pose.pushPose();

        // Position: center X/Z, hover above block
        pose.translate(0.5, EYE_BASE_Y + bob, 0.5);

        // 1. Spin slowly around Y (continuous rotation)
        pose.mulPose(Axis.YP.rotationDegrees(spinY));

        // 2. Tilt toward player (yaw + pitch)
        pose.mulPose(Axis.YP.rotation(-(float)(Math.PI / 2.0) - yawToPlayer - (float)Math.toRadians(spinY)));
        pose.mulPose(Axis.XP.rotation(pitch));

        // Draw eye quad — always fully lit (glowing effect)
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(EYE_TEXTURE));
        PoseStack.Pose last = pose.last();
        float r = EYE_RADIUS;

        vc.addVertex(last, -r, -r, 0).setColor(255, 255, 255, 255)
                .setUv(0, 1).setOverlay(packedOverlay).setLight(0xF000F0).setNormal(last, 0, 0, 1);
        vc.addVertex(last,  r, -r, 0).setColor(255, 255, 255, 255)
                .setUv(1, 1).setOverlay(packedOverlay).setLight(0xF000F0).setNormal(last, 0, 0, 1);
        vc.addVertex(last,  r,  r, 0).setColor(255, 255, 255, 255)
                .setUv(1, 0).setOverlay(packedOverlay).setLight(0xF000F0).setNormal(last, 0, 0, 1);
        vc.addVertex(last, -r,  r, 0).setColor(255, 255, 255, 255)
                .setUv(0, 0).setOverlay(packedOverlay).setLight(0xF000F0).setNormal(last, 0, 0, 1);

        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MarketTerminalBlockEntity be) {
        return true;
    }
}
