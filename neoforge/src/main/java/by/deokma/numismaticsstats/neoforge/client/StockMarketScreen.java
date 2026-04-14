package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.market.MarketData;
import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.RequestMarketPacket;
import by.deokma.numismaticsstats.neoforge.network.RequestShopListPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Container screen with two tabs: Shops and Market.
 * Tab 0 = Shops (delegates to ShopListScreen rendering logic inline)
 * Tab 1 = Market (delegates to MarketMonitorScreen)
 */
public class StockMarketScreen extends Screen {

    // ── Persist last active tab across opens ──────────────────────────────────
    private static int lastTab = 0; // 0 = Shops, 1 = Market

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int TAB_H  = 20;
    private static final int MARGIN = 14;
    private static final int MIN_W  = 560;
    private static final int MAX_W  = 800;

    private int panelW() { return Mth.clamp(width  - MARGIN * 2, MIN_W, MAX_W); }
    private int panelH() { return Mth.clamp(height - MARGIN * 2, 220,   380);   }
    private int panelX() { return (width  - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2; }

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_PANEL  = 0xEE0D1117;
    private static final int C_HEADER = 0xFF111827;
    private static final int C_ACCENT = 0xFFD4AF37;
    private static final int C_TEXT   = 0xFFDDDDDD;
    private static final int C_GOLD   = 0xFFD4AF37;

    // ── State ─────────────────────────────────────────────────────────────────
    private int activeTab;
    private ShopListScreen shopScreen;
    private MarketMonitorScreen marketScreen;

    public StockMarketScreen() {
        super(Component.translatable("screen.numismaticsstats.stock_market"));
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        activeTab = lastTab;

        int px = panelX(), py = panelY();
        int contentY = py + TAB_H + 2;
        int contentH = panelH() - TAB_H - 2;

        // Shops sub-screen — create fresh and let it init itself
        shopScreen = new ShopListScreen();
        shopScreen.init(minecraft, width, height);

        // Market sub-screen — pass bounds directly (no protected method needed)
        marketScreen = new MarketMonitorScreen();
        marketScreen.init(px, contentY, panelW(), contentH);

        // Request data for active tab
        if (activeTab == 0) {
            NetworkHandler.sendToServer(new RequestShopListPacket());
        } else {
            MarketData.setLoading(true);
            NetworkHandler.sendToServer(new RequestMarketPacket());
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Transparent — world stays visible
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        int px = panelX(), py = panelY();

        drawPanel(gfx, px, py);
        drawTabs(gfx, px, py, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, delta); // registered widgets

        if (activeTab == 0) {
            shopScreen.render(gfx, mouseX, mouseY, delta);
        } else {
            marketScreen.render(gfx, mouseX, mouseY, delta);
        }
    }

    private void drawPanel(GuiGraphics gfx, int px, int py) {
        gfx.fill(px + 3, py + 3, px + panelW() + 3, py + panelH() + 3, 0x66000000);
        gfx.fill(px, py, px + panelW(), py + panelH(), C_PANEL);
        gfx.fill(px,              py,              px + panelW(), py + 1,            C_ACCENT);
        gfx.fill(px,              py + panelH()-1, px + panelW(), py + panelH(),     C_ACCENT);
        gfx.fill(px,              py,              px + 1,        py + panelH(),     C_ACCENT);
        gfx.fill(px + panelW()-1, py,              px + panelW(), py + panelH(),     C_ACCENT);
    }

    private void drawTabs(GuiGraphics gfx, int px, int py, int mx, int my) {
        gfx.fill(px + 1, py + 1, px + panelW() - 1, py + TAB_H, C_HEADER);
        gfx.fill(px + 1, py + TAB_H - 1, px + panelW() - 1, py + TAB_H, C_ACCENT);

        String[] labels = { "Shops", "Market" };
        int tx = px + 8;
        for (int i = 0; i < labels.length; i++) {
            int tw = font.width(labels[i]) + 10;
            boolean active  = (i == activeTab);
            boolean hovered = mx >= tx && mx < tx + tw && my >= py + 2 && my < py + TAB_H - 2;

            int bg = active  ? 0xFFD4AF37 : hovered ? 0x60D4AF37 : 0x30FFFFFF;
            int fg = active  ? 0xFF111111 : C_TEXT;

            gfx.fill(tx, py + 2, tx + tw, py + TAB_H - 2, bg);
            gfx.drawString(font, labels[i], tx + 5, py + (TAB_H - 8) / 2, fg);
            tx += tw + 4;
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = panelX(), py = panelY();

        // Tab click
        String[] labels = { "Shops", "Market" };
        int tx = px + 8;
        for (int i = 0; i < labels.length; i++) {
            int tw = font.width(labels[i]) + 10;
            if (mx >= tx && mx < tx + tw && my >= py + 2 && my < py + TAB_H - 2) {
                switchTab(i);
                return true;
            }
            tx += tw + 4;
        }

        if (activeTab == 0 && shopScreen != null) {
            if (shopScreen.mouseClicked(mx, my, button)) return true;
        }
        if (activeTab == 1 && marketScreen.mouseClicked(mx, my, button)) return true;

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (activeTab == 0 && shopScreen != null) {
            return shopScreen.mouseScrolled(mx, my, dx, dy);
        }
        if (activeTab == 1) return marketScreen.mouseScrolled(mx, my, dx, dy);
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (activeTab == 0 && shopScreen != null && shopScreen.keyPressed(key, scan, mods)) return true;
        if (activeTab == 1 && marketScreen.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (activeTab == 1 && marketScreen.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Called by MarketPacket when new market data arrives. */
    public void refreshEntries() {
        marketScreen.refreshEntries();
    }

    /** Called by ShopListPacket when new shop data arrives. */
    public void refreshShopEntries() {
        if (shopScreen != null) shopScreen.refreshEntries();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void switchTab(int tab) {
        if (tab == activeTab) return;
        activeTab = tab;
        lastTab   = tab;
        if (tab == 1 && MarketData.get().isEmpty()) {
            MarketData.setLoading(true);
            NetworkHandler.sendToServer(new RequestMarketPacket());
        }
    }
}
