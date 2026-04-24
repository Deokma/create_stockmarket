package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.market.TradeStatsData;
import by.deokma.numismaticsstats.shop.ShopListData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Top Sellers leaderboard panel.
 * Shows players ranked by number of sales (from TradeStatsData),
 * with fallback to shop count if no sales data is available yet.
 */
public class TopSellersScreen {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_BG       = 0xEE060D14;
    private static final int C_ACCENT   = 0xFFD4AF37;
    private static final int C_SEP      = 0x50D4AF37;
    private static final int C_TEXT     = 0xFFDDDDDD;
    private static final int C_DIM      = 0xFF778899;
    private static final int C_GOLD_1   = 0xFFFFD700;
    private static final int C_SILVER   = 0xFFC0C0C0;
    private static final int C_BRONZE   = 0xFFCD7F32;
    private static final int C_ROW_ODD  = 0x14FFFFFF;
    private static final int C_ROW_HOV  = 0x28D4AF37;
    private static final int C_GREEN    = 0xFF4CAF50;

    private static final int ROW_H   = 28;
    private static final int PADDING = 16;
    private static final int HDR_H   = 32;
    private static final int FOOTER_H = 20;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Map.Entry<String, Long>> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private int x, y, w, h;
    private Font font;
    private boolean hasRealData = false;

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(int px, int py, int pw, int ph) {
        this.x = px; this.y = py; this.w = pw; this.h = ph;
        this.font = Minecraft.getInstance().font;
        refresh();
    }

    public void refresh() {
        List<Map.Entry<String, Long>> real = TradeStatsData.getTopSellers();
        if (!real.isEmpty()) {
            entries = new ArrayList<>(real);
            hasRealData = true;
        } else {
            // Fallback: count shops per owner
            Map<String, Long> counts = new LinkedHashMap<>();
            for (var e : ShopListData.get()) {
                counts.merge(e.ownerName(), 1L, Long::sum);
            }
            entries = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());
            hasRealData = false;
        }
        scrollOffset = 0;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        // Background
        gfx.fill(x, y, x + w, y + h, C_BG);

        // Header
        drawHeader(gfx);

        int listTop = y + HDR_H;
        int listBot = y + h - FOOTER_H;
        int listH   = listBot - listTop;
        int rowsVis = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, entries.size() - rowsVis);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        if (entries.isEmpty()) {
            gfx.drawCenteredString(font, "§7No data yet — sales will appear here",
                    x + w / 2, listTop + listH / 2, C_DIM);
        } else {
            drawRows(gfx, listTop, rowsVis, mx, my);
            drawScrollBar(gfx, listTop, listH, rowsVis);
        }

        drawFooter(gfx, rowsVis);
    }

    private void drawHeader(GuiGraphics gfx) {
        gfx.fill(x, y, x + w, y + HDR_H, 0xFF0D1B2A);
        gfx.fill(x, y + HDR_H - 1, x + w, y + HDR_H, C_SEP);

        // Title
        String title = hasRealData ? "🏆  Top Sellers  —  by sales" : "🏪  Top Shops  —  by listing count";
        gfx.drawCenteredString(font, "§6" + title, x + w / 2, y + (HDR_H - 8) / 2, C_GOLD_1);

        // Column headers
        int colY = y + HDR_H - 14;
        gfx.drawString(font, "§7#", x + PADDING, colY, C_DIM);
        gfx.drawString(font, "§7Player", x + PADDING + 30, colY, C_DIM);
        String colLabel = hasRealData ? "§7Sales" : "§7Shops";
        gfx.drawString(font, colLabel, x + w - PADDING - font.width(colLabel.replace("§7", "")), colY, C_DIM);
    }

    private void drawRows(GuiGraphics gfx, int listTop, int rowsVis, int mx, int my) {
        int usableW = w - PADDING * 2 - 6;

        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + rowsVis); i++) {
            var entry = entries.get(i);
            int rank = i + 1;
            int ry = listTop + (i - scrollOffset) * ROW_H;
            int rx = x + PADDING;

            boolean hov = mx >= rx && mx < rx + usableW && my >= ry && my < ry + ROW_H;
            int rowBg = hov ? C_ROW_HOV : (i % 2 == 0 ? C_ROW_ODD : 0);
            if (rowBg != 0) gfx.fill(rx, ry, rx + usableW, ry + ROW_H, rowBg);

            int ty = ry + (ROW_H - 8) / 2;

            // Rank medal / number
            String rankStr;
            int rankColor;
            switch (rank) {
                case 1 -> { rankStr = "🥇"; rankColor = C_GOLD_1; }
                case 2 -> { rankStr = "🥈"; rankColor = C_SILVER; }
                case 3 -> { rankStr = "🥉"; rankColor = C_BRONZE; }
                default -> { rankStr = rank + "."; rankColor = C_DIM; }
            }
            gfx.drawString(font, rankStr, rx, ty, rankColor);

            // Left accent stripe for top 3
            if (rank <= 3) {
                gfx.fill(rx - PADDING, ry, rx - PADDING + 3, ry + ROW_H, rankColor);
            }

            // Player name
            String name = entry.getKey();
            int nameColor = switch (rank) {
                case 1 -> C_GOLD_1;
                case 2 -> C_SILVER;
                case 3 -> C_BRONZE;
                default -> C_TEXT;
            };
            int maxNameW = w - PADDING * 2 - 80;
            if (font.width(name) > maxNameW) name = font.plainSubstrByWidth(name, maxNameW - 4) + "…";
            gfx.drawString(font, name, rx + 30, ty, nameColor);

            // Count (right-aligned)
            String countStr = String.format("%,d", entry.getValue());
            String unit = hasRealData ? " sales" : " shops";
            String full = countStr + unit;
            int countColor = rank == 1 ? C_GOLD_1 : C_GREEN;
            gfx.drawString(font, full, x + w - PADDING - font.width(full) - 6, ty, countColor);

            // Bar chart background (visual indicator)
            if (!entries.isEmpty()) {
                long max = entries.get(0).getValue();
                if (max > 0) {
                    float ratio = (float) entry.getValue() / max;
                    int barMaxW = w / 3;
                    int barW = Math.max(2, (int)(ratio * barMaxW));
                    int barX = x + w - PADDING - font.width(full) - 6 - barW - 4;
                    int barAlpha = rank <= 3 ? 0x60 : 0x30;
                    int barColor = (barAlpha << 24) | (rankColor & 0x00FFFFFF);
                    gfx.fill(barX, ry + 4, barX + barW, ry + ROW_H - 4, barColor);
                }
            }
        }
    }

    private void drawScrollBar(GuiGraphics gfx, int listTop, int listH, int rowsVis) {
        if (entries.size() <= rowsVis) return;
        int bx = x + w - 5;
        int thumbH = Math.max(16, listH * rowsVis / entries.size());
        int maxS   = entries.size() - rowsVis;
        int thumbY = listTop + (scrollOffset * (listH - thumbH)) / Math.max(1, maxS);
        gfx.fill(bx, listTop, bx + 4, listTop + listH, 0x25FFFFFF);
        gfx.fill(bx, thumbY,  bx + 4, thumbY + thumbH, 0xAAD4AF37);
    }

    private void drawFooter(GuiGraphics gfx, int rowsVis) {
        int fy = y + h - FOOTER_H;
        gfx.fill(x, fy, x + w, fy + 1, C_SEP);
        gfx.fill(x, fy + 1, x + w, y + h, 0xEE060D14);

        String info = entries.size() + " players";
        gfx.drawString(font, info, x + PADDING, fy + (FOOTER_H - 8) / 2, C_DIM);

        String note = hasRealData ? "§aLive sales data" : "§7Fallback: shop count (no sales yet)";
        int nw = font.width(note.replaceAll("§.", ""));
        gfx.drawString(font, note, x + w - PADDING - nw, fy + (FOOTER_H - 8) / 2, C_DIM);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(dy));
        return true;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }
}
