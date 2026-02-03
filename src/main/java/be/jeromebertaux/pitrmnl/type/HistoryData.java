package be.jeromebertaux.pitrmnl.type;

import java.util.List;
import java.util.stream.Stream;

import static be.jeromebertaux.pitrmnl.util.TypeConverter.OBJECT_MAPPER;

public record HistoryData(List<HistoryEntry> history) {
    private static final int MAX_HISTORY_ENTRIES = 80;
    public record HistoryEntry(long timestamp, int total, int cached, int blocked, int forwarded) {
    }

    public static HistoryData fromJson(final String json) {
        try {
            return OBJECT_MAPPER.readValue(json, HistoryData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse History data from JSON", e);
        }
    }

    private Stream<HistoryEntry> limitedHistoryStream() {
        return this.history.stream().skip(Math.max(0, this.history.size() - MAX_HISTORY_ENTRIES));
    }

    public List<Integer> totalQueries() {
        return limitedHistoryStream().map(HistoryData.HistoryEntry::total).toList();
    }

    public List<Integer> blockedQueries() {
        return limitedHistoryStream().map(HistoryData.HistoryEntry::blocked).toList();
    }

    public List<Long> timestampQueries() {
        return limitedHistoryStream().map(entry -> entry.timestamp() * 1000L).toList();
    }
}