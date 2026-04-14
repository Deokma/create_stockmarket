# NumismaticsStats — Stock Market Monitor

**Minecraft 1.21.1 | NeoForge 21.1.226 | Fabric 0.16.9**

A mod that adds a real-time **server economy monitor** to Minecraft. Place the **Market Terminal** block, right-click it, and get a full overview of every shop on the server — prices, trends, filters, and more.

---

## Dependencies

| Mod | Version |
|-----|---------|
| [Create](https://modrinth.com/mod/create) | 6.0.9 |
| [Create: Numismatics](https://modrinth.com/mod/numismatics) | 1.0.19 |

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
You can also open it with the **M** key (configurable).

---

## Shops tab

A full list of every active shop on the server — both **Numismatics Vendor** and **Create TableCloth** shops.

**Table columns:**
- ★ — favourite toggle (persists per session)
- Item — icon + name, with `[V]` (Vendor) or `[TC]` (TableCloth) tag
- Price — coin price or barter item icon
- Owner — player name
- Mode — `SELL` / `BUY` badge
- Dim — dimension (overworld, nether, end…)

**Features:**
- Search by item name or owner
- Click any column header to sort; click again to reverse
- Star a shop to pin it to the top
- Filter tabs: All / ★ Fav / per-player

**Sidebar filters:**
- **Mode** — SELL / BUY
- **Type** — Vendor / TableCloth
- **Currency** — filter by payment type (Spur, Bevel, Cog, Crown… or barter item)
- **Items** — filter by what's being sold
- **Owner** — filter by shop owner
- **Dimension** — filter by world dimension

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
- Rows with high volume (≥ 5 shops) are highlighted in gold with a left stripe
- Hover over any row for a detailed tooltip:
  - Avg price, min price, 24h change
  - Volume breakdown (X sell / Y buy)
  - Full price history chart
  - 🔥 High activity badge
- Toolbar shows time since last refresh (`Xs ago`)
- Click column headers to sort

**Two sections:**
- **Coin prices** (top) — Numismatics Vendor shops with sortable prices
- **Barter** (bottom) — Create TableCloth shops showing payment item + sell/buy count

**Sidebar filters:**
- **Currency** — filter by coin denomination or barter item
- **Items** — filter by what's being sold

**Price history:**
- Server takes a price snapshot every **10 minutes**
- Stores up to **144 snapshots** per item (24 hours)
- History persists across server restarts (saved to world data)

---

## Technical details

- Shops are indexed automatically when chunks load and when blocks are placed/broken
- Data is sent to the client on demand (right-click block or press M)
- No commands required — everything is block-based
- Compatible with both **NeoForge** and **Fabric** (common module)

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
