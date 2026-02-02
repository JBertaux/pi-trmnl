package be.jeromebertaux.pitrmnl.type;

import be.jeromebertaux.pitrmnl.util.TypeConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import static be.jeromebertaux.pitrmnl.util.TypeConverter.OBJECT_MAPPER;
import static be.jeromebertaux.pitrmnl.util.TypeConverter.roundTo2Decimal;

public record PaddData(
    SystemData system,
    SensorsData sensors,
    String blocking,
    @JsonProperty("node_name") String nodeName,
    VersionData version
) {
    public record SystemData(
        CpuData cpu,
        MemoryData memory
    ) {}

    public record CpuData(
        @JsonProperty("%cpu") double percentCpu,
        LoadData load
    ) {}

    public record LoadData(
        List<Double> percent
    ) {}

    public record MemoryData(
        RamData ram
    ) {}

    public record RamData(
        @JsonProperty("%used") double percentUsed
    ) {}

    public record SensorsData(
        @JsonProperty("cpu_temp") double cpuTemp,
        String unit,
        @JsonProperty("hot_limit") double hotLimit
    ) {}

    public record VersionData(
        ComponentVersion core,
        ComponentVersion web,
        ComponentVersion ftl
    ) {}

    public record ComponentVersion(
        VersionInfo local,
        VersionInfo remote
    ) {}

    public record VersionInfo(
        String version
    ) {}

    public String cpuPercent() {
        return roundTo2Decimal(this.system.cpu.percentCpu);
    }

    public List<String> cpuLoad() {
        return this.system.cpu.load.percent.stream().map(TypeConverter::roundTo2Decimal).toList();
    }

    public String cpuTemp() {
        return roundTo2Decimal(this.sensors.cpuTemp);
    }

    public String cpuUnit() {
        return this.sensors.unit;
    }

    public String cpuLimit() {
        return roundTo2Decimal(this.sensors.hotLimit);
    }

    public String memoryPercent() {
        return roundTo2Decimal(this.system.memory.ram.percentUsed);
    }

    public boolean update() {
        return !this.version.core.local.version.equals(this.version.core.remote.version) ||
                !this.version.web.local.version.equals(this.version.web.remote.version) ||
                !this.version.ftl.local.version.equals(this.version.ftl.remote.version);
    }

    public static PaddData fromJson(final String json) {
        try {
            return OBJECT_MAPPER.readValue(json, PaddData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PADD data from JSON", e);
        }
    }
}