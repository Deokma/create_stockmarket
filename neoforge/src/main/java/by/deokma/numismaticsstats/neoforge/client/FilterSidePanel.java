package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.market.MarketEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Sidebar filter panel for the Market tab.
 * Two collapsible sections: Currency (payment) and Items (selling).
 * Each entry shows an item icon, truncated name, and lot count.
 */
public class FilterSidePanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xEE060D14;
    private static final int C_BORDER    = 0xFF1A2535;
    private static final int C_ACCENT    = 0xFFD4AF37;
    private static final int C_SEP       = 0x40D4AF37;
    private static final int C_TEXT      = 0xFFCCCCCC;
    private static final int C_DIM       = 0xFF778899;
    private static final int C_GOLD      = 0xFFD4AF37;
    private static final int C_ACTIVE    = 0xFFD4AF37;
    private static final int C_ACTIVE_BG = 0x30D4AF37;
    private static final int C_HOV       = 0x20FFFFFF;
    private static final int C_HDR_BG    = 0xFF0D1B2A;

    private static final int ENTRY_H  = 20;
    private static final int HDR_H    = 16;
    private static final int PADDING  = 4;

    // ── State ─────────────────────────────────────────────────────────────────
    public record FilterEntry(ResourceLocation itemId, ItemStack icon, String label, int count) {}

    private List<FilterEntry> currencyEntries = new ArrayList<>();
    private List<FilterEntry> itemEntries     = new ArrayList<>();

    /** Active filters — null means "all" */
    private final Set<ResourceLocation> activeCurrencies = new LinkedHashSet<>();
    private final Set<ResourceLocation> activeItems      = new LinkedHashSet<>();

    private boolean currencyCollapsed = false;
    private boolean itemsCollapsed    = false;

    private int currencyScroll = 0;
    private int itemsScroll    = 0;

    private int x, y, w, h;
    private Font font;

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(int px, int py, int pw, int ph) {
        this.x = px; this.y = py; this.w = pw; this.h = ph;
        this.font = Minecraft.getInstance().font;
    }

    public void rebuild(List<MarketEntry> all) {
        // Use String keys internally to support both ResourceLocation and synthetic keys like coin names
        Map<String, FilterEntry> currMap = new LinkedHashMap<>();
        Map<ResourceLocation, FilterEntry> itemMap = new LinkedHashMap<>();

        for (MarketEntry e : all) {
            // Items section — what's being sold
            ResourceLocation itemId = e.itemId();
            itemMap.merge(itemId,
                    new FilterEntry(itemId, e.displayStack().copyWithCount(1),
                            e.displayStack().getHoverName().getString(), e.sellCount()),
                    (a, b) -> new FilterEntry(a.itemId(), a.icon(), a.label(), a.count() + b.count()));

            // Currency section
            if (e.isBarterOnly() && !e.barterItem().isEmpty()) {
                String payKey = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(e.barterItem().getItem()).toString();
                ResourceLocation payId = ResourceLocation.parse(payKey);
                FilterEntry fe = new FilterEntry(payId, e.barterItem().copyWithCount(1),
                        e.barterItem().getHoverName().getString(), e.sellCount());
                currMap.merge(payKey, fe,
                        (a, b) -> new FilterEntry(a.itemId(), a.icon(), a.label(), a.count() + b.count()));
            } else if (!e.isBarterOnly() && e.avgPrice() > 0) {
                // Numismatics Vendor — show dominant coin denomination (e.g. "Bevel")
                String coinKey   = dominantCoinKey(e.avgPrice());
                String coinLabel = dominantCoinLabel(e.avgPrice());
                ItemStack coinIc = coinIcon(coinKey);
                ResourceLocation coinId = ResourceLocation.parse(coinKey);
                FilterEntry fe = new FilterEntry(coinId, coinIc, coinLabel, e.sellCount());
                currMap.merge(coinKey, fe,
                        (a, b) -> new FilterEntry(a.itemId(), a.icon(), a.label(), a.count() + b.count()));
            }
        }

        currencyEntries = new ArrayList<>(currMap.values());
        currencyEntries.sort(Comparator.comparingInt(FilterEntry::count).reversed());

        itemEntries = new ArrayList<>(itemMap.values());
        itemEntries.sort(Comparator.comparingInt(FilterEntry::count).reversed());

        // Prune stale active filters
        Set<ResourceLocation> validCurr = currencyEntries.stream()
                .map(FilterEntry::itemId).collect(java.util.stream.Collectors.toSet());
        activeCurrencies.retainAll(validCurr);
        Set<ResourceLocation> validItems = itemEntries.stream()
                .map(FilterEntry::itemId).collect(java.util.stream.Collectors.toSet());
        activeItems.retainAll(validItems);
    }

    // ── Filter API ────────────────────────────────────────────────────────────

    public boolean hasCurrencyFilter() { return !activeCurrencies.isEmpty(); }
    public boolean hasItemFilter()     { return !activeItems.isEmpty(); }

    public boolean matchesCurrency(MarketEntry e) {
        if (activeCurrencies.isEmpty()) return true;
        if (e.isBarterOnly() && !e.barterItem().isEmpty()) {
            ResourceLocation payId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(e.barterItem().getItem());
            return activeCurrencies.contains(payId);
        }
        // Vendor — match by dominant coin key
        if (!e.isBarterOnly() && e.avgPrice() > 0) {
            ResourceLocation coinId = ResourceLocation.parse(dominantCoinKey(e.avgPrice()));
            return activeCurrencies.contains(coinId);
        }
        return false;
    }

    public boolean matchesItem(MarketEntry e) {
        if (activeItems.isEmpty()) return true;
        return activeItems.contains(e.itemId());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, int mx, int my) {
        // Background
        gfx.fill(x, y, x + w, y + h, C_BG);
        gfx.fill(x + w - 1, y, x + w, y + h, C_BORDER);

        int cy = y + 2;

        // ── Currency section ──────────────────────────────────────────────────
        cy = drawSection(gfx, mx, my, cy, "Currency", currencyEntries,
                activeCurrencies, currencyCollapsed, currencyScroll);

        cy += 4; // gap between sections

        // ── Items section ─────────────────────────────────────────────────────
        drawSection(gfx, mx, my, cy, "Items", itemEntries,
                activeItems, itemsCollapsed, itemsScroll);
    }

    private int drawSection(GuiGraphics gfx, int mx, int my, int startY,
                             String title, List<FilterEntry> entries,
                             Set<ResourceLocation> active, boolean collapsed, int scroll) {
        // Section header
        gfx.fill(x, startY, x + w, startY + HDR_H, C_HDR_BG);
        gfx.fill(x, startY + HDR_H - 1, x + w, startY + HDR_H, C_SEP);

        String arrow = collapsed ? "▶" : "▼";
        gfx.drawString(font, arrow + " " + title, x + PADDING, startY + (HDR_H - 8) / 2, C_GOLD);

        // Active count badge
        if (!active.isEmpty()) {
            String badge = String.valueOf(active.size());
            int bw = font.width(badge) + 6;
            int bx = x + w - bw - PADDING;
            gfx.fill(bx, startY + 2, bx + bw, startY + HDR_H - 2, 0x80D4AF37);
            gfx.drawString(font, badge, bx + 3, startY + (HDR_H - 8) / 2, 0xFF111111);
        }

        int cy = startY + HDR_H;
        if (collapsed) return cy;

        // Available height for entries
        int maxH = (h - (cy - y)) / 2 - 4; // split remaining height roughly in half
        int maxVisible = Math.max(1, maxH / ENTRY_H);
        int maxScroll  = Math.max(0, entries.size() - maxVisible);
        scroll = Math.min(scroll, maxScroll);

        for (int i = scroll; i < Math.min(entries.size(), scroll + maxVisible); i++) {
            FilterEntry fe = entries.get(i);
            boolean isActive = active.contains(fe.itemId());
            boolean hov = mx >= x && mx < x + w && my >= cy && my < cy + ENTRY_H;

            int rowBg = isActive ? C_ACTIVE_BG : hov ? C_HOV : 0;
            if (rowBg != 0) gfx.fill(x, cy, x + w, cy + ENTRY_H, rowBg);
            if (isActive) gfx.fill(x, cy, x + 2, cy + ENTRY_H, C_ACTIVE);

            // Icon
            if (!fe.icon().isEmpty()) {
                gfx.renderItem(fe.icon(), x + PADDING, cy + (ENTRY_H - 16) / 2);
            } else {
                // Coins — draw "C" symbol
                gfx.drawString(font, "©", x + PADDING + 4, cy + (ENTRY_H - 8) / 2, C_GOLD);
            }

            // Label (truncated)
            String label = fe.label();
            int maxLabelW = w - PADDING * 2 - 20 - font.width("99") - 4;
            if (font.width(label) > maxLabelW)
                label = font.plainSubstrByWidth(label, maxLabelW - 4) + "…";
            gfx.drawString(font, label, x + PADDING + 18, cy + (ENTRY_H - 8) / 2,
                    isActive ? C_ACTIVE : C_TEXT);

            // Count badge (right-aligned)
            String cnt = String.valueOf(fe.count());
            gfx.drawString(font, cnt, x + w - PADDING - font.width(cnt),
                    cy + (ENTRY_H - 8) / 2, C_DIM);

            cy += ENTRY_H;
        }

        // Scroll indicators
        if (scroll > 0)
            gfx.drawCenteredString(font, "▲", x + w / 2, cy - maxVisible * ENTRY_H - 8, C_DIM);
        if (scroll < maxScroll)
            gfx.drawCenteredString(font, "▼", x + w / 2, cy + 1, C_DIM);

        return cy;
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        int cy = y + 2;
        // Currency header
        if (mx >= x && mx < x + w && my >= cy && my < cy + HDR_H) {
            currencyCollapsed = !currencyCollapsed;
            return true;
        }
        cy += HDR_H;
        if (!currencyCollapsed) {
            int maxVisible = visibleCount(cy);
            for (int i = currencyScroll; i < Math.min(currencyEntries.size(), currencyScroll + maxVisible); i++) {
                if (mx >= x && mx < x + w && my >= cy && my < cy + ENTRY_H) {
                    toggleFilter(activeCurrencies, currencyEntries.get(i).itemId());
                    return true;
                }
                cy += ENTRY_H;
            }
            cy += 4;
        } else cy += 4;

        // Items header
        if (mx >= x && mx < x + w && my >= cy && my < cy + HDR_H) {
            itemsCollapsed = !itemsCollapsed;
            return true;
        }
        cy += HDR_H;
        if (!itemsCollapsed) {
            int maxVisible = visibleCount(cy);
            for (int i = itemsScroll; i < Math.min(itemEntries.size(), itemsScroll + maxVisible); i++) {
                if (mx >= x && mx < x + w && my >= cy && my < cy + ENTRY_H) {
                    toggleFilter(activeItems, itemEntries.get(i).itemId());
                    return true;
                }
                cy += ENTRY_H;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx < x || mx >= x + w) return false;
        int dir = -(int) Math.signum(dy);
        int cy = y + 2 + HDR_H;
        int midY = cy + (h / 2);
        if (my < midY) {
            currencyScroll = Math.max(0, Math.min(currencyScroll + dir,
                    Math.max(0, currencyEntries.size() - visibleCount(cy))));
        } else {
            itemsScroll = Math.max(0, Math.min(itemsScroll + dir,
                    Math.max(0, itemEntries.size() - visibleCount(cy))));
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int visibleCount(int fromY) {
        int remaining = (y + h) - fromY;
        return Math.max(1, remaining / ENTRY_H / 2);
    }

    private void toggleFilter(Set<ResourceLocation> set, ResourceLocation id) {
        if (set.contains(id)) set.remove(id);
        else set.add(id);
    }

    public void clearFilters() {
        activeCurrencies.clear();
        activeItems.clear();
    }

    // ── Numismatics coin helpers ───────────────────────────────────────────────
    private static final int[] COIN_VALUES = { 32768, 4096, 512, 64, 8, 1 };
    private static final String[] COIN_KEYS = {
        "numismatics:sun", "numismatics:crown", "numismatics:cog",
        "numismatics:sprocket", "numismatics:bevel", "numismatics:spur"
    };
    private static final String[] COIN_LABELS = {
        "Sun", "Crown", "Cog", "Sprocket", "Bevel", "Spur"
    };

    static String dominantCoinKey(int spurs) {
        for (int i = 0; i < COIN_VALUES.length; i++) {
            if (spurs >= COIN_VALUES[i]) return COIN_KEYS[i];
        }
        return "numismatics:spur";
    }

    static String dominantCoinLabel(int spurs) {
        for (int i = 0; i < COIN_VALUES.length; i++) {
            if (spurs >= COIN_VALUES[i]) return COIN_LABELS[i];
        }
        return "Spur";
    }

    static ItemStack coinIcon(String key) {
        try {
            var loc  = ResourceLocation.parse(key);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
            if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}
