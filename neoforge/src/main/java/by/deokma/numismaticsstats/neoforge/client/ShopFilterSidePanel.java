package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.shop.ShopEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sidebar filter panel for the Shops tab.
 * Sections: Mode, Type, Currency, Items, Owner, Dimension.
 */
public class ShopFilterSidePanel {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xEE060D14;
    private static final int C_BORDER    = 0xFF1A2535;
    private static final int C_SEP       = 0x40D4AF37;
    private static final int C_TEXT      = 0xFFCCCCCC;
    private static final int C_DIM       = 0xFF778899;
    private static final int C_GOLD      = 0xFFD4AF37;
    private static final int C_ACTIVE    = 0xFFD4AF37;
    private static final int C_ACTIVE_BG = 0x30D4AF37;
    private static final int C_HOV       = 0x20FFFFFF;
    private static final int C_HDR_BG    = 0xFF0D1B2A;
    private static final int C_GREEN     = 0xFF4CAF50;
    private static final int C_RED       = 0xFFEF5350;

    private static final int ENTRY_H  = 18;
    private static final int ICON_H   = 18; // entries with item icons
    private static final int HDR_H    = 16;
    private static final int PADDING  = 4;
    private static final int GAP      = 2;

    // ── Currency / Item entry ─────────────────────────────────────────────────
    record IconEntry(String key, String label, ItemStack icon, int count) {}

    // ── Filter state ──────────────────────────────────────────────────────────
    private final Set<String> activeModes      = new LinkedHashSet<>();
    private final Set<String> activeTypes      = new LinkedHashSet<>();
    private final Set<String> activeCurrencies = new LinkedHashSet<>(); // "coins" or item registry key
    private final Set<String> activeItems      = new LinkedHashSet<>(); // item registry key
    private final Set<String> activeOwners     = new LinkedHashSet<>();
    private final Set<String> activeDims       = new LinkedHashSet<>();

    // Available values
    private Map<String, Integer> modeCounts  = new LinkedHashMap<>();
    private Map<String, Integer> typeCounts  = new LinkedHashMap<>();
    private List<IconEntry>      currencies  = new ArrayList<>();
    private List<IconEntry>      items       = new ArrayList<>();
    private List<String>         owners      = new ArrayList<>();
    private Map<String, Integer> ownerCounts = new LinkedHashMap<>();
    private List<String>         dims        = new ArrayList<>();
    private Map<String, Integer> dimCounts   = new LinkedHashMap<>();

    // Collapse state — Currency and Items collapsed by default
    private boolean modeCollapsed     = false;
    private boolean typeCollapsed     = false;
    private boolean currencyCollapsed = true;
    private boolean itemsCollapsed    = true;
    private boolean ownerCollapsed    = false;
    private boolean dimCollapsed      = true;

    private int currencyScroll = 0;
    private int itemsScroll    = 0;
    private int ownerScroll    = 0;

    private int x, y, w, h;
    private Font font;

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(int px, int py, int pw, int ph) {
        this.x = px; this.y = py; this.w = pw; this.h = ph;
        this.font = Minecraft.getInstance().font;
    }

    public void rebuild(List<ShopEntry> all) {
        modeCounts.clear(); typeCounts.clear(); ownerCounts.clear(); dimCounts.clear();
        Map<String, IconEntry> currMap = new LinkedHashMap<>();
        Map<String, IconEntry> itemMap = new LinkedHashMap<>();

        for (ShopEntry e : all) {
            modeCounts.merge(e.mode(), 1, Integer::sum);
            typeCounts.merge(e.shopType(), 1, Integer::sum);
            ownerCounts.merge(e.ownerName(), 1, Integer::sum);
            String dim = e.dimensionId().replace("minecraft:", "");
            dimCounts.merge(dim, 1, Integer::sum);

            // Currency
            String currKey;
            String currLabel;
            ItemStack currIcon;
            if (e.isTableCloth() && !e.priceItem().isEmpty()) {
                currKey   = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(e.priceItem().getItem()).toString();
                currLabel = e.priceItem().getHoverName().getString();
                currIcon  = e.priceItem().copyWithCount(1);
            } else if (!e.isTableCloth() && e.totalPriceInSpurs() > 0) {
                // Numismatics Vendor — show the dominant coin denomination
                currKey   = dominantCoinKey(e.totalPriceInSpurs());
                currLabel = dominantCoinLabel(e.totalPriceInSpurs());
                currIcon  = coinIcon(currKey);
            } else {
                // Free / no price
                currKey   = "free";
                currLabel = "Free";
                currIcon  = ItemStack.EMPTY;
            }
            currMap.merge(currKey,
                    new IconEntry(currKey, currLabel, currIcon, 1),
                    (a, b) -> new IconEntry(a.key(), a.label(), a.icon(), a.count() + 1));

            // Items (what's being sold)
            String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(e.sellingItem().getItem()).toString();
            String itemLabel = e.sellingItem().getHoverName().getString();
            ItemStack itemIcon = e.sellingItem().copyWithCount(1);
            itemMap.merge(itemKey,
                    new IconEntry(itemKey, itemLabel, itemIcon, 1),
                    (a, b) -> new IconEntry(a.key(), a.label(), a.icon(), a.count() + 1));
        }

        currencies = currMap.values().stream()
                .sorted(Comparator.comparingInt(IconEntry::count).reversed())
                .collect(Collectors.toList());
        items = itemMap.values().stream()
                .sorted(Comparator.comparingInt(IconEntry::count).reversed())
                .collect(Collectors.toList());
        owners = ownerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        dims = dimCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).collect(Collectors.toList());

        // Prune stale active filters
        activeOwners.retainAll(ownerCounts.keySet());
        activeModes.retainAll(modeCounts.keySet());
        activeTypes.retainAll(typeCounts.keySet());
        Set<String> currKeys = currencies.stream().map(IconEntry::key).collect(Collectors.toSet());
        activeCurrencies.retainAll(currKeys);
        Set<String> itemKeys = items.stream().map(IconEntry::key).collect(Collectors.toSet());
        activeItems.retainAll(itemKeys);
    }

    // ── Filter API ────────────────────────────────────────────────────────────

    public boolean matches(ShopEntry e) {
        if (!activeModes.isEmpty()  && !activeModes.contains(e.mode()))      return false;
        if (!activeTypes.isEmpty()  && !activeTypes.contains(e.shopType()))  return false;
        if (!activeOwners.isEmpty() && !activeOwners.contains(e.ownerName())) return false;
        if (!activeDims.isEmpty()) {
            if (!activeDims.contains(e.dimensionId().replace("minecraft:", ""))) return false;
        }
        if (!activeCurrencies.isEmpty()) {
            String currKey;
            if (e.isTableCloth() && !e.priceItem().isEmpty()) {
                currKey = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(e.priceItem().getItem()).toString();
            } else if (!e.isTableCloth() && e.totalPriceInSpurs() > 0) {
                currKey = dominantCoinKey(e.totalPriceInSpurs());
            } else {
                currKey = "free";
            }
            if (!activeCurrencies.contains(currKey)) return false;
        }
        if (!activeItems.isEmpty()) {
            String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(e.sellingItem().getItem()).toString();
            if (!activeItems.contains(itemKey)) return false;
        }
        return true;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, int mx, int my) {
        gfx.fill(x, y, x + w, y + h, C_BG);
        gfx.fill(x + w - 1, y, x + w, y + h, C_BORDER);

        int cy = y + 2;

        cy = drawSimpleSection(gfx, mx, my, cy, "Mode",
                List.of("SELL", "BUY"), modeCounts, activeModes, modeCollapsed,
                k -> k.equals("SELL") ? C_RED : C_GREEN);
        cy += GAP;

        cy = drawSimpleSection(gfx, mx, my, cy, "Type",
                List.of("VENDOR", "TABLECLOTH"), typeCounts, activeTypes, typeCollapsed,
                k -> k.equals("VENDOR") ? 0xFF90CAF9 : 0xFF4CAF50);
        cy += GAP;

        cy = drawIconSection(gfx, mx, my, cy, "Currency",
                currencies, activeCurrencies, currencyCollapsed, currencyScroll);
        cy += GAP;

        cy = drawIconSection(gfx, mx, my, cy, "Items",
                items, activeItems, itemsCollapsed, itemsScroll);
        cy += GAP;

        cy = drawScrollableSection(gfx, mx, my, cy, "Owner",
                owners, ownerCounts, activeOwners, ownerCollapsed, ownerScroll);
        cy += GAP;

        drawSimpleSection(gfx, mx, my, cy, "Dimension",
                dims, dimCounts, activeDims, dimCollapsed, k -> C_DIM);
    }

    // ── Section renderers ─────────────────────────────────────────────────────

    private int drawSimpleSection(GuiGraphics gfx, int mx, int my, int startY,
                                   String title, List<String> keys,
                                   Map<String, Integer> counts, Set<String> active,
                                   boolean collapsed,
                                   java.util.function.Function<String, Integer> colorFn) {
        startY = drawSectionHeader(gfx, mx, my, startY, title, active.size(), collapsed);
        if (collapsed) return startY;
        for (String key : keys) {
            int count = counts.getOrDefault(key, 0);
            if (count == 0) continue;
            if (startY + ENTRY_H > y + h) break;
            boolean isActive = active.contains(key);
            boolean hov = hit(mx, my, startY, ENTRY_H);
            drawRowBg(gfx, startY, ENTRY_H, isActive, hov);
            String label = truncate(key, w - PADDING * 2 - font.width("99") - 4);
            gfx.drawString(font, label, x + PADDING, startY + (ENTRY_H - 8) / 2,
                    isActive ? C_ACTIVE : colorFn.apply(key));
            drawCount(gfx, count, startY, ENTRY_H);
            startY += ENTRY_H;
        }
        return startY;
    }

    /** Section with item icons (Currency / Items). */
    private int drawIconSection(GuiGraphics gfx, int mx, int my, int startY,
                                 String title, List<IconEntry> entries,
                                 Set<String> active, boolean collapsed, int scroll) {
        startY = drawSectionHeader(gfx, mx, my, startY, title, active.size(), collapsed);
        if (collapsed) return startY;

        int remaining  = (y + h) - startY;
        int maxVisible = Math.max(1, remaining / ICON_H - 1);
        int maxScroll  = Math.max(0, entries.size() - maxVisible);
        scroll = Math.min(scroll, maxScroll);

        for (int i = scroll; i < Math.min(entries.size(), scroll + maxVisible); i++) {
            IconEntry fe = entries.get(i);
            if (startY + ICON_H > y + h) break;
            boolean isActive = active.contains(fe.key());
            boolean hov = hit(mx, my, startY, ICON_H);
            drawRowBg(gfx, startY, ICON_H, isActive, hov);

            int iconX = x + PADDING;
            if (!fe.icon().isEmpty()) {
                gfx.renderItem(fe.icon(), iconX, startY + (ICON_H - 16) / 2);
                iconX += 18;
            } else {
                // Coins symbol
                gfx.drawString(font, "©", iconX + 2, startY + (ICON_H - 8) / 2, C_GOLD);
                iconX += 14;
            }

            int maxLabelW = w - (iconX - x) - PADDING - font.width("99") - 4;
            String label = truncate(fe.label(), maxLabelW);
            gfx.drawString(font, label, iconX, startY + (ICON_H - 8) / 2,
                    isActive ? C_ACTIVE : C_TEXT);
            drawCount(gfx, fe.count(), startY, ICON_H);
            startY += ICON_H;
        }
        return startY;
    }

    private int drawScrollableSection(GuiGraphics gfx, int mx, int my, int startY,
                                       String title, List<String> keys,
                                       Map<String, Integer> counts, Set<String> active,
                                       boolean collapsed, int scroll) {
        startY = drawSectionHeader(gfx, mx, my, startY, title, active.size(), collapsed);
        if (collapsed) return startY;
        int remaining  = (y + h) - startY;
        int maxVisible = Math.max(2, remaining / ENTRY_H - 1);
        int maxScroll  = Math.max(0, keys.size() - maxVisible);
        scroll = Math.min(scroll, maxScroll);
        for (int i = scroll; i < Math.min(keys.size(), scroll + maxVisible); i++) {
            String key = keys.get(i);
            if (startY + ENTRY_H > y + h) break;
            boolean isActive = active.contains(key);
            boolean hov = hit(mx, my, startY, ENTRY_H);
            drawRowBg(gfx, startY, ENTRY_H, isActive, hov);
            String label = truncate(key, w - PADDING * 2 - font.width("99") - 4);
            gfx.drawString(font, label, x + PADDING, startY + (ENTRY_H - 8) / 2,
                    isActive ? C_ACTIVE : C_TEXT);
            drawCount(gfx, counts.getOrDefault(key, 0), startY, ENTRY_H);
            startY += ENTRY_H;
        }
        return startY;
    }

    private int drawSectionHeader(GuiGraphics gfx, int mx, int my, int startY,
                                   String title, int activeCount, boolean collapsed) {
        if (startY + HDR_H > y + h) return startY;
        gfx.fill(x, startY, x + w, startY + HDR_H, C_HDR_BG);
        gfx.fill(x, startY + HDR_H - 1, x + w, startY + HDR_H, C_SEP);
        gfx.drawString(font, (collapsed ? "▶ " : "▼ ") + title,
                x + PADDING, startY + (HDR_H - 8) / 2, C_GOLD);
        if (activeCount > 0) {
            String badge = String.valueOf(activeCount);
            int bw = font.width(badge) + 6;
            int bx = x + w - bw - PADDING;
            gfx.fill(bx, startY + 2, bx + bw, startY + HDR_H - 2, 0x80D4AF37);
            gfx.drawString(font, badge, bx + 3, startY + (HDR_H - 8) / 2, 0xFF111111);
        }
        return startY + HDR_H;
    }

    private void drawRowBg(GuiGraphics gfx, int startY, int rowH, boolean isActive, boolean hov) {
        if (isActive) { gfx.fill(x, startY, x + w, startY + rowH, C_ACTIVE_BG);
                        gfx.fill(x, startY, x + 2, startY + rowH, C_ACTIVE); }
        else if (hov)   gfx.fill(x, startY, x + w, startY + rowH, C_HOV);
    }

    private void drawCount(GuiGraphics gfx, int count, int startY, int rowH) {
        String cnt = String.valueOf(count);
        gfx.drawString(font, cnt, x + w - PADDING - font.width(cnt),
                startY + (rowH - 8) / 2, C_DIM);
    }

    private String truncate(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        return font.plainSubstrByWidth(s, maxW - 4) + "…";
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        int cy = y + 2;

        // Mode
        if (hitHeader(mx, my, cy)) { modeCollapsed = !modeCollapsed; return true; }
        cy += HDR_H;
        if (!modeCollapsed) {
            for (String key : List.of("SELL", "BUY")) {
                if (modeCounts.getOrDefault(key, 0) == 0) continue;
                if (hit(mx, my, cy, ENTRY_H)) { toggle(activeModes, key); return true; }
                cy += ENTRY_H;
            }
        }
        cy += GAP;

        // Type
        if (hitHeader(mx, my, cy)) { typeCollapsed = !typeCollapsed; return true; }
        cy += HDR_H;
        if (!typeCollapsed) {
            for (String key : List.of("VENDOR", "TABLECLOTH")) {
                if (typeCounts.getOrDefault(key, 0) == 0) continue;
                if (hit(mx, my, cy, ENTRY_H)) { toggle(activeTypes, key); return true; }
                cy += ENTRY_H;
            }
        }
        cy += GAP;

        // Currency
        if (hitHeader(mx, my, cy)) { currencyCollapsed = !currencyCollapsed; return true; }
        cy += HDR_H;
        if (!currencyCollapsed) {
            int remaining  = (y + h) - cy;
            int maxVisible = Math.max(1, remaining / ICON_H - 1);
            int maxScroll  = Math.max(0, currencies.size() - maxVisible);
            currencyScroll = Math.min(currencyScroll, maxScroll);
            for (int i = currencyScroll; i < Math.min(currencies.size(), currencyScroll + maxVisible); i++) {
                if (hit(mx, my, cy, ICON_H)) { toggle(activeCurrencies, currencies.get(i).key()); return true; }
                cy += ICON_H;
            }
        }
        cy += GAP;

        // Items
        if (hitHeader(mx, my, cy)) { itemsCollapsed = !itemsCollapsed; return true; }
        cy += HDR_H;
        if (!itemsCollapsed) {
            int remaining  = (y + h) - cy;
            int maxVisible = Math.max(1, remaining / ICON_H - 1);
            int maxScroll  = Math.max(0, items.size() - maxVisible);
            itemsScroll = Math.min(itemsScroll, maxScroll);
            for (int i = itemsScroll; i < Math.min(items.size(), itemsScroll + maxVisible); i++) {
                if (hit(mx, my, cy, ICON_H)) { toggle(activeItems, items.get(i).key()); return true; }
                cy += ICON_H;
            }
        }
        cy += GAP;

        // Owner
        if (hitHeader(mx, my, cy)) { ownerCollapsed = !ownerCollapsed; return true; }
        cy += HDR_H;
        if (!ownerCollapsed) {
            int remaining  = (y + h) - cy;
            int maxVisible = Math.max(2, remaining / ENTRY_H - 1);
            int maxScroll  = Math.max(0, owners.size() - maxVisible);
            ownerScroll = Math.min(ownerScroll, maxScroll);
            for (int i = ownerScroll; i < Math.min(owners.size(), ownerScroll + maxVisible); i++) {
                if (hit(mx, my, cy, ENTRY_H)) { toggle(activeOwners, owners.get(i)); return true; }
                cy += ENTRY_H;
            }
        }
        cy += GAP;

        // Dimension
        if (hitHeader(mx, my, cy)) { dimCollapsed = !dimCollapsed; return true; }
        cy += HDR_H;
        if (!dimCollapsed) {
            for (String key : dims) {
                if (cy + ENTRY_H > y + h) break;
                if (hit(mx, my, cy, ENTRY_H)) { toggle(activeDims, key); return true; }
                cy += ENTRY_H;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx < x || mx >= x + w) return false;
        int dir = -(int) Math.signum(dy);
        // Scroll whichever expanded section the mouse is over
        int cy = y + 2 + HDR_H; // skip Mode header
        if (!modeCollapsed) cy += modeCounts.values().stream().mapToInt(v -> v > 0 ? ENTRY_H : 0).sum();
        cy += GAP + HDR_H;
        if (!typeCollapsed) cy += typeCounts.values().stream().mapToInt(v -> v > 0 ? ENTRY_H : 0).sum();
        cy += GAP;
        // Currency section
        int currStart = cy;
        cy += HDR_H;
        if (!currencyCollapsed) {
            int maxV = Math.max(1, ((y + h) - cy) / ICON_H - 1);
            if (my >= currStart && my < cy + maxV * ICON_H) {
                currencyScroll = Math.max(0, Math.min(currencyScroll + dir,
                        Math.max(0, currencies.size() - maxV)));
                return true;
            }
            cy += maxV * ICON_H;
        }
        cy += GAP;
        // Items section
        int itemStart = cy;
        cy += HDR_H;
        if (!itemsCollapsed) {
            int maxV = Math.max(1, ((y + h) - cy) / ICON_H - 1);
            if (my >= itemStart && my < cy + maxV * ICON_H) {
                itemsScroll = Math.max(0, Math.min(itemsScroll + dir,
                        Math.max(0, items.size() - maxV)));
                return true;
            }
            cy += maxV * ICON_H;
        }
        cy += GAP;
        // Owner section — default scroll target
        ownerScroll = Math.max(0, ownerScroll + dir);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hitHeader(double mx, double my, int cy) {
        return mx >= x && mx < x + w && my >= cy && my < cy + HDR_H;
    }

    private boolean hit(double mx, double my, int cy, int rowH) {
        return mx >= x && mx < x + w && my >= cy && my < cy + rowH;
    }

    private void toggle(Set<String> set, String key) {
        if (set.contains(key)) set.remove(key); else set.add(key);
    }

    public void clearFilters() {
        activeModes.clear(); activeTypes.clear(); activeCurrencies.clear();
        activeItems.clear(); activeOwners.clear(); activeDims.clear();
    }

    // ── Numismatics coin helpers ───────────────────────────────────────────────
    // Coin values in Spurs: Spur=1, Bevel=8, Sprocket=64, Cog=512, Crown=4096, Sun=32768
    private static final int[] COIN_VALUES = { 32768, 4096, 512, 64, 8, 1 };
    private static final String[] COIN_KEYS = {
        "numismatics:sun", "numismatics:crown", "numismatics:cog",
        "numismatics:sprocket", "numismatics:bevel", "numismatics:spur"
    };
    private static final String[] COIN_LABELS = {
        "Sun", "Crown", "Cog", "Sprocket", "Bevel", "Spur"
    };

    /** Returns the key of the highest denomination coin in the price. */
    private static String dominantCoinKey(int spurs) {
        for (int i = 0; i < COIN_VALUES.length; i++) {
            if (spurs >= COIN_VALUES[i]) return COIN_KEYS[i];
        }
        return "numismatics:spur";
    }

    private static String dominantCoinLabel(int spurs) {
        for (int i = 0; i < COIN_VALUES.length; i++) {
            if (spurs >= COIN_VALUES[i]) return COIN_LABELS[i];
        }
        return "Spur";
    }

    /** Tries to get the ItemStack for a Numismatics coin by registry key. */
    private static ItemStack coinIcon(String key) {
        try {
            var loc = net.minecraft.resources.ResourceLocation.parse(key);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}
