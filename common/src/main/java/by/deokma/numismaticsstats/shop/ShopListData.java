package by.deokma.numismaticsstats.shop;

import java.util.List;

/** Holds the latest shop list received from the server (client-side singleton). */
public final class ShopListData {

    private ShopListData() {}

    private static List<ShopEntry> entries = List.of();

    public static void set(List<ShopEntry> list) { entries = List.copyOf(list); }
    public static List<ShopEntry> get()          { return entries; }
}
