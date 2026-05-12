package by.deokma.create_stockmarket.neoforge.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Centralized texture registry for all UI elements.
 *
 * Texture files live under:
 *   assets/create_stockmarket/textures/gui/market/
 *     panel/     — main panel background, shadow, border, header
 *     tabs/      — tab strip, active/inactive tab pills
 *     toolbar/   — toolbar, search field, column header, barter header, footer
 *     rows/      — table row backgrounds, hot stripe, scrollbar
 *     sidebar/   — sidebar background, section header, row states
 *     buttons/   — refresh, close, nav buttons
 *     badges/    — SELL/BUY mode badges, count badge
 *     charts/    — sparkline background
 */
public final class GuiTextures {
    private static final String MOD_ID = "create_stockmarket";

    // ── panel/ ────────────────────────────────────────────────────────────────
    /** Tileable panel background — fills the entire main panel */
    public static final ResourceLocation PANEL_BG     = loc("gui/market/panel/bg");
    /** Drop shadow rendered behind the main panel */
    public static final ResourceLocation PANEL_SHADOW = loc("gui/market/panel/shadow");
    /** 1×1 gold pixel — tiled to draw all border lines */
    public static final ResourceLocation PANEL_BORDER = loc("gui/market/panel/border");
    /** Horizontal separator line */
    public static final ResourceLocation SEPARATOR    = loc("gui/market/panel/separator");

    // ── tabs/ ─────────────────────────────────────────────────────────────────
    /** Background strip behind the owner-tab row */
    public static final ResourceLocation TAB_STRIP    = loc("gui/market/tabs/strip");
    /** Active tab pill */
    public static final ResourceLocation TAB_ACTIVE   = loc("gui/market/tabs/active");
    /** Inactive tab pill */
    public static final ResourceLocation TAB_INACTIVE = loc("gui/market/tabs/inactive");

    // ── toolbar/ ──────────────────────────────────────────────────────────────
    public static final ResourceLocation TOOLBAR       = loc("gui/market/toolbar/toolbar");
    public static final ResourceLocation SEARCH_BG     = loc("gui/market/toolbar/search_bg");
    public static final ResourceLocation COL_HEADER    = loc("gui/market/toolbar/col_header");
    public static final ResourceLocation FOOTER        = loc("gui/market/toolbar/footer");

    // ── rows/ ─────────────────────────────────────────────────────────────────
    public static final ResourceLocation ROW_ODD      = loc("gui/market/rows/odd");
    public static final ResourceLocation ROW_HOT      = loc("gui/market/rows/hot");
    public static final ResourceLocation ROW_HOVER    = loc("gui/market/rows/hover");
    public static final ResourceLocation HOT_STRIPE   = loc("gui/market/rows/hot_stripe");
    public static final ResourceLocation SCROLL_TRACK = loc("gui/market/rows/scroll_track");
    public static final ResourceLocation SCROLL_THUMB = loc("gui/market/rows/scroll_thumb");

    // ── sidebar/ ──────────────────────────────────────────────────────────────
    /** Sidebar panel background — tileable vertically */
    public static final ResourceLocation SIDEBAR_BG     = loc("gui/market/sidebar/bg");
    /** Section header inside sidebar */
    public static final ResourceLocation SIDEBAR_HDR    = loc("gui/market/sidebar/header");
    /** Active sidebar row */
    public static final ResourceLocation SIDEBAR_ACTIVE = loc("gui/market/sidebar/row_active");
    /** Hovered sidebar row */
    public static final ResourceLocation SIDEBAR_HOVER  = loc("gui/market/sidebar/row_hover");

    // ── buttons/ ──────────────────────────────────────────────────────────────
    public static final ResourceLocation BTN_REFRESH     = loc("gui/market/buttons/refresh");
    public static final ResourceLocation BTN_REFRESH_HOV = loc("gui/market/buttons/refresh_hov");
    public static final ResourceLocation BTN_CLOSE       = loc("gui/market/buttons/close");
    public static final ResourceLocation BTN_CLOSE_HOV   = loc("gui/market/buttons/close_hov");

    // ── badges/ ───────────────────────────────────────────────────────────────
    /** Red background for SELL mode badge */
    public static final ResourceLocation BADGE_SELL  = loc("gui/market/badges/sell");
    /** Green background for BUY mode badge */
    public static final ResourceLocation BADGE_BUY   = loc("gui/market/badges/buy");
    /** Gold background for active-filter count badge */
    public static final ResourceLocation BADGE_COUNT = loc("gui/market/badges/count");

    // ── icons/ ────────────────────────────────────────────────────────────────
    /** Icon for the Shops tab */
    public static final ResourceLocation ICON_SHOPS      = loc("gui/market/icons/shops");
    /** Icon for the Market tab */
    public static final ResourceLocation ICON_MARKET     = loc("gui/market/icons/markets");
    /** Icon for the Top Sellers tab */
    public static final ResourceLocation ICON_TOP_SELLERS = loc("gui/market/icons/top_saller");

    // ── Texture Dimensions ────────────────────────────────────────────────────
    public static final class Dimensions {
        // panel/
        public static final int PANEL_BG_W      = 256;
        public static final int PANEL_BG_H      = 236;
        public static final int SEPARATOR_W     = 256;
        public static final int SEPARATOR_H     = 1;

        // tabs/
        public static final int TAB_STRIP_W     = 256;
        public static final int TAB_STRIP_H     = 18;
        public static final int TAB_W           = 64;
        public static final int TAB_H           = 14;

        // toolbar/
        public static final int TOOLBAR_W       = 256;
        public static final int TOOLBAR_H       = 26;
        public static final int SEARCH_BG_W     = 256;
        public static final int SEARCH_BG_H     = 16;
        public static final int COL_HEADER_W    = 256;
        public static final int COL_HEADER_H    = 14;
        public static final int FOOTER_W        = 256;
        public static final int FOOTER_H        = 26;

        // rows/
        public static final int ROW_W           = 4;
        public static final int ROW_H           = 24;
        public static final int HOT_STRIPE_W    = 2;
        public static final int HOT_STRIPE_H    = 24;
        public static final int SCROLL_TRACK_W  = 4;
        public static final int SCROLL_TRACK_H  = 16;
        public static final int SCROLL_THUMB_W  = 4;
        public static final int SCROLL_THUMB_H  = 16;

        // sidebar/
        public static final int SIDEBAR_BG_W    = 112;
        public static final int SIDEBAR_BG_H    = 256;
        public static final int SIDEBAR_HDR_W   = 110;
        public static final int SIDEBAR_HDR_H   = 16;
        public static final int SIDEBAR_ROW_W   = 110;
        public static final int SIDEBAR_ROW_H   = 18;

        // buttons/
        public static final int BTN_W           = 18;
        public static final int BTN_H           = 20;

        // badges/
        public static final int BADGE_SELL_W    = 32;
        public static final int BADGE_SELL_H    = 16;
        public static final int BADGE_BUY_W     = 32;
        public static final int BADGE_BUY_H     = 16;
        public static final int BADGE_COUNT_W   = 64;
        public static final int BADGE_COUNT_H   = 12;

        // icons/
        public static final int ICON_W          = 16;
        public static final int ICON_H          = 16;
    }

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/" + path + ".png");
    }

    private GuiTextures() {}
}
