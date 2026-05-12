# NumismaticsStats — Stock Market Monitor

**Minecraft 1.21.1 | NeoForge 21.1.226 | Fabric 0.16.9**

A mod that adds a real-time **server economy monitor** to Minecraft. Place the **Market Terminal** block, right-click it, and get a full overview of every shop on the server — prices, trends, filters, and more.

---

## Dependencies

| Mod | Version | Required |
|-----|---------|----------|
| [Create](https://modrinth.com/mod/create) | 6.0.9 | ✅ Required |
| [Create: Numismatics](https://modrinth.com/mod/numismatics) | 1.0.19+ | ⚠️ Optional |

> Numismatics is optional — the mod works without it, but coin prices and Vendor shops won't appear.

---

## How to get started

Craft the **Market Terminal** block and place it anywhere in the world.

```
G C G
I B I
G C G

G = Gold Ingot
C = Comparator
I = Iron Ingot
B = Iron Block
```

Right-click the block to open the Stock Exchange screen.  
You can also open it with the **M** key (configurable in Controls → NumismaticsStats).

The block faces the direction you're looking when placed, has a custom hitbox (lower platform + upper frame), and drops itself when broken.

---

## Screen overview

The screen has three tabs at the top: **Shops**, **Market**, and **🏆 Top Sellers**.  
The panel is fully responsive — it scales with your GUI size (90% of screen width, 85% of height, clamped to 380–900 × 200–500 px).

---

## Shops tab

A full list of every active shop on the server — both **Numismatics Vendor** and **Create TableCloth** shops.

**Table columns:**
- ★ — favourite toggle (persists per session, click to pin to top)
- Item — icon + name, with `[V]` (Vendor) or `[TC]` (TableCloth) tag
- Price — coin price or barter item icon with count
- Owner — player name
- Mode — `SELL` / `BUY` badge
- Dim — dimension (overworld, nether, end…)

**Features:**
- Search by item name or owner
- Click any column header to sort; click again to reverse
- Favourited shops are always pinned to the top; free (0-price) shops always sort to the bottom
- Player tabs: **All** / **★ Fav** / one tab per unique owner
- Hover over a row for a detailed tooltip (item, price, owner, mode, position, dimension)

**Sidebar filters (6 collapsible sections):**
- **Mode** — SELL / BUY
- **Type** — Vendor / TableCloth
- **Currency** — filter by coin denomination (Spur → Bevel → Sprocket → Cog → Crown → Sun) or barter item
- **Items** — filter by what's being sold
- **Owner** — filter by shop owner
- **Dimension** — filter by world dimension

Each section shows a count badge when filters are active. Sections can be collapsed individually; Currency, Items, and Dimension are collapsed by default.

---

## Market tab

A **stock-exchange-style** view of the server economy, aggregated by item.

**Table columns:**
- Item — icon + name
- Price — average coin price across all Vendor shops
- Change — % price change over recorded history (`+5.2%` green / `-3.1%` red)
- Vol — total volume (sell + buy shop count)
- Chart — inline sparkline showing price trend
- Trend — ▲ Rising / ▼ Falling / — Stable

**Features:**
- Rows with high volume (≥ 5 shops) are highlighted in gold with a left stripe (🔥 High activity)
- Hover over any row for a detailed tooltip:
  - Avg price, min price, 24h change
  - Volume breakdown (X sell / Y buy)
  - Full price history bar chart
- Toolbar shows time since last refresh (`Xs ago`) and a ↺ refresh button
- Click column headers to sort (Chart column is not sortable)
- Default sort: highest price first

**Two sections in one scrollable list:**
- **Coin prices** (top) — Numismatics Vendor shops with sortable prices and sparklines
- **Barter** (bottom) — Create TableCloth shops showing payment item + sell/buy count

**Sidebar filters:**
- **Currency** — filter by coin denomination or barter item
- **Items** — filter by what's being sold

**Price history:**
- Server takes a price snapshot every **10 minutes**
- Stores up to **144 snapshots** per item (24 hours)
- History persists across server restarts (saved to world data)
- Used for % change calculation and sparkline rendering

---

## Top Sellers tab

A leaderboard of the most active traders on the server.

- Shows players ranked by **actual sales count** (from server-side trade statistics)
- Falls back to **shop listing count** if no sales data has been recorded yet
- Top 3 get 🥇🥈🥉 medals with gold/silver/bronze name colours
- Each row has a proportional bar chart indicator
- Footer shows whether the data is live sales or a fallback estimate

---

## Technical details

- Shops are indexed automatically when chunks load and when blocks are placed/broken
- Data is sent to the client on demand (right-click block or press M)
- No commands required — everything is block-based
- The block entity exists only to support the client-side renderer; no server data is stored in it
- All network packets use NeoForge's `CustomPacketPayload` system (protocol version "1")
- Compatible with both **NeoForge** and **Fabric** (common module)

**Network packets:**

| Direction | Packet | Purpose |
|-----------|--------|---------|
| C → S | `RequestShopListPacket` | Request full shop list |
| C → S | `RequestMarketPacket` | Request market + trade stats |
| S → C | `ShopListPacket` | Full list of `ShopEntry` records |
| S → C | `MarketPacket` | Aggregated `MarketEntry` list |
| S → C | `TradeStatsPacket` | Top sellers leaderboard |
| S → C | `OpenShopListPacket` | Signal client to open Shops screen |
| S → C | `OpenStockMarketPacket` | Signal client to open Market screen |

---

## Compatibility

| Platform | Status |
|----------|--------|
| NeoForge 1.21.1 | ✅ Supported |
| Fabric 1.21.1 | ✅ Supported |
| Forge | ❌ Not supported |

---

## License

MIT — see [LICENSE](LICENSE)
