package by.deokma.numismaticsstats.market;

import java.util.List;

/** Holds the latest market data received from the server (client-side singleton). */
public final class MarketData {

    private MarketData() {}

    private static List<MarketEntry> entries = List.of();
    private static boolean loading = false;

    public static void set(List<MarketEntry> list) { entries = List.copyOf(list); }
    public static List<MarketEntry> get()          { return entries; }
    public static void setLoading(boolean v)       { loading = v; }
    public static boolean isLoading()              { return loading; }
}
