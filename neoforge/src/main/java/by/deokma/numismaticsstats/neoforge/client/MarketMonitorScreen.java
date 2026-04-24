package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.market.MarketData;
import by.deokma.numismaticsstats.market.MarketEntry;
import by.deokma.numismaticsstats.market.TradeStatsData;
import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.RequestMarketPacket;
import by.deokma.numismaticsstats.shop.ShopListData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stock Exchange panel — two sections:
 *  - Top:    Vendor shops (coin prices, % change, sparkline, volume)
 *  - Bottom: TableCloth barter shops (item payment)
 */
public class MarketMonitorScreen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_TOOLBAR    = 0xFF060D14;
    private static final int C_ACCENT     = 0xFFD4AF37;
    private static final int C_SEP        = 0x50D4AF37;
    private static final int C_ROW_ODD    = 0x14FFFFFF;
    private static final int C_ROW_HOV    = 0x28D4AF37;
    private static final int C_ROW_HOT    = 0x18FFD700; // high-volume "hot" row
    private static final int C_TEXT       = 0xFFDDDDDD;
    private static final int C_DIM        = 0xFF778899;
    private static final int C_GOLD       = 0xFFD4AF37;
    private static final int C_GREEN      = 0xFF4CAF50;
    private static final int C_RED        = 0xFFEF5350;
    private static final int C_BLUE       = 0xFF90CAF9;
    private static final int C_BARTER_HDR = 0xFF0A1A0A;
    private static final int C_SPARK_UP   = 0xFF4CAF50;
    private static final int C_SPARK_DOWN = 0xFFEF5350;
    private static final int C_SPARK_FLAT = 0xFF778899;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int TOOLBAR_H  = 26;
    private static final int COL_HDR_H  = 14;
    private static final int ROW_H      = 24;
    private static final int FOOTER_H   = 36;  // taller to fit top traders row
    private static final int SECTION_H  = 12;
    private static final int PADDING    = 10;

    // Vendor columns: Item | Price | Change% | Vol | Spark | Trend
    private static final int COL_ITEM   = 130;
    private static final int COL_PRICE  = 72;  // avg price (main price)
    private static final int COL_CHANGE = 52;  // % change
    private static final int COL_VOL    = 36;  // volume = sell+buy
    private static final int COL_SPARK  = 48;  // inline sparkline
    private static final int COL_TREND  = 28;  // ▲▼—

    // Barter columns
    private static final int BCOL_ITEM    = 150;
    private static final int BCOL_PAYMENT = 100;
    private static final int BCOL_SELL    = 44;
    private static final int BCOL_BUY     = 44;

    // Sparkline dimensions (inline in row)
    private static final int SPARK_W = 40;
    private static final int SPARK_H = 12;

    // "Hot" threshold — rows with volume >= this get highlighted
    private static final int HOT_VOLUME = 5;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<MarketEntry> vendorRows  = new ArrayList<>();
    private List<MarketEntry> barterRows  = new ArrayList<>();

    private int scrollOffset = 0;
    private EditBox searchBox;
    // sortCol: 0=name 1=price 2=change% 3=volume 4=trend
    private int sortCol = 1;
    private boolean sortAsc = true;
    private long lastUpdateMs = 0;

    /** Top traders from real sales stats: owner name → sales count, sorted desc */
    private List<Map.Entry<String, Long>> topTraders = new ArrayList<>();

    private int x, y, w, h;
    private Font font;

    private int refreshBtnX, refreshBtnY;
    private static final int BTN_W = 18, BTN_H = 20;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 110;
    private FilterSidePanel sidebar;

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(int px, int py, int pw, int ph) {
        this.x = px + SIDEBAR_W; // table starts after sidebar
        this.y = py;
        this.w = pw - SIDEBAR_W;
        this.h = ph;
        this.font = Minecraft.getInstance().font;

        // Sidebar
        sidebar = new FilterSidePanel();
        sidebar.init(px, py, SIDEBAR_W, ph);

        refreshBtnX = this.x + this.w - PADDING - BTN_W - 2;
        refreshBtnY = this.y + TOOLBAR_H / 2 - BTN_H / 2;

        searchBox = new EditBox(font,
                this.x + PADDING, this.y + TOOLBAR_H / 2 - 9,
                this.w - PADDING * 2 - BTN_W - 8, 18,
                Component.empty());
        searchBox.setHint(Component.literal("Search item..."));
        searchBox.setBordered(false);
        searchBox.setTextColor(C_TEXT);
        searchBox.setResponder(s -> { scrollOffset = 0; applyFilter(); });

        refreshEntries();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void refreshEntries() {
        lastUpdateMs = System.currentTimeMillis();
        if (sidebar != null) sidebar.rebuild(MarketData.get());
        rebuildTopTraders();
        applyFilter();
    }

    private void rebuildTopTraders() {
        // Use real sales statistics from the server (TradeStatsData)
        topTraders = TradeStatsData.getTopSellers();
        // Fallback to shop count if no sales data yet
        if (topTraders.isEmpty()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (var e : ShopListData.get()) {
                counts.merge(e.ownerName(), 1L, Long::sum);
            }
            topTraders = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }
    }

    private void applyFilter() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";

        List<MarketEntry> all = MarketData.get().stream()
                .filter(e -> q.isEmpty()
                        || e.displayStack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                        || (!e.barterItem().isEmpty() && e.barterItem().getHoverName().getString()
                                .toLowerCase(Locale.ROOT).contains(q)))
                .filter(e -> sidebar == null || (sidebar.matchesCurrency(e) && sidebar.matchesItem(e)))
                .collect(Collectors.toList());

        vendorRows = all.stream()
                .filter(e -> !e.isBarterOnly())
                .sorted((a, b) -> {
                    int cmp = switch (sortCol) {
                        case 1 -> Integer.compare(b.avgPrice(), a.avgPrice()); // default: highest price first
                        case 2 -> Double.compare(priceChangePct(b), priceChangePct(a));
                        case 3 -> Integer.compare(b.sellCount() + b.buyCount(), a.sellCount() + a.buyCount());
                        case 4 -> a.trend().compareTo(b.trend());
                        default -> a.displayStack().getHoverName().getString()
                                .compareToIgnoreCase(b.displayStack().getHoverName().getString());
                    };
                    return sortAsc ? cmp : -cmp;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        barterRows = all.stream()
                .filter(MarketEntry::isBarterOnly)
                .sorted(Comparator.comparing(e -> e.displayStack().getHoverName().getString(),
                        String::compareToIgnoreCase))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** % change between oldest and newest snapshot. Returns 0 if < 2 snapshots. */
    private static double priceChangePct(MarketEntry e) {
        List<Integer> h = e.priceHistory();
        if (h.size() < 2) return 0.0;
        int first = h.get(0), last = h.get(h.size() - 1);
        if (first == 0) return 0.0;
        return (last - first) * 100.0 / first;
    }

    private static String formatChangePct(double pct) {
        if (Math.abs(pct) < 0.05) return "—";
        return String.format("%+.1f%%", pct);
    }

    /** Total virtual rows including section headers */
    private int totalRows() {
        int total = vendorRows.size();
        if (!barterRows.isEmpty()) total += 1 + barterRows.size(); // +1 for barter section header
        if (!vendorRows.isEmpty() && !barterRows.isEmpty()) total += 1; // separator
        return total;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        // Sidebar
        if (sidebar != null) sidebar.render(gfx, mx, my);

        drawToolbar(gfx, mx, my);
        if (searchBox != null) searchBox.render(gfx, mx, my, delta);
        drawVendorColumnHeaders(gfx, mx, my);

        int listTop = y + TOOLBAR_H + COL_HDR_H + 2;
        int listBot = y + h - FOOTER_H;
        int listH   = listBot - listTop;
        int rowsVis = Math.max(1, listH / ROW_H);

        int maxScroll = Math.max(0, totalRows() - rowsVis);
        scrollOffset  = Mth.clamp(scrollOffset, 0, maxScroll);

        if (MarketData.isLoading()) {
            gfx.drawCenteredString(font, "Loading...", x + w / 2, listTop + listH / 2, C_DIM);
        } else if (vendorRows.isEmpty() && barterRows.isEmpty()) {
            gfx.drawCenteredString(font, "No market data", x + w / 2, listTop + listH / 2, C_DIM);
        } else {
            drawAllRows(gfx, listTop, rowsVis, mx, my);
            drawScrollBar(gfx, listTop, listH, rowsVis);
        }

        drawFooter(gfx, rowsVis);
    }

    private void drawToolbar(GuiGraphics gfx, int mx, int my) {
        gfx.fill(x, y, x + w, y + TOOLBAR_H, C_TOOLBAR);
        // Search box bg
        int bx = x + PADDING - 2, by = y + TOOLBAR_H / 2 - 11;
        int bw = w - PADDING * 2 - BTN_W - 12;
        gfx.fill(bx, by, bx + bw, by + 20, 0x40FFFFFF);
        gfx.fill(bx + 1, by + 1, bx + bw - 1, by + 19, 0xFF060D14);

        // Refresh button
        boolean hov = mx >= refreshBtnX && mx < refreshBtnX + BTN_W
                && my >= refreshBtnY && my < refreshBtnY + BTN_H;
        gfx.fill(refreshBtnX, refreshBtnY, refreshBtnX + BTN_W, refreshBtnY + BTN_H,
                hov ? 0x80D4AF37 : 0x30FFFFFF);
        gfx.drawCenteredString(font, "↺", refreshBtnX + BTN_W / 2, refreshBtnY + (BTN_H - 8) / 2, C_GOLD);

        // Last update timestamp (top-right of toolbar)
        if (lastUpdateMs > 0) {
            long secAgo = (System.currentTimeMillis() - lastUpdateMs) / 1000;
            String ts = secAgo < 60 ? secAgo + "s ago" : (secAgo / 60) + "m ago";
            gfx.drawString(font, ts, refreshBtnX - font.width(ts) - 4,
                    y + (TOOLBAR_H - 8) / 2, C_DIM);
        }
    }

    private void drawVendorColumnHeaders(GuiGraphics gfx, int mx, int my) {
        int top = y + TOOLBAR_H;
        gfx.fill(x, top, x + w, top + COL_HDR_H, 0x60D4AF37);
        // Column headers: Item | Price | Change | Vol | Chart | Trend
        int[] widths = { COL_ITEM, COL_PRICE, COL_CHANGE, COL_VOL, COL_SPARK, COL_TREND };
        String[] names = { "Item", "Price", "Change", "Vol", "Chart", "Trend" };
        int cx = x + PADDING;
        for (int i = 0; i < names.length; i++) {
            String label = names[i] + (i == sortCol ? (sortAsc ? " ▲" : " ▼") : "");
            boolean hov = mx >= cx && mx < cx + widths[i] && my >= top && my < top + COL_HDR_H;
            gfx.drawString(font, label, cx + 2, top + 3, hov ? C_GOLD : 0xFFCCBB44);
            cx += widths[i];
        }
        gfx.fill(x + PADDING, top + COL_HDR_H - 1, x + w - PADDING, top + COL_HDR_H, C_SEP);
    }

    /**
     * Renders all rows in a unified virtual list:
     * [vendor rows...] [separator] [barter header] [barter rows...]
     */
    private void drawAllRows(GuiGraphics gfx, int listTop, int rowsVis, int mx, int my) {
        int scrollBarW = 5;
        int usableW = w - PADDING * 2 - scrollBarW - 2;

        // Build virtual row list
        // Each entry: type 0=vendor, 1=separator, 2=barterHeader, 3=barter
        record VRow(int type, int dataIdx) {}
        List<VRow> vrows = new ArrayList<>();
        for (int i = 0; i < vendorRows.size(); i++) vrows.add(new VRow(0, i));
        if (!barterRows.isEmpty()) {
            if (!vendorRows.isEmpty()) vrows.add(new VRow(1, -1)); // separator
            vrows.add(new VRow(2, -1)); // barter section header
            for (int i = 0; i < barterRows.size(); i++) vrows.add(new VRow(3, i));
        }

        int rendered = 0;
        for (int vi = scrollOffset; vi < vrows.size() && rendered < rowsVis; vi++, rendered++) {
            VRow vr = vrows.get(vi);
            int ry = listTop + rendered * ROW_H;

            switch (vr.type()) {
                case 1 -> { // separator
                    gfx.fill(x + PADDING, ry + ROW_H / 2 - 1, x + w - PADDING, ry + ROW_H / 2, C_SEP);
                }
                case 2 -> { // barter section header
                    gfx.fill(x, ry, x + w, ry + ROW_H, C_BARTER_HDR);
                    // Barter column headers
                    int cx = x + PADDING;
                    gfx.drawString(font, "§7Item", cx + 2, ry + (ROW_H - 8) / 2, 0xFFCCBB44);
                    cx += BCOL_ITEM;
                    gfx.drawString(font, "§7Payment", cx + 2, ry + (ROW_H - 8) / 2, 0xFFCCBB44);
                    cx += BCOL_PAYMENT;
                    gfx.drawString(font, "§7Sell", cx + 2, ry + (ROW_H - 8) / 2, 0xFFCCBB44);
                    cx += BCOL_SELL;
                    gfx.drawString(font, "§7Buy", cx + 2, ry + (ROW_H - 8) / 2, 0xFFCCBB44);
                    // Label on right
                    String label = "⇄ Barter";
                    gfx.drawString(font, label, x + w - PADDING - font.width(label) - scrollBarW - 4,
                            ry + (ROW_H - 8) / 2, 0xFF4CAF50);
                }
                case 0 -> drawVendorRow(gfx, vendorRows.get(vr.dataIdx()), vi, ry, usableW, mx, my);
                case 3 -> drawBarterRow(gfx, barterRows.get(vr.dataIdx()), vi, ry, usableW, mx, my);
            }
        }
    }

    private void drawVendorRow(GuiGraphics gfx, MarketEntry e, int vi, int ry, int usableW, int mx, int my) {
        int rx = x + PADDING;
        boolean hov = mx >= rx && mx < rx + usableW && my >= ry && my < ry + ROW_H;
        int volume = e.sellCount() + e.buyCount();
        boolean hot = volume >= HOT_VOLUME;

        int rowBg = hov ? C_ROW_HOV : hot ? C_ROW_HOT : (vi % 2 == 0 ? C_ROW_ODD : 0);
        if (rowBg != 0) gfx.fill(rx, ry, rx + usableW, ry + ROW_H, rowBg);

        // Hot indicator — left edge stripe
        if (hot) gfx.fill(rx, ry, rx + 2, ry + ROW_H, 0xFFD4AF37);

        int cx = rx, ty = ry + (ROW_H - 8) / 2;

        // ── Item icon + name ──────────────────────────────────────────────────
        gfx.renderItem(e.displayStack(), cx, ry + (ROW_H - 16) / 2);
        String name = e.displayStack().getHoverName().getString();
        if (font.width(name) > COL_ITEM - 22) name = font.plainSubstrByWidth(name, COL_ITEM - 26) + "…";
        gfx.drawString(font, name, cx + 18, ty, C_TEXT);
        cx += COL_ITEM;

        // ── Price (avg) ───────────────────────────────────────────────────────
        gfx.drawString(font, formatPrice(e.avgPrice()), cx + 2, ty, C_GOLD);
        cx += COL_PRICE;

        // ── % Change ─────────────────────────────────────────────────────────
        double pct = priceChangePct(e);
        String pctStr = formatChangePct(pct);
        int pctColor = pct > 0.05 ? C_GREEN : pct < -0.05 ? C_RED : C_DIM;
        gfx.drawString(font, pctStr, cx + 2, ty, pctColor);
        cx += COL_CHANGE;

        // ── Volume ────────────────────────────────────────────────────────────
        gfx.drawString(font, String.valueOf(volume), cx + 2, ty, hot ? C_GOLD : C_DIM);
        cx += COL_VOL;

        // ── Inline sparkline ──────────────────────────────────────────────────
        List<Integer> hist = e.priceHistory();
        if (hist.size() >= 2) {
            int sparkColor = pct > 0.05 ? C_SPARK_UP : pct < -0.05 ? C_SPARK_DOWN : C_SPARK_FLAT;
            drawInlineSparkline(gfx, hist, cx + 2, ry + (ROW_H - SPARK_H) / 2, SPARK_W, SPARK_H, sparkColor);
        }
        cx += COL_SPARK;

        // ── Trend symbol ─────────────────────────────────────────────────────
        String ts; int tc;
        switch (e.trend()) {
            case RISING  -> { ts = "▲"; tc = C_GREEN; }
            case FALLING -> { ts = "▼"; tc = C_RED;   }
            default      -> { ts = "—"; tc = C_DIM;   }
        }
        gfx.drawString(font, ts, cx + 2, ty, tc);

        if (hov) drawVendorTooltip(gfx, e, mx, my);
    }

    /** Draws a small line-chart sparkline directly in the row. */
    private void drawInlineSparkline(GuiGraphics gfx, List<Integer> history,
                                     int sx, int sy, int sw, int sh, int color) {
        float[] norm = normalizeHistory(history);
        if (norm.length < 2) return;
        // Draw as connected line segments (pixel-by-pixel)
        float stepX = (float) sw / (norm.length - 1);
        for (int i = 0; i < norm.length - 1; i++) {
            int x1 = sx + Math.round(i * stepX);
            int y1 = sy + sh - 1 - Math.round(norm[i] * (sh - 1));
            int x2 = sx + Math.round((i + 1) * stepX);
            int y2 = sy + sh - 1 - Math.round(norm[i + 1] * (sh - 1));
            // Draw a 1px thick line between (x1,y1) and (x2,y2)
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            if (steps == 0) { gfx.fill(x1, y1, x1 + 1, y1 + 1, color); continue; }
            for (int s = 0; s <= steps; s++) {
                int px = x1 + (x2 - x1) * s / steps;
                int py = y1 + (y2 - y1) * s / steps;
                gfx.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private void drawBarterRow(GuiGraphics gfx, MarketEntry e, int vi, int ry, int usableW, int mx, int my) {
        int rx = x + PADDING;
        boolean hov = mx >= rx && mx < rx + usableW && my >= ry && my < ry + ROW_H;
        int rowBg = hov ? C_ROW_HOV : (vi % 2 == 0 ? C_ROW_ODD : 0);
        if (rowBg != 0) gfx.fill(rx, ry, rx + usableW, ry + ROW_H, rowBg);

        int cx = rx, ty = ry + (ROW_H - 8) / 2;

        // Selling item
        gfx.renderItem(e.displayStack(), cx, ry + (ROW_H - 16) / 2);
        String name = e.displayStack().getHoverName().getString();
        if (font.width(name) > BCOL_ITEM - 22) name = font.plainSubstrByWidth(name, BCOL_ITEM - 26) + "…";
        gfx.drawString(font, name, cx + 18, ty, C_TEXT);
        cx += BCOL_ITEM;

        // Payment item (icon + name)
        if (!e.barterItem().isEmpty()) {
            gfx.renderItem(e.barterItem(), cx, ry + (ROW_H - 16) / 2);
            String payName = e.barterItem().getHoverName().getString();
            if (e.barterItem().getCount() > 1) payName = "x" + e.barterItem().getCount() + " " + payName;
            int maxPay = BCOL_PAYMENT - 22;
            if (font.width(payName) > maxPay) payName = font.plainSubstrByWidth(payName, maxPay - 4) + "…";
            gfx.drawString(font, payName, cx + 18, ty, C_DIM);
        } else {
            gfx.drawString(font, "?", cx + 2, ty, C_DIM);
        }
        cx += BCOL_PAYMENT;

        gfx.drawString(font, String.valueOf(e.sellCount()), cx + 2, ty, C_GREEN);
        cx += BCOL_SELL;
        gfx.drawString(font, String.valueOf(e.buyCount()), cx + 2, ty, C_BLUE);

        if (hov) drawBarterTooltip(gfx, e, mx, my);
    }

    private void drawVendorTooltip(GuiGraphics gfx, MarketEntry e, int mx, int my) {
        List<Component> tt = new ArrayList<>();
        tt.add(e.displayStack().getHoverName().copy().withStyle(s -> s.withColor(C_GOLD)));

        // Price info
        tt.add(Component.literal("§7Avg Price: §f" + formatPrice(e.avgPrice())));
        tt.add(Component.literal("§7Min Price: §f" + formatPrice(e.minPrice())));

        // % change
        double pct = priceChangePct(e);
        String pctStr = formatChangePct(pct);
        String pctColored = pct > 0.05 ? "§a" + pctStr : pct < -0.05 ? "§c" + pctStr : "§7" + pctStr;
        tt.add(Component.literal("§724h Change: " + pctColored));

        // Volume
        int vol = e.sellCount() + e.buyCount();
        tt.add(Component.literal("§7Volume: §f" + vol
                + " §8(§a" + e.sellCount() + " sell §8/ §b" + e.buyCount() + " buy§8)"));

        // Trend
        String ts = switch (e.trend()) {
            case RISING  -> "§a▲ Rising";
            case FALLING -> "§c▼ Falling";
            default      -> "§7— Stable";
        };
        tt.add(Component.literal("§7Trend: " + ts));

        if (vol >= HOT_VOLUME) tt.add(Component.literal("§6🔥 High activity"));

        gfx.renderTooltip(font, tt.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mx, my);

        // Full-size chart in tooltip
        if (e.priceHistory().size() >= 2) {
            int chartX = Math.min(mx + 4, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 92);
            int chartY = Math.min(my + tt.size() * 10 + 6, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 38);
            drawMiniChart(gfx, e.priceHistory(), chartX, chartY, 88, 34);
        }
    }

    private void drawBarterTooltip(GuiGraphics gfx, MarketEntry e, int mx, int my) {
        List<Component> tt = new ArrayList<>();
        tt.add(e.displayStack().getHoverName().copy().withStyle(s -> s.withColor(C_GOLD)));
        tt.add(Component.literal("§8Create TableCloth Shop"));
        if (!e.barterItem().isEmpty()) {
            String pay = e.barterItem().getHoverName().getString()
                    + (e.barterItem().getCount() > 1 ? " x" + e.barterItem().getCount() : "");
            tt.add(Component.literal("§7Payment: §f" + pay));
        }
        tt.add(Component.literal("§7Sellers: §a" + e.sellCount() + "  §7Buyers: §b" + e.buyCount()));
        gfx.renderTooltip(font, tt.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), mx, my);
    }

    private void drawMiniChart(GuiGraphics gfx, List<Integer> history, int cx, int cy, int cw, int ch) {
        float[] norm = normalizeHistory(history);
        int barW = Math.max(1, cw / norm.length);
        gfx.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xAA000000);
        for (int i = 0; i < norm.length; i++) {
            int barH = Math.max(1, (int)(norm[i] * ch));
            gfx.fill(cx + i * barW, cy + ch - barH, cx + i * barW + barW - 1, cy + ch, C_ACCENT);
        }
    }

    private void drawScrollBar(GuiGraphics gfx, int listTop, int listH, int rowsVis) {
        int total = totalRows();
        if (total <= rowsVis) return;
        int bx = x + w - PADDING - 4;
        int thumbH = Math.max(16, listH * rowsVis / total);
        int maxS   = total - rowsVis;
        int thumbY = listTop + (scrollOffset * (listH - thumbH)) / Math.max(1, maxS);
        gfx.fill(bx, listTop, bx + 4, listTop + listH, 0x25FFFFFF);
        gfx.fill(bx, thumbY,  bx + 4, thumbY + thumbH, 0xAAD4AF37);
    }

    private void drawFooter(GuiGraphics gfx, int rowsVis) {
        int fy = y + h - FOOTER_H;
        gfx.fill(x, fy, x + w, fy + 1, C_SEP);
        gfx.fill(x, fy + 1, x + w, y + h, 0xEE060D14);

        int vCount = vendorRows.size(), bCount = barterRows.size();
        int row1Y = fy + 3;
        int row2Y = fy + FOOTER_H / 2 + 2;

        // Row 1 left: item counts
        String info = "§f" + vCount + "§7 listed  §f" + bCount + "§7 barter";
        gfx.drawString(font, info, x + PADDING, row1Y, C_DIM);

        // Row 1 right: sort hint
        String hint = "↑↓ sort";
        gfx.drawString(font, hint, x + w - PADDING - font.width(hint) - 4, row1Y, C_DIM);

        // Row 2: Top Traders
        if (!topTraders.isEmpty()) {
            boolean hasRealData = !TradeStatsData.getTopSellers().isEmpty();
            String label = hasRealData ? "§6🏆 Top sellers: " : "§7🏪 Most shops: ";
            gfx.drawString(font, label, x + PADDING, row2Y, C_GOLD);
            int tx = x + PADDING + font.width(hasRealData ? "🏆 Top sellers: " : "🏪 Most shops: ") + 2;

            for (int i = 0; i < topTraders.size() && tx < x + w - PADDING; i++) {
                var entry = topTraders.get(i);
                // Medal colour: gold / silver / bronze / rest
                int nameColor = switch (i) {
                    case 0 -> 0xFFFFD700; // gold
                    case 1 -> 0xFFC0C0C0; // silver
                    case 2 -> 0xFFCD7F32; // bronze
                    default -> C_DIM;
                };
                String badge = (i + 1) + ". " + entry.getKey() + " §8(" + entry.getValue() + ")";
                int bw = font.width(badge);
                if (tx + bw > x + w - PADDING) break;
                gfx.drawString(font, entry.getKey(), tx, row2Y, nameColor);
                tx += font.width(entry.getKey());
                gfx.drawString(font, " §8(" + entry.getValue() + ")", tx, row2Y, C_DIM);
                tx += font.width(" (" + entry.getValue() + ")");
                if (i < topTraders.size() - 1) {
                    gfx.drawString(font, "  ", tx, row2Y, C_DIM);
                    tx += font.width("  ");
                }
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        // Sidebar first
        if (sidebar != null && sidebar.mouseClicked(mx, my, button)) {
            applyFilter(); // re-filter when sidebar selection changes
            return true;
        }

        if (mx >= refreshBtnX && mx < refreshBtnX + BTN_W
                && my >= refreshBtnY && my < refreshBtnY + BTN_H) {
            MarketData.setLoading(true);
            NetworkHandler.sendToServer(new RequestMarketPacket());
            return true;
        }

        // Search box — explicitly set focus based on click position
        if (searchBox != null) {
            boolean inBox = mx >= searchBox.getX() && mx < searchBox.getX() + searchBox.getWidth()
                    && my >= searchBox.getY() && my < searchBox.getY() + searchBox.getHeight();
            searchBox.setFocused(inBox);
            if (inBox) { searchBox.mouseClicked(mx, my, button); return true; }
        }

        int colTop = y + TOOLBAR_H;
        if (my >= colTop && my < colTop + COL_HDR_H) {
            int[] widths = { COL_ITEM, COL_PRICE, COL_CHANGE, COL_VOL, COL_SPARK, COL_TREND };
            int cx = x + PADDING;
            for (int i = 0; i < widths.length; i++) {
                if (mx >= cx && mx < cx + widths[i]) {
                    if (i == 4) { cx += widths[i]; continue; } // chart column not sortable
                    if (sortCol == i) sortAsc = !sortAsc;
                    else { sortCol = i; sortAsc = true; }
                    applyFilter();
                    return true;
                }
                cx += widths[i];
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (sidebar != null && sidebar.mouseScrolled(mx, my, dx, dy)) return true;
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(dy));
        return true;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (searchBox != null && searchBox.isFocused()) return searchBox.keyPressed(key, scan, mods);
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (searchBox != null && searchBox.isFocused()) return searchBox.charTyped(c, mods);
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static float[] normalizeHistory(List<Integer> history) {
        if (history.isEmpty()) return new float[0];
        int min = history.stream().mapToInt(i -> i).min().orElse(0);
        int max = history.stream().mapToInt(i -> i).max().orElse(0);
        float[] result = new float[history.size()];
        if (min == max) { Arrays.fill(result, 0.5f); return result; }
        for (int i = 0; i < history.size(); i++)
            result[i] = (float)(history.get(i) - min) / (max - min);
        return result;
    }

    private static String formatPrice(int spurs) {
        if (spurs <= 0) return "Free";
        int c = spurs / 512; spurs %= 512;
        int sp = spurs / 64; spurs %= 64;
        int b = spurs / 8;   spurs %= 8;
        StringBuilder sb = new StringBuilder();
        if (c  > 0) sb.append(c).append("C ");
        if (sp > 0) sb.append(sp).append("Sp ");
        if (b  > 0) sb.append(b).append("B ");
        if (spurs > 0) sb.append(spurs).append("s");
        return sb.toString().trim();
    }
}
