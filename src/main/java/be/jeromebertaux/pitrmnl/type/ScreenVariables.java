package be.jeromebertaux.pitrmnl.type;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

import static be.jeromebertaux.pitrmnl.util.TypeConverter.FORMATTER;
import static be.jeromebertaux.pitrmnl.util.TypeConverter.OBJECT_MAPPER;

public record ScreenVariables(
        @JsonProperty("cpu_percent") String cpuPercent,
        @JsonProperty("cpu_load") List<String> cpuLoad,
        @JsonProperty("cpu_temp") String cpuTemp,
        @JsonProperty("cpu_unit") String cpuUnit,
        @JsonProperty("cpu_limit") String cpuLimit,
        @JsonProperty("memory_usage") String memoryUsage,
        String blocking,
        @JsonProperty("node_name") String nodeName,
        boolean update,
        @JsonProperty("last_refreshed") String lastRefreshed,
        @JsonProperty("query_total") List<Integer> queryTotal,
        @JsonProperty("query_blocked") List<Integer> queryBlocked,
        @JsonProperty("query_date") List<Long> queryDate
) {

    public ScreenVariables(final PaddData padd, final HistoryData historyData) {
        this(
                // Padd
                padd.cpuPercent(),
                padd.cpuLoad(),
                padd.cpuTemp(),
                padd.cpuUnit(),
                padd.cpuLimit(),
                padd.memoryPercent(),
                padd.blocking(),
                padd.nodeName(),
                padd.update(),
                FORMATTER.format(Instant.now()),
                // History
                historyData.totalQueries(),
                historyData.blockedQueries(),
                historyData.timestampQueries()
        );
    }

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ScreenVariables to JSON", e);
        }
    }
}
