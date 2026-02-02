package be.jeromebertaux.pitrmnl.util;

import tools.jackson.databind.ObjectMapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TypeConverter {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TypeConverter() {}

    public static String roundTo2Decimal(final double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
