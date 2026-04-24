package by.deokma.numismaticsstats.market;

import java.util.List;
import java.util.Map;

/**
 * Client-side singleton holding the top sellers leaderboard
 * received from the server via TradeStatsPacket.
 */
public final class TradeStatsData {

    private TradeStatsData() {}

    private static List<Map.Entry<String, Long>> topSellers = List.of();

    public static void set(List<Map.Entry<String, Long>> data) {
        topSellers = List.copyOf(data);
    }

    public static List<Map.Entry<String, Long>> getTopSellers() {
        return topSellers;
    }
}
