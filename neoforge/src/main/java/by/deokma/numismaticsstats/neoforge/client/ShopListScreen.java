package by.deokma.numismaticsstats.neoforge.client;

import by.deokma.numismaticsstats.neoforge.network.NetworkHandler;
import by.deokma.numismaticsstats.neoforge.network.RequestShopListPacket;
import by.deokma.numismaticsstats.shop.ShopEntry;
import by.deokma.numismaticsstats.shop.ShopListData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.stream.Collectors;

public class ShopListScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    // HEADER_H matches StockMarketScreen.TAB_H so content starts right below the tab bar
    private static final int HEADER_H  = 20;
    private static final int TOOLBAR_H = 26;
    private static final int COL_HDR_H = 16;
    private static final int ROW_H     = 24;
    private static final int FOOTER_H  = 22;
    private static final int PADDING   = 10;
    // Match StockMarketScreen dimensions so the table fills the full panel width
    private static final int MIN_W     = 560;
    private static final int MAX_W     = 800;
    private static final int MARGIN    = 14;
    private static final int SIDEBAR_W = 110;

    private int panelW() { return Mth.clamp(width  - MARGIN * 2, MIN_W, MAX_W); }
    private int panelH() { return Mth.clamp(height - MARGIN * 2, 220,   360);   }
    private int panelX() { return (width  - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2; }

    // Table area (right of sidebar)
    private int tableX() { return panelX() + SIDEBAR_W; }
    private int tableW() { return panelW() - SIDEBAR_W; }

    // Columns — sized to fill the wider table area (panelW - SIDEBAR_W - PADDING*2 - scrollbar)
    private static final int COL_FAV   = 16;
    private static final int COL_ITEM  = 200;
    private static final int COL_PRICE = 90;
    private static final int COL_OWNER = 110;
    private static final int COL_MODE  = 48;
    private static final int COL_DIM   = 90;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_PANEL   = 0xEE0D1117;
    private static final int C_HEADER  = 0xFF111827;
    private static final int C_TOOLBAR = 0xFF0D1B2A;
    private static final int C_ACCENT  = 0xFFD4AF37;
    private static final int C_SEP     = 0x50D4AF37;
    private static final int C_ROW_ODD = 0x14FFFFFF;
    private static final int C_ROW_HOV = 0x28D4AF37;
    private static final int C_ROW_FAV = 0x18FFD700;
    private static final int C_TEXT    = 0xFFDDDDDD;
    private static final int C_DIM     = 0xFF778899;
    private static final int C_GOLD    = 0xFFD4AF37;
    private static final int C_GREEN   = 0xFF4CAF50;
    private static final int C_RED     = 0xFFEF5350;
    private static final int C_BLUE    = 0xFF90CAF9;
    private static final int C_STAR_ON = 0xFFFFD700;
    private static final int C_STAR_OFF= 0xFF444455;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<ShopEntry> displayed = new ArrayList<>();
    private int  scrollOffset = 0;
    private EditBox searchBox;
    private boolean loading = true;

    // Sorting: 0=item 1=price 2=owner 3=mode 4=dim
    private int     sortCol = 0;
    private boolean sortAsc = true;

    // Favourites — persisted in memory for the session
    private static final Set<String> favourites = new LinkedHashSet<>();

    // Filter tabs: "all" | "fav" | <playerName>
    private String  activeTab   = "all";
    private List<String> playerTabs = new ArrayList<>(); // unique owners

    // Sidebar filter panel
    private ShopFilterSidePanel sidebar;

    public ShopListScreen() {
        super(Component.translatable("screen.numismaticsstats.shop_list"));
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        int px = panelX(), py = panelY();
        int tx = tableX();
        int tw = tableW();

        // Sidebar
        sidebar = new ShopFilterSidePanel();
        sidebar.init(px, py + HEADER_H, SIDEBAR_W, panelH() - HEADER_H - FOOTER_H);

        // Search box — positioned in table area
        searchBox = new EditBox(font,
                tx + PADDING, py + HEADER_H + 4,
                tw - PADDING * 2 - 48, 18,
                Component.empty());
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setTextColor(C_TEXT);
        searchBox.setResponder(s -> { scrollOffset = 0; applyFilter(); });
        addRenderableWidget(searchBox);

        // Refresh
        addRenderableWidget(Button.builder(Component.literal("↺"), btn -> requestData())
                .bounds(tx + tw - PADDING - 44, py + HEADER_H + 3, 18, 20).build());
        // Close
        addRenderableWidget(Button.builder(Component.literal("✕"), btn -> onClose())
                .bounds(tx + tw - PADDING - 22, py + HEADER_H + 3, 18, 20).build());

        requestData();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void requestData() {
        loading = true;
        displayed.clear();
        NetworkHandler.sendToServer(new RequestShopListPacket());
    }

    public void refreshEntries() {
        loading = false;
        if (sidebar != null) sidebar.rebuild(ShopListData.get());
        // Rebuild player tabs
        playerTabs = ShopListData.get().stream()
                .map(ShopEntry::ownerName)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
        applyFilter();
    }

    private static String favKey(ShopEntry e) {
        return e.dimensionId() + "|" + e.pos().toShortString();
    }

    private void applyFilter() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        displayed = ShopListData.get().stream()
                .filter(e -> {
                    // Sidebar filter
                    if (sidebar != null && !sidebar.matches(e)) return false;
                    // Tab filter (favourites / owner)
                    if ("fav".equals(activeTab) && !favourites.contains(favKey(e))) return false;
                    if (!"all".equals(activeTab) && !"fav".equals(activeTab)
                            && !e.ownerName().equals(activeTab)) return false;
                    // Search filter
                    return q.isEmpty()
                            || e.sellingItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                            || e.ownerName().toLowerCase(Locale.ROOT).contains(q);
                })
                .sorted((a, b) -> {
                    // Favourites always on top
                    boolean af = favourites.contains(favKey(a));
                    boolean bf = favourites.contains(favKey(b));
                    if (af != bf) return af ? -1 : 1;
                    int cmp = switch (sortCol) {
                        case 1 -> {
                            // Free (0) always at the bottom regardless of sort direction
                            int pa = a.totalPriceInSpurs(), pb = b.totalPriceInSpurs();
                            if (pa == 0 && pb == 0) yield 0;
                            if (pa == 0) yield 1;   // a is Free → goes to bottom
                            if (pb == 0) yield -1;  // b is Free → goes to bottom
                            yield Integer.compare(pa, pb);
                        }
                        case 2 -> a.ownerName().compareToIgnoreCase(b.ownerName());
                        case 3 -> a.mode().compareTo(b.mode());
                        case 4 -> a.dimensionId().compareTo(b.dimensionId());
                        default -> a.sellingItem().getHoverName().getString()
                                    .compareToIgnoreCase(b.sellingItem().getHoverName().getString());
                    };
                    return sortAsc ? cmp : -cmp;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // No blur — transparent, world stays visible
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        int px = panelX(), py = panelY();
        int tx = tableX(), tw = tableW();

        // When embedded in StockMarketScreen, panel/header are drawn by the parent.
        // We only draw our own content area (toolbar downward).
        drawToolbar(gfx, tx, tw, py);
        drawPlayerTabs(gfx, tx, tw, py, mouseX, mouseY);

        // Sidebar
        if (sidebar != null) sidebar.render(gfx, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, delta); // widgets on top

        int tabsH   = playerTabs.isEmpty() ? 0 : 18;
        int listTop = py + HEADER_H + TOOLBAR_H + tabsH + COL_HDR_H + 2;
        int listBot = py + panelH() - FOOTER_H;
        int listH   = listBot - listTop;
        int rowsVis = Math.max(1, listH / ROW_H);

        drawColumnHeaders(gfx, tx, tw, py + HEADER_H + TOOLBAR_H + tabsH, mouseX, mouseY);

        int maxScroll = Math.max(0, displayed.size() - rowsVis);
        scrollOffset  = Mth.clamp(scrollOffset, 0, maxScroll);

        if (loading) {
            gfx.drawCenteredString(font, "Loading...", tx + tw / 2, listTop + listH / 2, C_DIM);
        } else if (displayed.isEmpty()) {
            gfx.drawCenteredString(font, "No shops found", tx + tw / 2, listTop + listH / 2, C_DIM);
        } else {
            drawRows(gfx, tx, tw, listTop, rowsVis, mouseX, mouseY);
            drawScrollBar(gfx, tx, tw, listTop, listH, rowsVis);
        }

        drawFooter(gfx, px, py, tx, tw, rowsVis);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawPanel(GuiGraphics gfx, int px, int py) {
        // Drop shadow
        gfx.fill(px + 3, py + 3, px + panelW() + 3, py + panelH() + 3, 0x66000000);
        // Body
        gfx.fill(px, py, px + panelW(), py + panelH(), C_PANEL);
        // Gold border
        gfx.fill(px,              py,              px + panelW(), py + 1,            C_ACCENT);
        gfx.fill(px,              py + panelH()-1, px + panelW(), py + panelH(),     C_ACCENT);
        gfx.fill(px,              py,              px + 1,        py + panelH(),     C_ACCENT);
        gfx.fill(px + panelW()-1, py,              px + panelW(), py + panelH(),     C_ACCENT);
    }

    private void drawHeader(GuiGraphics gfx, int px, int py, int tx, int tw) {
        gfx.fill(px + 1, py + 1, px + panelW() - 1, py + HEADER_H, C_HEADER);
        gfx.fill(px + 1, py + HEADER_H - 1, px + panelW() - 1, py + HEADER_H, C_ACCENT);

        Component title = Component.literal("● ").withStyle(s -> s.withColor(C_GOLD))
                .append(Component.literal("Shop Market").withStyle(s -> s.withColor(C_TEXT)));
        int tw2 = font.width("● Shop Market");
        gfx.drawString(font, title, px + panelW() / 2 - tw2 / 2, py + (HEADER_H - 8) / 2, C_TEXT);

        if (!loading) {
            String badge = displayed.size() + " shops";
            int bw = font.width(badge) + 8;
            int bx = tx + tw - PADDING - bw;
            int by = py + 10;
            gfx.fill(bx, by, bx + bw, by + 12, 0x80D4AF37);
            gfx.drawString(font, badge, bx + 4, by + 2, 0xFF111111);
        }
    }

    private void drawToolbar(GuiGraphics gfx, int tx, int tw, int py) {
        int ty = py + HEADER_H;
        gfx.fill(tx, ty, tx + tw, ty + TOOLBAR_H, C_TOOLBAR);
        int bx = tx + PADDING - 2;
        int by = ty + 3;
        int bw = tw - PADDING * 2 - 52;
        gfx.fill(bx, by, bx + bw, by + 20, 0x40FFFFFF);
        gfx.fill(bx + 1, by + 1, bx + bw - 1, by + 19, 0xFF0A0F14);
    }

    private void drawPlayerTabs(GuiGraphics gfx, int tx, int tw, int py, int mouseX, int mouseY) {
        if (playerTabs.isEmpty()) return;
        int ty = py + HEADER_H + TOOLBAR_H;
        gfx.fill(tx, ty, tx + tw, ty + 18, 0xFF0A1020);

        List<String> tabs = new ArrayList<>();
        tabs.add("all"); tabs.add("fav"); tabs.addAll(playerTabs);

        int tabX = tx + PADDING;
        for (String tab : tabs) {
            String label = switch (tab) {
                case "all" -> "All"; case "fav" -> "★ Fav"; default -> tab;
            };
            int tabW = font.width(label) + 8;
            boolean active  = tab.equals(activeTab);
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabW
                    && mouseY >= ty + 2 && mouseY < ty + 16;
            int bg = active ? 0xFFD4AF37 : hovered ? 0x60D4AF37 : 0x30FFFFFF;
            int fg = active ? 0xFF111111 : "fav".equals(tab) ? C_STAR_ON : C_TEXT;
            gfx.fill(tabX, ty + 2, tabX + tabW, ty + 16, bg);
            gfx.drawString(font, label, tabX + 4, ty + 4, fg);
            tabX += tabW + 3;
            if (tabX > tx + tw - PADDING) break;
        }
    }

    private void drawColumnHeaders(GuiGraphics gfx, int tx, int tw, int top, int mouseX, int mouseY) {
        gfx.fill(tx, top, tx + tw, top + COL_HDR_H, 0x40D4AF37);
        int[] widths  = { COL_FAV, COL_ITEM, COL_PRICE, COL_OWNER, COL_MODE, COL_DIM };
        String[] names = { "", "Item", "Price", "Owner", "Mode", "Dim" };
        int[] sortIdx  = { -1, 0, 1, 2, 3, 4 };
        int cx = tx + PADDING;
        for (int i = 0; i < names.length; i++) {
            if (names[i].isEmpty()) { cx += widths[i]; continue; }
            String label = names[i];
            if (sortIdx[i] == sortCol) label += sortAsc ? " ▲" : " ▼";
            boolean hov = mouseX >= cx && mouseX < cx + widths[i]
                    && mouseY >= top && mouseY < top + COL_HDR_H;
            gfx.drawString(font, label, cx + 2, top + 4, hov ? C_GOLD : 0xFFCCBB44);
            cx += widths[i];
        }
        gfx.fill(tx + PADDING, top + COL_HDR_H - 1, tx + tw - PADDING, top + COL_HDR_H, C_SEP);
    }

    private void drawRows(GuiGraphics gfx, int tx, int tw, int listTop, int rowsVis, int mouseX, int mouseY) {
        int scrollBarW = 5;
        int usableW = tw - PADDING * 2 - scrollBarW - 2;

        for (int i = scrollOffset; i < Math.min(displayed.size(), scrollOffset + rowsVis); i++) {
            ShopEntry e   = displayed.get(i);
            boolean  fav  = favourites.contains(favKey(e));
            int ry = listTop + (i - scrollOffset) * ROW_H;
            int rx = tx + PADDING;

            boolean hov = mouseX >= rx && mouseX < rx + usableW
                    && mouseY >= ry && mouseY < ry + ROW_H;

            // Row bg
            int rowBg = hov ? C_ROW_HOV : fav ? C_ROW_FAV : (i % 2 == 0 ? C_ROW_ODD : 0);
            if (rowBg != 0) gfx.fill(rx, ry, rx + usableW, ry + ROW_H, rowBg);

            int cx = rx;
            int ty = ry + (ROW_H - 8) / 2;

            // ★ Favourite star
            gfx.drawString(font, "★", cx + 2, ty, fav ? C_STAR_ON : C_STAR_OFF);
            cx += COL_FAV;

            // Item
            gfx.renderItem(e.sellingItem(), cx, ry + (ROW_H - 16) / 2);
            String tag  = e.isTableCloth() ? "§8[TC] §r" : "§8[V] §r";
            String name = e.sellingItem().getHoverName().getString();
            int maxW = COL_ITEM - 22;
            if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - 4) + "…";
            gfx.drawString(font, tag + name, cx + 18, ty, C_TEXT);
            cx += COL_ITEM;

            // Price
            if (e.isTableCloth() && !e.priceItem().isEmpty()) {
                gfx.renderItem(e.priceItem(), cx, ry + (ROW_H - 16) / 2);
                if (e.priceItem().getCount() > 1)
                    gfx.drawString(font, "x" + e.priceItem().getCount(), cx + 18, ty, C_GOLD);
            } else {
                gfx.drawString(font, formatPrice(e.totalPriceInSpurs()), cx + 2, ty, C_GOLD);
            }
            cx += COL_PRICE;

            // Owner
            String owner = e.ownerName();
            if (font.width(owner) > COL_OWNER - 4)
                owner = font.plainSubstrByWidth(owner, COL_OWNER - 8) + "…";
            gfx.drawString(font, owner, cx + 2, ty, C_BLUE);
            cx += COL_OWNER;

            // Mode badge
            boolean sell = e.mode().equals("SELL");
            int badgeBg  = sell ? 0x80EF5350 : 0x804CAF50;
            int badgeFg  = sell ? C_RED : C_GREEN;
            int bw = font.width(e.mode()) + 6;
            gfx.fill(cx + 2, ry + 4, cx + 2 + bw, ry + ROW_H - 4, badgeBg);
            gfx.drawString(font, e.mode(), cx + 5, ty, badgeFg);
            cx += COL_MODE;

            // Dim
            String dim = e.dimensionId().replace("minecraft:", "");
            if (font.width(dim) > COL_DIM - 4)
                dim = font.plainSubstrByWidth(dim, COL_DIM - 8) + "…";
            gfx.drawString(font, dim, cx + 2, ty, C_DIM);

            // Tooltip
            if (hov) {
                List<Component> tt = new ArrayList<>();
                tt.add(e.sellingItem().getHoverName().copy().withStyle(s -> s.withColor(C_GOLD)));
                if (e.isTableCloth()) {
                    String ps = e.priceItem().isEmpty() ? "?"
                            : e.priceItem().getHoverName().getString()
                              + (e.priceItem().getCount() > 1 ? " x" + e.priceItem().getCount() : "");
                    tt.add(Component.literal("§7Price: §f" + ps));
                    tt.add(Component.literal("§8Create TableCloth Shop"));
                } else {
                    tt.add(Component.literal("§7Price: §f" + formatPrice(e.totalPriceInSpurs())));
                    tt.add(Component.literal("§8Numismatics Vendor"));
                }
                tt.add(Component.literal("§7Owner: §b" + e.ownerName()));
                tt.add(Component.literal("§7Mode: " + (sell ? "§cSELL" : "§aBUY")));
                tt.add(Component.literal("§7Pos: §f" + e.pos().toShortString()));
                tt.add(Component.literal("§7Dim: §8" + e.dimensionId()));
                if (fav) tt.add(Component.literal("§e★ Favourited"));
                gfx.renderTooltip(font,
                        tt.stream().map(Component::getVisualOrderText).collect(Collectors.toList()),
                        mouseX, mouseY);
            }
        }
    }

    private void drawScrollBar(GuiGraphics gfx, int tx, int tw, int listTop, int listH, int rowsVis) {
        if (displayed.size() <= rowsVis) return;
        int bx = tx + tw - PADDING - 4;
        int thumbH = Math.max(16, listH * rowsVis / displayed.size());
        int maxS   = displayed.size() - rowsVis;
        int thumbY = listTop + (scrollOffset * (listH - thumbH)) / Math.max(1, maxS);
        gfx.fill(bx, listTop, bx + 4, listTop + listH, 0x25FFFFFF);
        gfx.fill(bx, thumbY,  bx + 4, thumbY + thumbH, 0xAAD4AF37);
    }

    private void drawFooter(GuiGraphics gfx, int px, int py, int tx, int tw, int rowsVis) {
        int fy = py + panelH() - FOOTER_H;
        gfx.fill(px + 1, fy, px + panelW() - 1, fy + 1, C_SEP);
        gfx.fill(px + 1, fy + 1, px + panelW() - 1, py + panelH() - 1, 0xEE060D14);

        if (!displayed.isEmpty()) {
            int end = Math.min(displayed.size(), scrollOffset + rowsVis);
            gfx.drawString(font, (scrollOffset + 1) + "–" + end + " / " + displayed.size(),
                    tx + PADDING, fy + (FOOTER_H - 8) / 2, C_DIM);
        }
        // Compact hint
        String hint = "↕ scroll  col → sort  ★ fav";
        gfx.drawString(font, hint,
                tx + tw / 2 - font.width(hint) / 2,
                fy + (FOOTER_H - 8) / 2, C_DIM);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (sidebar != null && sidebar.mouseScrolled(mx, my, dx, dy)) return true;
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(dy));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = panelX(), py = panelY();
        int tx = tableX(), tw = tableW();

        // Sidebar
        if (sidebar != null && sidebar.mouseClicked(mx, my, button)) {
            applyFilter();
            return true;
        }

        // ── Player tabs ───────────────────────────────────────────────────────
        if (!playerTabs.isEmpty()) {
            int tabY = py + HEADER_H + TOOLBAR_H;
            if (my >= tabY + 2 && my < tabY + 16) {
                List<String> tabs = new ArrayList<>();
                tabs.add("all"); tabs.add("fav"); tabs.addAll(playerTabs);
                int tabX = tx + PADDING;
                for (String tab : tabs) {
                    String label = switch (tab) {
                        case "all" -> "All"; case "fav" -> "★ Fav"; default -> tab;
                    };
                    int tabW = font.width(label) + 8;
                    if (mx >= tabX && mx < tabX + tabW) {
                        activeTab = tab;
                        scrollOffset = 0;
                        applyFilter();
                        return true;
                    }
                    tabX += tabW + 3;
                    if (tabX > tx + tw - PADDING) break;
                }
            }
        }

        // ── Column headers ────────────────────────────────────────────────────
        int tabsH  = playerTabs.isEmpty() ? 0 : 18;
        int colTop = py + HEADER_H + TOOLBAR_H + tabsH;
        if (my >= colTop && my < colTop + COL_HDR_H) {
            int[] widths  = { COL_FAV, COL_ITEM, COL_PRICE, COL_OWNER, COL_MODE, COL_DIM };
            int[] sortIdx = { -1, 0, 1, 2, 3, 4 };
            int cx = tx + PADDING;
            for (int i = 0; i < widths.length; i++) {
                if (mx >= cx && mx < cx + widths[i] && sortIdx[i] >= 0) {
                    if (sortCol == sortIdx[i]) sortAsc = !sortAsc;
                    else { sortCol = sortIdx[i]; sortAsc = true; }
                    applyFilter();
                    return true;
                }
                cx += widths[i];
            }
        }

        // ── Star click (favourite toggle) ─────────────────────────────────────
        int listTop = colTop + COL_HDR_H + 2;
        int listBot = py + panelH() - FOOTER_H;
        int rowsVis = Math.max(1, (listBot - listTop) / ROW_H);
        if (mx >= tx + PADDING && mx < tx + PADDING + COL_FAV
                && my >= listTop && my < listTop + rowsVis * ROW_H) {
            int idx = scrollOffset + (int)((my - listTop) / ROW_H);
            if (idx >= 0 && idx < displayed.size()) {
                String key = favKey(displayed.get(idx));
                if (favourites.contains(key)) favourites.remove(key);
                else favourites.add(key);
                applyFilter();
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (searchBox != null && searchBox.isFocused())
            return searchBox.keyPressed(key, scan, mods);
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
