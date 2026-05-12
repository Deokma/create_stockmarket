package by.deokma.create_stockmarket.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Stateless utility for rendering a player head icon (face + hat overlay)
 * from a Minecraft skin texture.
 *
 * <p>The face layer occupies UV region (8,8)→(16,16) on the 64×64 skin sheet.
 * The hat/overlay layer occupies UV region (40,8)→(48,16) on the same sheet.
 * Legacy 64×32 skins do not have a hat layer and are rendered with the face only.
 */
public final class PlayerHeadRenderer {

    // ── UV constants ──────────────────────────────────────────────────────────
    /** U offset of the face layer on the skin texture. */
    public static final int FACE_U = 8;
    /** V offset of the face layer on the skin texture. */
    public static final int FACE_V = 8;
    /** U offset of the hat/overlay layer on the skin texture. */
    public static final int HAT_U  = 40;
    /** V offset of the hat/overlay layer on the skin texture. */
    public static final int HAT_V  = 8;
    /** Width of the sampled UV region (8 pixels on the skin sheet). */
    public static final int UV_W   = 8;
    /** Height of the sampled UV region (8 pixels on the skin sheet). */
    public static final int UV_H   = 8;
    /** Full width of the skin texture sheet. */
    public static final int TEX_W  = 64;
    /** Full height of the skin texture sheet. */
    public static final int TEX_H  = 64;

    private PlayerHeadRenderer() {}

    /**
     * Renders a player head icon at {@code (x, y)} with the given pixel size.
     *
     * <p>For modern (64×64) skins, two blit calls are issued: the face layer
     * followed by the hat overlay at the same position. For legacy (64×32) skins
     * only the face layer is drawn.
     *
     * @param gfx      GuiGraphics rendering context
     * @param texture  ResourceLocation of the resolved skin texture (or fallback)
     * @param x        left edge of the icon in screen pixels
     * @param y        top edge of the icon in screen pixels
     * @param size     rendered width and height in pixels (square)
     * @param isLegacy {@code true} if the skin uses the legacy 64×32 format
     */
    public static void render(GuiGraphics gfx, ResourceLocation texture,
                              int x, int y, int size, boolean isLegacy) {
        if (texture == null) return;
        // Face layer
        gfx.blit(texture, x, y, size, size, FACE_U, FACE_V, UV_W, UV_H, TEX_W, TEX_H);
        // Hat/overlay layer (modern skins only)
        if (!isLegacy) {
            gfx.blit(texture, x, y, size, size, HAT_U, HAT_V, UV_W, UV_H, TEX_W, TEX_H);
        }
    }

    /**
     * Computes the icon size appropriate for a given row height.
     * The result is clamped to the range [8, 16].
     *
     * @param rowHeight height of the UI row in pixels
     * @return icon size in pixels, always in [8, 16]
     */
    public static int iconSizeForRow(int rowHeight) {
        return Math.max(8, Math.min(16, rowHeight));
    }
}
