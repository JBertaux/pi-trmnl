package be.jeromebertaux.pitrmnl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DataProcessor {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public static JSONObject processPadd(String data) {
        System.out.println("⚙️ Convert PADD data...");
        JSONObject jsonData = new JSONObject(data);

        JSONObject dataOutput = new JSONObject();
        dataOutput.put("cpu_percent", roundTo2Decimal(jsonData.getJSONObject("system")
                .getJSONObject("cpu").getDouble("%cpu")));

        final JSONArray cpuLoad = jsonData.getJSONObject("system").getJSONObject("cpu").getJSONObject("load").getJSONArray("percent");
        final JSONArray cpuLoadOut = new JSONArray();

        for (int i = 0; i < cpuLoad.length(); i++) {
            cpuLoadOut.put(roundTo2Decimal(cpuLoad.getDouble(i)));
        }

        dataOutput.put("cpu_load", cpuLoadOut);

        dataOutput.put("cpu_temp", roundTo2Decimal(jsonData.getJSONObject("sensors").getDouble("cpu_temp")));
        dataOutput.put("cpu_unit", jsonData.getJSONObject("sensors").getString("unit"));
        dataOutput.put("cpu_limit", roundTo2Decimal(jsonData.getJSONObject("sensors").getDouble("hot_limit")));
        dataOutput.put("memory_usage", roundTo2Decimal(jsonData.getJSONObject("system")
                .getJSONObject("memory").getJSONObject("ram").getDouble("%used")));
        dataOutput.put("blocking", jsonData.getString("blocking"));
        dataOutput.put("node_name", jsonData.getString("node_name"));

        String coreLocal = jsonData.getJSONObject("version").getJSONObject("core")
                .getJSONObject("local").getString("version");
        String coreRemote = jsonData.getJSONObject("version").getJSONObject("core")
                .getJSONObject("remote").getString("version");
        boolean coreUpdate = !coreLocal.equals(coreRemote);

        String webLocal = jsonData.getJSONObject("version").getJSONObject("web")
                .getJSONObject("local").getString("version");
        String webRemote = jsonData.getJSONObject("version").getJSONObject("web")
                .getJSONObject("remote").getString("version");
        boolean webUpdate = !webLocal.equals(webRemote);

        String ftlLocal = jsonData.getJSONObject("version").getJSONObject("ftl")
                .getJSONObject("local").getString("version");
        String ftlRemote = jsonData.getJSONObject("version").getJSONObject("ftl")
                .getJSONObject("remote").getString("version");
        boolean ftlUpdate = !ftlLocal.equals(ftlRemote);

        dataOutput.put("update", coreUpdate || webUpdate || ftlUpdate);

        String lastRefreshed = FORMATTER.format(Instant.now());
        dataOutput.put("last_refreshed", lastRefreshed);

        System.out.println("📊 PADD data converted successfully.");

        return dataOutput;
    }

    public static JSONObject processHistory(JSONObject jsonHistory) {
        System.out.println("⚙️ Convert History data...");

        JSONArray historyEntries = jsonHistory.getJSONArray("history");
        int startIndex = Math.max(0, historyEntries.length() - 40);
        JSONArray lastEntries = new JSONArray();
        
        for (int i = startIndex; i < historyEntries.length(); i++) {
            lastEntries.put(historyEntries.getJSONObject(i));
        }

        JSONObject dataOutput = new JSONObject();
        
        JSONArray queryTotal = new JSONArray();
        JSONArray queryBlocked = new JSONArray();
        JSONArray queryDate = new JSONArray();
        
        for (int i = 0; i < lastEntries.length(); i++) {
            JSONObject entry = lastEntries.getJSONObject(i);
            queryTotal.put(entry.getInt("total"));
            queryBlocked.put(entry.getInt("blocked"));
            queryDate.put(entry.getLong("timestamp") * 1000);
        }
        
        dataOutput.put("query_total", queryTotal);
        dataOutput.put("query_blocked", queryBlocked);
        dataOutput.put("query_date", queryDate);

        System.out.println("📊 History data converted successfully.");

        return dataOutput;
    }

    private static String roundTo2Decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}